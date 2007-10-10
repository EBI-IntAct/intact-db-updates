/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.deadproteins;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.business.IntactTransactionException;
import uk.ac.ebi.intact.context.IntactContext;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.persistence.dao.*;
import uk.ac.ebi.intact.uniprot.UniprotServiceException;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.service.UniprotRemoteService;
import uk.ac.ebi.intact.uniprot.service.UniprotService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * TODO comment this
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since <pre>17-Nov-2006</pre>
 */
public class UpdateDeadProteins {

    /**
     * Sets up a logger for that class.
     */
    public static final Log log = LogFactory.getLog( UpdateDeadProteins.class );

    private File proteinsToUpdate;
    private BufferedWriter proteinsToUpdateBuffer;
    private static final String NEW_LINE = System.getProperty( "line.separator" );

    ////////////////////////
    // Constructor

    public UpdateDeadProteins() {
    }

    ////////////////////////
    // Setters & Getters

    public File getProteinsToUpdate() {
        return proteinsToUpdate;
    }

    public void setProteinsToUpdate( File proteinsToUpdate ) {
        this.proteinsToUpdate = proteinsToUpdate;
    }

    ///////////////////////
    // Utility methods

    private String pickUniparcWithLongestSequence( Collection<RemappingEntry> entries ) {
        int maxLen = 0;
        String upi = null;
        for ( RemappingEntry entry : entries ) {
            if ( entry.getSequenceLength() > maxLen ) {
                maxLen = entry.getSequenceLength();
                upi = entry.getUpi();
            }
        }
        log.info( "Picked UPI (" + upi + ") having the longest sequence: " + maxLen );
        return upi;
    }

    private String pickLongestUniprotSequence( Collection<String> liveIds ) throws UniprotServiceException {
        UniprotService uniprot = new UniprotRemoteService();
        int maxLen = 0;
        String selectedId = null;

        for ( String liveId : liveIds ) {

            // search uniprot
            Collection<UniprotProtein> uniprotProteins = uniprot.retrieve( liveId );

            if ( uniprotProteins.size() > 1 ) {
                log.warn( "WARNING - Found " + uniprotProteins.size() + " proteins in UniProt matching " + liveId );
            }

            for ( UniprotProtein uniprotProtein : uniprotProteins ) {
                if ( uniprotProtein.getSequenceLength() > maxLen ) {
                    maxLen = uniprotProtein.getSequenceLength();
                    selectedId = liveId;
                    if ( !selectedId.equals( uniprotProtein.getPrimaryAc() ) ) {
                        log.info( "WARNING - The matching live id is not a primary AC" );
                    }
                }
            }
        }
        log.info( "The longest sequence was: " + maxLen );

        return selectedId;
    }

    private boolean isLiveUniprot( String id, int taxidFilter ) throws UniprotServiceException {
        UniprotService uniprot = new UniprotRemoteService();
        Collection<UniprotProtein> uniprotProteins = uniprot.retrieve( id );
        for ( Iterator<UniprotProtein> iterator = uniprotProteins.iterator(); iterator.hasNext(); ) {
            UniprotProtein protein = iterator.next();
            int t = protein.getOrganism().getTaxid();
            if ( t != taxidFilter ) {
                log.info( "Protein " + id + " was filtered out. Taxid required: " + taxidFilter + ", found: " + t + "." );
                iterator.remove();
            }
        }
        return !uniprotProteins.isEmpty();
    }

    private Collection<String> selectLiveUniProtIds( Collection<String> ids, int taxidFilter ) throws UniprotServiceException {
        Collection<String> liveIds = new ArrayList<String>();
        for ( String id : ids ) {
            if ( isLiveUniprot( id, taxidFilter ) ) {
                liveIds.add( id );
            }
        }
        return liveIds;
    }

    private Set<String> collectUniProtIds( Collection<RemappingEntry> entries, String anId ) {
        if ( anId == null ) {
            throw new IllegalArgumentException();
        }
        Set<String> selectedIds = new HashSet<String>();
        for ( RemappingEntry entry : entries ) {
            for ( String id : entry.getIdentifiers() ) {
                if ( !anId.equals( id ) ) {
                    selectedIds.add( id );
                }
            }
        }
        return selectedIds;
    }

    public static final int NOT_INITIALIZED = -99;

    private int getTaxId( List<ProteinImpl> list, String uniprotId ) {
//        DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
//        ProteinDao pdao = daoFactory.getProteinDao();
//        IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        int taxid = NOT_INITIALIZED;
//        List<ProteinImpl> list = pdao.getByUniprotId( uniprotId );
        for ( ProteinImpl protein : list ) {
            int t = Integer.parseInt( protein.getBioSource().getTaxId() );
            if ( taxid == NOT_INITIALIZED ) {
                taxid = t;
            } else {
                if ( t != taxid ) {
                    throw new IllegalStateException( uniprotId + " matches proteins in IntAct having different taxid: "
                                                     + t + ", " + taxid + "." );
                }
            }
        }

//        IntactContext.getCurrentInstance().getDataContext().commitTransaction();
        return taxid;
    }

    private String printCollection( Collection<String> c ) {
        StringBuffer sb = new StringBuffer( 8 * c.size() );
        sb.append( '[' );
        for ( Iterator<String> iterator1 = c.iterator(); iterator1.hasNext(); ) {
            String liveId = iterator1.next();
            sb.append( liveId );
            if ( iterator1.hasNext() ) {
                sb.append( ',' );
            }
        }
        sb.append( ']' );
        return sb.toString();
    }

    private Xref searchXref( Protein protein, CvDatabase db, CvXrefQualifier qualifier ) {
        for ( InteractorXref xref : protein.getXrefs() ) {
            if ( db.equals( xref.getCvDatabase() ) ) {
                if ( qualifier.equals( xref.getCvXrefQualifier() ) ) {
                    return xref;
                }
            }
        }
        return null;
    }

    private void updateComponent( Protein protein, String formerUniprotId, DaoFactory daoFactory ) {

        log.info( "Adding extra Xref on Components of protein " + protein.getShortLabel() + " (" + protein.getAc() + ") ..." );

        CvObjectDao<CvObject> cvdao = daoFactory.getCvObjectDao();
        CvDatabase uniprot = cvdao.getByPrimaryId( CvDatabase.class, CvDatabase.UNIPROT_MI_REF );
        if ( uniprot == null ) {
            throw new IllegalStateException();
        }

        CvXrefQualifier secondaryAc = cvdao.getByPrimaryId( CvXrefQualifier.class, CvXrefQualifier.SECONDARY_AC_MI_REF );
        if ( secondaryAc == null ) {
            throw new IllegalStateException();
        }

        Institution owner = IntactContext.getCurrentInstance().getConfig().getInstitution();
        ComponentDao cdao = daoFactory.getComponentDao();

        XrefDao xdao = daoFactory.getXrefDao();
        List<Component> components = cdao.getByInteractorAc( protein.getAc() );
        for ( Component component : components ) {
            ComponentXref xref = new ComponentXref( owner, uniprot, formerUniprotId, secondaryAc );
            xdao.persist( xref );

            if ( component.getXrefs().contains( xref ) ) {
                log.info( "  This component has already that Xref(uniprotkb, " + formerUniprotId + ", secondary-ac)." );
            } else {
                log.info( "  Adding Xref(uniprotkb, " + formerUniprotId + ", secondary-ac) on Component(" + component.getAc() + ")" );
                component.addXref( xref );
                cdao.update( component );
            }
        }
    }

    private void addNoUniprotUpdateAnnotation( ProteinImpl protein, DaoFactory daoFactory ) {

        CvObjectDao<CvObject> cvdao = daoFactory.getCvObjectDao();
        CvTopic noUniprotUpdate = cvdao.getByShortLabel( CvTopic.class, CvTopic.NON_UNIPROT );
        if ( noUniprotUpdate == null ) {
            throw new IllegalStateException();
        }
        AnnotationDao adao = daoFactory.getAnnotationDao();
        ProteinDao pdao = daoFactory.getProteinDao();

        Institution owner = IntactContext.getCurrentInstance().getConfig().getInstitution();
        Annotation annot = new Annotation( owner, noUniprotUpdate );
        log.info( "Created new Anntotation( no-uniprot-update )" );
        adao.persist( annot );

        protein.addAnnotation( annot );
        log.info( "Adding Annotation( no-uniprot-update ) to protein " + protein.getShortLabel() + "(" + protein.getAc() + ")" );
        pdao.update( protein );
    }

    private void printDatabaseConnectionDetails() {
        try {
            String dbname = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getBaseDao().getDbName();
            String username = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getBaseDao().getDbUserName();
            log.info( "username = " + username );
            log.info( "dbname = " + dbname );
        } catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
    }

    private void logRequestForProteinUpdate( String uniprotId ) {

        try {
            if ( proteinsToUpdate != null ) {

                if ( proteinsToUpdateBuffer == null ) {
                    proteinsToUpdateBuffer = new BufferedWriter( new FileWriter( proteinsToUpdate ) );
                }

                proteinsToUpdateBuffer.write( uniprotId + NEW_LINE );
                proteinsToUpdateBuffer.flush();

            }
        } catch ( Exception e ) {
            // ignore
        } finally {
            log.warn( "TODO - run update on that protein: " + uniprotId );
        }
    }

    private void closeOpenedFiles() {
        if ( proteinsToUpdate != null ) {
            if ( proteinsToUpdateBuffer == null ) {
                try {
                    log.info( "Closing File containing the list of proteins to update." );
                    proteinsToUpdateBuffer.flush();
                    proteinsToUpdateBuffer.close();
                } catch ( IOException e ) {
                    // ignore
                }
            }
        }
    }

    //////////////////////
    // Business logic

    private int updatedToUniparc = 0;
    private int updatedToAnOtherUniprot = 0;
    private int updatedToAnExistingIntActProtein = 0;

    /**
     * Replace protein by new protein. That involves transfering all existing interaction to the new one and leaving
     * extra Xref on the components.
     *
     * @param protein    old protein
     * @param uniprotId  old protein's uniprot id
     * @param newProtein new protein
     * @param daoFactory database access
     */
    private void updateExistingProteinToUniprot( ProteinImpl protein, String uniprotId, ProteinImpl newProtein, DaoFactory daoFactory ) {

        // Add Xref on Components
        updateComponent( protein, uniprotId, daoFactory );

        // transfert interaction to the new protein
        ComponentDao cdao = daoFactory.getComponentDao();
        List<Component> components = cdao.getByInteractorAc( protein.getAc() );
        for ( Component component : components ) {

            Interaction inter = component.getInteraction();
            log.info( "Replacing protein " + protein.getShortLabel() + "(" + protein.getAc() + ") by " +
                      newProtein.getShortLabel() + "(" + newProtein.getAc() + ") in interaction " + inter.getShortLabel() + "(" + inter.getAc() + ")" );

            protein.removeActiveInstance( component ); // Note: this sets interactor to null !
            component.setInteractor( newProtein );

            cdao.update( component );
        }

        // consequently, the original protein is not linked to anything ...
        log.info( "Deleting protein related to " + uniprotId + " namely: " + protein.getShortLabel() + " (" + protein.getAc() + ")." );
        ProteinDao pdao = daoFactory.getProteinDao();
        pdao.delete( protein );

        updatedToAnExistingIntActProtein++;
    }

    private void updateToUniprot( ProteinImpl protein, String uniprotId, DaoFactory daoFactory ) {

        XrefDao xdao = daoFactory.getXrefDao();
        CvObjectDao<CvObject> cvDao = daoFactory.getCvObjectDao();

        CvDatabase uniprot = cvDao.getByPrimaryId( CvDatabase.class, CvDatabase.UNIPROT_MI_REF );
        if ( uniprot == null ) {
            throw new IllegalStateException();
        }

        CvXrefQualifier identity = cvDao.getByPrimaryId( CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF );
        if ( identity == null ) {
            throw new IllegalStateException();
        }

        // search for the xref identity now and update it.
        Xref identityXref = searchXref( protein, uniprot, identity );
        if ( identityXref == null ) {
            throw new IllegalStateException( "Could not find an Xref( uniprot, identity ) for protein " +
                                             protein.getShortLabel() + "(" + protein.getAc() + "). Abort." );
        } else {
            // update it
            log.info( "Updated UniProt identity to " + uniprotId );
            String formerId = identityXref.getPrimaryId();
            identityXref.setPrimaryId( uniprotId );
            xdao.update( identityXref );

            // Add old uniprot Xref onto all respective Component where this protein is in use.
            updateComponent( protein, formerId, daoFactory );
        }

        logRequestForProteinUpdate( uniprotId );

        updatedToAnOtherUniprot++;
    }

    private void updateToUniparc( ProteinImpl protein, String upi, DaoFactory daoFactory ) {

        CvObjectDao<CvObject> cvdao = daoFactory.getCvObjectDao();

        CvDatabase uniprot = cvdao.getByPrimaryId( CvDatabase.class, CvDatabase.UNIPROT_MI_REF );
        CvDatabase uniparc = cvdao.getByPrimaryId( CvDatabase.class, CvDatabase.UNIPARC_MI_REF );
        CvXrefQualifier identity = cvdao.getByPrimaryId( CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF );

        Xref identityXref = searchXref( protein, uniprot, identity );
        if ( identityXref == null ) {
            throw new IllegalStateException( "Could not find an Xref( uniprot, identity ) for protein " +
                                             protein.getShortLabel() + "(" + protein.getAc() + "). Abort." );
        } else {
            // update it
            log.info( "Updated UniProt identity to UniParc " + upi );

            String formerId = identityXref.getPrimaryId();
            identityXref.setPrimaryId( upi );
            identityXref.setCvDatabase( uniparc );

            log.info( "Updating identity Xref to UniParc " + upi + "..." );
            XrefDao xdao = daoFactory.getXrefDao();
            xdao.update( identityXref );

            // add new Annotation( no-uniprot-update )
            addNoUniprotUpdateAnnotation( protein, daoFactory );

            // Add old uniprot Xref onto all respective Component where this protein is in use.
            updateComponent( protein, formerId, daoFactory );

            updatedToUniparc++;
        }
    }

    public void remapProteins( Map<String, Collection<RemappingEntry>> remapping ) throws UniprotServiceException, IntactTransactionException {

        printDatabaseConnectionDetails();
        int count = 0;
        int total = remapping.size();

        for ( Iterator<String> iterator = remapping.keySet().iterator(); iterator.hasNext(); ) {
            String id = iterator.next();
            Collection<RemappingEntry> entries = remapping.get( id );

            count++;
            log.debug( "--" + count + "/" + total + "------------------------------------------" );
            log.debug( id );

            DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
            ProteinDao pdao = daoFactory.getProteinDao();
            IntactContext.getCurrentInstance().getDataContext().beginTransaction();

            List<ProteinImpl> deadProteins = pdao.getByUniprotId( id );

            if ( deadProteins.isEmpty() ) {
                log.info( "Could not find protein matching uniprot id: " + id + ". It might have been remapped already. skip." );

                // close transaction before to go to next entry
                try {
                    IntactContext.getCurrentInstance().getDataContext().commitTransaction();
                } catch ( IntactTransactionException e ) {
                    throw new UniprotServiceException( e );
                }
                continue;
            }

            if ( deadProteins.size() > 1 ) {
                throw new UnsupportedOperationException( "More than 1 proteins was found for UniProt id: " + id );
            }

            ProteinImpl deadProtein = deadProteins.iterator().next();
            int taxid = getTaxId( deadProteins, id );
            log.info( "Protein: " + deadProtein.getShortLabel() + " (" + deadProtein.getAc() + ") - Taxid: " + taxid );

            boolean doUniparcRemapping = false;

            // Check if there is any other uniprot id mention than id
            log.info( "Looking up for other UniProt IDs than " + id + " ..." );
            Set<String> otherIds = collectUniProtIds( entries, id );
            log.info( "Found " + otherIds.size() + " IDs..." );

            // Check if any of them is live in UniProt
            if ( !otherIds.isEmpty() ) {
                log.info( "Extracting live UniProt identifiers..." );
                Collection<String> liveIds = selectLiveUniProtIds( otherIds, taxid );
                log.info( "Found " + liveIds.size() + " live IDs..." );

                if ( liveIds.isEmpty() ) {

                    doUniparcRemapping = true;

                } else {

                    log.info( "Live IDs: " + printCollection( liveIds ) );
                    String idReplacement = null;
                    if ( liveIds.size() > 1 ) {
                        // pick the one having the longest sequence
                        log.info( "Selecting UniProt AC providing the longest sequence..." );
                        idReplacement = pickLongestUniprotSequence( liveIds );
                    } else {
                        // take the first and only one
                        idReplacement = liveIds.iterator().next();
                    }

                    // check if any of the live ID has instances of protein in the database
                    log.info( "We will replace " + id + " by " + idReplacement );

                    // but first, check that this protein doesn't exist in IntAct yet...
                    List<ProteinImpl> proteins = pdao.getByUniprotId( idReplacement );
                    log.info( "Found " + proteins.size() + " protein(s) matching " + idReplacement + " in IntAct" );
                    if ( !proteins.isEmpty() ) {
                        if ( proteins.size() == 1 ) {

                            log.info( "We will use the single protein we found and remap interactions ..." );
                            ProteinImpl newProtein = proteins.iterator().next();
                            updateExistingProteinToUniprot( deadProtein, id, newProtein, daoFactory );

                        } else {

                            log.info( "Found more than 1 protein matching " + idReplacement );
                            for ( ProteinImpl protein : proteins ) {
                                log.info( protein.toString() );
                            }
                            throw new IllegalArgumentException();
                        }
                    } else {
                        // replace existing uniprot ID by the new one
                        updateToUniprot( deadProtein, idReplacement, daoFactory );
                    }
                } // else
            } else {
                // we didn't find any uniprot identifiers in the entry, fall back on UniParc
                doUniparcRemapping = true;
            }

            if ( doUniparcRemapping ) {
                String upi = pickUniparcWithLongestSequence( entries );
                log.info( "Updating " + id + " by " + upi + " ..." );
                updateToUniparc( deadProtein, upi, daoFactory );
            }

            try {
                IntactContext.getCurrentInstance().getDataContext().commitTransaction();
            } catch ( IntactTransactionException e ) {
                throw new UniprotServiceException( e );
            }

        } // for remapping entry

        closeOpenedFiles();

        // print simple stats
        log.info( "Protein updated to an existing IntAct protein: " + updatedToAnExistingIntActProtein );
        log.info( "Protein updated to an other UniProt identity: " + updatedToAnOtherUniprot );
        log.info( "Protein updated to UniParc identity: " + updatedToUniparc );
    }
}