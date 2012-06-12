package uk.ac.ebi.intact.util.biosource;

import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.TransactionStatus;
import uk.ac.ebi.intact.bridges.taxonomy.TaxonomyService;
import uk.ac.ebi.intact.bridges.taxonomy.TaxonomyServiceException;
import uk.ac.ebi.intact.bridges.taxonomy.TaxonomyTerm;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.model.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Global biosource update.
 * <p/>
 * The routine is simple, for each biosource in the intact database, we query the taxonomy source by taxid. If the
 * taxon is obsolete, we replace the taxid by the new one. shortlabel and fullname are updated based on scientific name,
 * common name as well as the tissue and cell types attached to the intact biosource.
 * <br/>
 * A basic report (in the form of a CSV file stored in the current directory) is generated and reflects the changes made
 * to updated biosources.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 2.1.2
 */
public class BioSourceUpdater {

    public static void main( String[] args ) {

        if( args.length != 1 ) {
            System.err.println( "Usage: BioSourceUpdater <database>" );
            System.exit( 1 );
        }

        final String database = args[0];

        IntactContext.initContext( new String[]{"/META-INF/"+ database +".spring.xml"} );

        final DaoFactory daoFactory = IntactContext.getCurrentInstance().getDaoFactory();

        final TransactionStatus transactionStatus = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        final List<BioSource> bioSources = daoFactory.getBioSourceDao().getAll();

        BioSourceService service = BioSourceServiceFactory.getInstance().buildBioSourceService();
        final TaxonomyService taxonomy = service.getTaxonomyService();

        final CvAliasType synonymType = getOrCreateSynonymType( daoFactory );

        StringBuilder sb = new StringBuilder( 4096 );

        int totalCount = 0;
        int updatedCount = 0;

        for ( BioSource bs : bioSources ) {

            totalCount++;

            final String shortlabel = bs.getShortLabel();
            final String fullname = bs.getFullName();
            final String taxid = bs.getTaxId();

            final CvCellType cellType = bs.getCvCellType();
            final CvTissue tissue = bs.getCvTissue();

            String log = bs.getAc() + "\t" +
                       bs.getTaxId() + "\t" +
                       bs.getShortLabel() + "\t" +
                       bs.getFullName() + "\t" +
                       (tissue==null?"-":tissue.getShortLabel()) + "\t" +
                       (cellType==null?"-":cellType.getShortLabel()) + "\t";


            if( tissue != null && cellType != null ) {
                System.err.println( "WARNING: Biosource with both tissue and cell type set: " + bs.getAc() );
            }

            printBiosource( bs, cellType, tissue );

            try {
                final TaxonomyTerm taxon = taxonomy.getTaxonomyTerm( Integer.parseInt( bs.getTaxId() ) );

                if( ! bs.getTaxId().equals(String.valueOf( taxon.getTaxid() ) ) ) {
                    bs.setTaxId( String.valueOf( taxon.getTaxid() ) );
                }

                boolean updatedShortlabel = false;
                // shortlabel
                if( taxon.hasMnemonic() ) {
                    bs.setShortLabel( taxon.getMnemonic().toLowerCase() );
                    updatedShortlabel = true;
                } else if( taxon.hasCommonName() ) {
                    bs.setShortLabel( taxon.getCommonName() );
                    updatedShortlabel = true;
                }

                if( updatedShortlabel ) {

                    if( tissue != null ) {
                        bs.setShortLabel( bs.getShortLabel() + '-' + tissue.getShortLabel() );
                    }

                    if( cellType != null ) {
                        bs.setShortLabel( bs.getShortLabel() + '-' + cellType.getShortLabel() );
                    }
                }

                // fullName
                if( tissue == null && cellType == null ) {
                    bs.setFullName( taxon.getScientificName() );
                    if( taxon.hasCommonName() ) {
                        bs.setFullName( bs.getFullName() + " ("+ taxon.getCommonName() +")" );
                    }
                }

                // aliases
                if( ! taxon.getSynonyms().isEmpty() ) {

                    for ( final String synonym : taxon.getSynonyms() ) {
                         bs.addAlias( new BioSourceAlias( bs.getOwner(), bs, synonymType, synonym) );
                    }
                }

                printBiosource( bs, cellType, tissue );

                // updated fields
                log += bs.getTaxId() + "\t" +
                       bs.getShortLabel() + "\t" +
                       bs.getFullName() +
                       "\n" ;

                if( !StringUtils.equals( bs.getShortLabel(), shortlabel ) ||
                    !StringUtils.equals( bs.getFullName(), fullname ) ||
                    !StringUtils.equals( bs.getTaxId(), taxid ) ) {
                    // only log updated BioSource
                    sb.append( log );
                    updatedCount++;
                }

                System.out.println( "Saving biosource..." );
                IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate( bs );

            } catch ( TaxonomyServiceException e ) {
                e.printStackTrace();
            }

        } // all biosources

        System.out.println("#biosource: " + totalCount);
        System.out.println("#updated biosource: " + updatedCount);

        try {
            BufferedWriter out = new BufferedWriter( new FileWriter( "biosource_upated.tsv" ) );
            try{
                out.write( sb.toString() );
                out.flush();
                System.out.println( "Saved audit file" );
            }
            finally {
                out.close();
            }

        } catch ( IOException e ) {
        }

        IntactContext.getCurrentInstance().getDataContext().commitTransaction( transactionStatus );
    }

    private static void printBiosource( BioSource bs, CvCellType cellType, CvTissue tissue ) {
        System.out.println( "Updating biosource: " + bs.getShortLabel() +
                            "(taxid: " + bs.getTaxId() +
                            " | CellType: " + (cellType != null ? cellType.getShortLabel() : "-") +
                            " | Tissue: " + (tissue != null ? tissue.getShortLabel() : "-") +
                            " | #alias: "+ bs.getAliases().size() +")" );
    }

    private static CvAliasType getOrCreateSynonymType( DaoFactory daoFactory ) {
        final Institution owner = IntactContext.getCurrentInstance().getInstitution();

        CvAliasType synonymType =
                daoFactory.getCvObjectDao( CvAliasType.class ).getByIdentifier( "MI:1041" );

        if( synonymType != null ) {
            return synonymType;
        }

        // create the term
        System.out.println( "Could not find the CvAliasType( 'synonym', 'MI:1041' ), creating it now ..." );
        final CvDatabase psimi =
                daoFactory.getCvObjectDao( CvDatabase.class ).getByIdentifier( CvDatabase.PSI_MI_MI_REF );
        if ( psimi == null ) {
            throw new IllegalArgumentException( "You must give a non null psimi" );
        }

        final CvXrefQualifier identity =
                daoFactory.getCvObjectDao( CvXrefQualifier.class ).getByIdentifier( CvXrefQualifier.IDENTITY_MI_REF );
        if ( identity == null ) {
            throw new IllegalArgumentException( "You must give a non null identity" );
        }

                synonymType = new CvAliasType( owner, "synonym" );
        synonymType.setIdentifier( "MI:1041" );
        synonymType.addXref( new CvObjectXref(owner, psimi, "MI:1041", identity));

        // persist
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate( synonymType );
        System.out.println( "CvAliasType( 'synonym', 'MI:1041' ) was persisted." );

        return synonymType;
    }
}
