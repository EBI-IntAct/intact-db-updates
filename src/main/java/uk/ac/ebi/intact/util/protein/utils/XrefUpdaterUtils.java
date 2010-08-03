/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.protein.utils;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.CvObjectDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.uniprot.model.UniprotFeatureChain;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;
import uk.ac.ebi.intact.uniprot.model.UniprotXref;
import uk.ac.ebi.intact.util.protein.CvHelper;

import java.util.*;

/**
 * Utilities for updating Xrefs.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 1.1.2
 */
public final class XrefUpdaterUtils {

    private XrefUpdaterUtils() {
    }

    /**
     * Sets up a logger for that class.
     */
    public static final Log log = LogFactory.getLog( XrefUpdaterUtils.class );

    public static Xref convert( UniprotXref uniprotXref, CvDatabase db ) {
        Institution owner = IntactContext.getCurrentInstance().getInstitution();
        return new InteractorXref( owner, db, uniprotXref.getAccession(), null );
    }

    public static Collection<Xref> convert( Collection<UniprotXref> uniprotXrefs, CvDatabase db ) {
        Collection<Xref> xrefs = new ArrayList<Xref>( uniprotXrefs.size() );
        for ( UniprotXref uniprotXref : uniprotXrefs ) {
            xrefs.add( convert( uniprotXref, db ) );
        }
        return xrefs;
    }

    public static Map<String, Collection<UniprotXref>> clusterByDatabaseName( Collection<UniprotXref> xrefs ) {

        Map<String, Collection<UniprotXref>> xrefCluster = new HashMap<String, Collection<UniprotXref>>( xrefs.size() );

        for ( UniprotXref cr : xrefs ) {
            Collection<UniprotXref> c = xrefCluster.get( cr.getDatabase() );
            if ( c == null ) {
                // not found, add an entry
                c = new ArrayList<UniprotXref>();
                xrefCluster.put( cr.getDatabase(), c );
            }
            c.add( cr );
        }

        return xrefCluster;
    }

    public static XrefUpdaterReport updateAllXrefs( Protein protein,
                                       UniprotProtein uniprotProtein,
                                       Map<String, String> databaseName2mi
    ) {

        Map<String, Collection<UniprotXref>> xrefCluster = XrefUpdaterUtils.clusterByDatabaseName( uniprotProtein.getCrossReferences() );

        List<Xref> createdXrefs = new ArrayList<Xref>();
        List<Xref> deletedXrefs = new ArrayList<Xref>();

        for ( Map.Entry<String, Collection<UniprotXref>> entry : xrefCluster.entrySet() ) {

            String db = entry.getKey();
            Collection<UniprotXref> uniprotXrefs = entry.getValue();

            DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
            CvObjectDao<CvDatabase> dbDao = daoFactory.getCvObjectDao( CvDatabase.class );

            // search by shortlabel is dodgy ! Try mapping to MI:xxxx first.
            String mi = databaseName2mi.get( db.toLowerCase() );
            CvDatabase cvDatabase = null;
            if ( mi != null ) {
                cvDatabase = dbDao.getByPsiMiRef( mi );

                if( cvDatabase == null ) {
                    log.error( "Could not find CvDatabase by label: " + db );
                }
            }

            if(cvDatabase != null){
                // Convert collection into Xref
                Collection<Xref> xrefs = XrefUpdaterUtils.convert( uniprotXrefs, cvDatabase );
                XrefUpdaterReport report = XrefUpdaterUtils.updateXrefCollection( protein, cvDatabase, xrefs );
                createdXrefs.addAll(Arrays.asList(report.getAddedXrefs()));
                deletedXrefs.addAll(Arrays.asList(report.getRemovedXrefs()));
            }else{
                log.debug("We are not copying across xref to " + db);
            }
        }
        //UPDATE THE UNIPROT XREF (SECONDARY AND PRIMARY ID)
        XrefUpdaterUtils.updateUniprotXrefs( protein, uniprotProtein );

        // CONVERT ALL THE uniprotProtein crossReferences to intact protein InteractorXref and put them in the convertedXrefs
        // collection.
        CvObjectDao<CvDatabase> dbDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getCvObjectDao(CvDatabase.class);
        Collection<Xref> convertedXrefs = new ArrayList<Xref>();
        for(UniprotXref uniprotXref : uniprotProtein.getCrossReferences()){
            String db = uniprotXref.getDatabase().toLowerCase();
            String mi = databaseName2mi.get( db );
            CvDatabase cvDatabase = null;
            if ( mi != null ) {
                cvDatabase = dbDao.getByPsiMiRef( mi );

                if( cvDatabase == null ) {
                    log.error( "Could not find CvDatabase by label: " + db );
                }
            }
            if(cvDatabase!=null){
                convertedXrefs.add(XrefUpdaterUtils.convert(uniprotXref, cvDatabase));
            }else{
                log.debug("cvDatabase " + db + " could not be found, the uniprot crossRef won't be converted to intact xref");
            }
        }

        // CHECK THAT ALL INTACT XREF STILL EXIST IN UNIPROT, OTHERWISE DELETE THEM
        Collection<InteractorXref> xrefs = protein.getXrefs();
        for (Iterator<InteractorXref> iterator = xrefs.iterator(); iterator.hasNext();) {
            InteractorXref xref =  iterator.next();

            // Dont's check uniprot xref as those one have been done earlier and as anyway, the uniprot Xref won't have
            // uniprot xref as intact protein does (the uniprot xref of the intact protein correspond to the primary and
            // secondary ac of the proteins).
            CvDatabase cvDb = xref.getCvDatabase();
            String cvDbMi = cvDb.getMiIdentifier();

            if(CvDatabase.UNIPROT_MI_REF.equals(cvDbMi)){
                continue;
            }
            // If the protein xref does not exist in the uniprot entry anymore delete it.
            if(!convertedXrefs.contains(xref)){
                iterator.remove();
                //xrefDao.delete(xref);
            }
        }

        return new XrefUpdaterReport(protein,
                                     createdXrefs.toArray(new Xref[createdXrefs.size()]),
                                     deletedXrefs.toArray(new Xref[deletedXrefs.size()]));
    }

    /**
     * Update of the Xref of a protein.
     * <p/>
     * <pre>
     * Algo sketch:
     * 1) select a subset of the xref of the given protein based on the given CvDatabase
     * 2) select the outdated Xref
     * 3) reused them to create new Xref and delete the remaining one. By doing so we don't waste ACs
     * </pre>
     *
     * @param protein  the protein what we want to update the Xrefs
     * @param database the target database
     * @param newXrefs the new set of xrefs
     *
     * @return true if the protein has been updated, otherwise false
     */
    public static XrefUpdaterReport updateXrefCollection( Protein protein, CvDatabase database, Collection<Xref> newXrefs ) {

        if ( protein == null ) {
            throw new IllegalArgumentException( "You must give a non null protein." );
        }

        if ( database == null ) {
            throw new IllegalArgumentException( "You must give a non null database (cvDatabase))." );
        }

        if ( newXrefs == null ) {
            throw new IllegalArgumentException( "You must give a non null collection of xrefs." );
        }

        boolean updated = false;
        Collection<Xref> currentXrefs = null;

        // select only the xref of the given database
        for ( Xref xref : protein.getXrefs() ) {
            if ( database.equals( xref.getCvDatabase() ) ) {
                if ( currentXrefs == null ) {
                    currentXrefs = new ArrayList<Xref>();
                }
                currentXrefs.add( xref );
            }
        }

        if ( currentXrefs == null ) {
            currentXrefs = Collections.EMPTY_LIST;
        }

        Collection<Xref> toDelete = CollectionUtils.subtract( currentXrefs, newXrefs ); // current minus new
        if( log.isDebugEnabled() ) {
            log.debug( toDelete.size() + " xrefs to delete: "+toDelete );
        }
        Collection<Xref> toCreate = CollectionUtils.subtract( newXrefs, currentXrefs );
        if( log.isDebugEnabled() ) {
            log.debug( toCreate.size() + " xrefs to create: "+toCreate );
        }

        for ( Xref xref : toCreate ) {
            addNewXref( protein, xref );
        }

        for ( Xref xref : toDelete ) {
            // delete remaining outdated xrefs
            if( log.isDebugEnabled() ) {
                log.debug( "DELETING: " + xref );
            }
            Iterator<InteractorXref> protXrefsIterator = protein.getXrefs().iterator();
            while (protXrefsIterator.hasNext()) {
                InteractorXref interactorXref =  protXrefsIterator.next();

                if (interactorXref.getPrimaryId().equals(xref.getPrimaryId())) {
                    IntactContext.getCurrentInstance().getDataContext().getDaoFactory()
                            .getXrefDao(InteractorXref.class).delete(interactorXref);
                    protXrefsIterator.remove();
                }

            }
        }
        
        return new XrefUpdaterReport(protein,
                                     toCreate.toArray(new Xref[toCreate.size()]),
                                     toDelete.toArray(new Xref[toDelete.size()]));
    }

    private static boolean addNewXref( AnnotatedObject current, final Xref xref ) {
        // Make sure the xref does not yet exist in the object
        if ( current.getXrefs().contains( xref ) ) {
            log.debug( "SKIPPED: [" + xref + "] already exists" );
            return false; // quit
        }

        // add the xref to the AnnotatedObject
        IntactContext.getCurrentInstance().getDataContext().getDaoFactory()
                            .getXrefDao().persist(xref);
        current.addXref( xref );

        return true;
    }

    public static XrefUpdaterReport updateUniprotXrefs( Protein protein, UniprotProtein uniprotProtein ) {

        CvDatabase uniprot = CvHelper.getDatabaseByMi( CvDatabase.UNIPROT_MI_REF );
        CvXrefQualifier identity = CvHelper.getQualifierByMi( CvXrefQualifier.IDENTITY_MI_REF );
        CvXrefQualifier secondaryAcQual = CvHelper.getQualifierByMi( CvXrefQualifier.SECONDARY_AC_MI_REF );
        Institution owner = CvHelper.getInstitution();

        String dbRelease = uniprotProtein.getReleaseVersion();

        if ( log.isDebugEnabled() ) {
            log.debug( "Building UniProt Xref collection prior to update of " + protein.getShortLabel() );
        }
        Collection<Xref> ux = new ArrayList<Xref>( uniprotProtein.getSecondaryAcs().size() + 1 );
        ux.add( new InteractorXref( owner, uniprot, uniprotProtein.getPrimaryAc(), null, dbRelease, identity ) );

        log.debug( "Found " + uniprotProtein.getSecondaryAcs().size() + " secondary ACs" );
        for ( String secondaryAc : uniprotProtein.getSecondaryAcs() ) {
            InteractorXref interactorXref =   new InteractorXref( owner, uniprot, secondaryAc, null, dbRelease, secondaryAcQual );
            interactorXref.setParent(protein);
            ux.add( interactorXref );

        }

        if ( log.isDebugEnabled() ) {
            log.debug( "Built " + ux.size() + " UniProt Xref(s)." );
        }

        return XrefUpdaterUtils.updateXrefCollection( protein, uniprot, ux );
    }

    public static void updateProteinTranscriptUniprotXrefs( Protein intactTranscript,
                                                        UniprotProteinTranscript uniprotProteinTranscript,
                                                        UniprotProtein uniprotProtein ) {

        CvDatabase uniprot = CvHelper.getDatabaseByMi( CvDatabase.UNIPROT_MI_REF );
        CvXrefQualifier identity = CvHelper.getQualifierByMi( CvXrefQualifier.IDENTITY_MI_REF );
        CvXrefQualifier secondaryAc = CvHelper.getQualifierByMi( CvXrefQualifier.SECONDARY_AC_MI_REF );
        Institution owner = CvHelper.getInstitution();

        String dbRelease = uniprotProtein.getReleaseVersion();

        if ( log.isDebugEnabled() ) {
            log.debug( "Building UniProt Xref collection prior to update of " + intactTranscript.getShortLabel() );
        }
        Collection<Xref> ux = new ArrayList<Xref>( uniprotProteinTranscript.getSecondaryAcs().size() + 1 );
        ux.add( new InteractorXref( owner, uniprot, uniprotProteinTranscript.getPrimaryAc(), null, dbRelease, identity ) );

        for ( String ac : uniprotProteinTranscript.getSecondaryAcs() ) {
            ux.add( new InteractorXref( owner, uniprot, ac, null, dbRelease, secondaryAc ) );
        }

        if ( log.isDebugEnabled() ) {
            log.debug( "Built " + ux.size() + " Xref(s)." );
        }

        XrefUpdaterUtils.updateXrefCollection( intactTranscript, uniprot, ux );
    }

    public static void updateFeatureChainUniprotXrefs( Protein intactChain,
                                                       UniprotFeatureChain uniprotFeatureChain,
                                                       UniprotProtein uniprotProtein ) {

        CvDatabase uniprot = CvHelper.getDatabaseByMi( CvDatabase.UNIPROT_MI_REF );
        CvXrefQualifier identity = CvHelper.getQualifierByMi( CvXrefQualifier.IDENTITY_MI_REF );
        CvXrefQualifier secondaryAc = CvHelper.getQualifierByMi( CvXrefQualifier.SECONDARY_AC_MI_REF );
        Institution owner = CvHelper.getInstitution();

        String dbRelease = uniprotProtein.getReleaseVersion();

        if ( log.isDebugEnabled() ) {
            log.debug( "Building UniProt Xref collection prior to update of " + intactChain.getShortLabel() );
        }
        Collection<Xref> ux = new ArrayList<Xref>();
        ux.add( new InteractorXref( owner, uniprot, uniprotFeatureChain.getPrimaryAc(), null, dbRelease, identity ) );

        XrefUpdaterUtils.updateXrefCollection( intactChain, uniprot, ux );
    }
}