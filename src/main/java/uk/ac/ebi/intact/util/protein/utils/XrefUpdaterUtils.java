/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.protein.utils;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.CvObjectDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.ProteinUtils;
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
        Institution owner = db.getOwner();
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

    public static Map<String, Collection<InteractorXref>> clusterInteractorXrefByDatabaseName( Collection<InteractorXref> xrefs ) {

        Map<String, Collection<InteractorXref>> xrefCluster = new HashMap<String, Collection<InteractorXref>>( xrefs.size() );

        for ( InteractorXref cr : xrefs ) {
            Collection<InteractorXref> c = xrefCluster.get( cr.getCvDatabase().getIdentifier());
            if ( c == null ) {
                // not found, add an entry
                c = new ArrayList<InteractorXref>();
                xrefCluster.put( cr.getCvDatabase().getIdentifier(), c );
            }
            c.add( cr );
        }

        return xrefCluster;
    }

    public static XrefUpdaterReport updateAllProteinTranscriptXrefs( Protein protein,
                                                                     UniprotProteinTranscript uniprotTranscript,
                                                                     UniprotProtein uniprot,
                                                                     DataContext context,
                                                                     ProteinUpdateProcessor processor
    ) {

        List<Xref> deletedXrefs = new ArrayList<Xref>();

        // UPDATE UNIPROT XREFS
        XrefUpdaterReport reports = updateProteinTranscriptUniprotXrefs(protein, uniprotTranscript, uniprot, context, processor);

        // CHECK THAT ALL INTACT XREF STILL EXIST IN UNIPROT, OTHERWISE DELETE THEM
        List<InteractorXref> refs = new ArrayList<InteractorXref>(protein.getXrefs());
        for (InteractorXref xref : refs) {

            // Dont's check uniprot xref as those one have been done earlier and as anyway, the uniprot Xref won't have
            // uniprot xref as intact protein does (the uniprot xref of the intact protein correspond to the primary and
            // secondary ac of the proteins).
            CvDatabase cvDb = xref.getCvDatabase();
            String cvDbMi = cvDb.getMiIdentifier();

            if(CvDatabase.UNIPROT_MI_REF.equals(cvDbMi) || CvDatabase.INTACT_MI_REF.equals(cvDbMi)){
                continue;
            }
            else {
                deletedXrefs.add(xref);
                ProteinTools.deleteInteractorXRef(protein, context, xref);
            }
        }

        if (reports == null && !deletedXrefs.isEmpty()){
            reports = new XrefUpdaterReport(protein, Collections.EMPTY_LIST, deletedXrefs);
        }
        else if (reports != null && !deletedXrefs.isEmpty()){
            reports.getRemovedXrefs().addAll(deletedXrefs);
        }

        return reports;
    }

    public static XrefUpdaterReport updateAllXrefs( Protein protein,
                                                    UniprotProtein uniprotProtein,
                                                    Map<String, String> databaseName2mi,
                                                    DataContext context,
                                                    ProteinUpdateProcessor processor
    ) {

        Map<String, Collection<InteractorXref>> xrefCluster = XrefUpdaterUtils.clusterInteractorXrefByDatabaseName(protein.getXrefs());
        Map<String, Collection<UniprotXref>> uniprotCluster = XrefUpdaterUtils.clusterByDatabaseName(uniprotProtein.getCrossReferences());

        List<Xref> createdXrefs = new ArrayList<Xref>(uniprotProtein.getCrossReferences().size());
        List<Xref> deletedXrefs = new ArrayList<Xref>(protein.getXrefs().size());
        DaoFactory daoFactory = context.getDaoFactory();
        CvObjectDao<CvDatabase> dbDao = daoFactory.getCvObjectDao( CvDatabase.class );

        // create missing xrefs
        for ( Map.Entry<String, Collection<UniprotXref>> entry : uniprotCluster.entrySet() ) {

            String db = entry.getKey();
            Collection<UniprotXref> uniprotXrefs = entry.getValue();

            // search by shortlabel is dodgy ! Try mapping to MI:xxxx first.
            String mi = databaseName2mi.get( db.toLowerCase() );

            // we update xrefs excepted uniprot and intact
            if (mi != null && !mi.equals(CvDatabase.UNIPROT_MI_REF) && !mi.equals(CvDatabase.INTACT_MI_REF)){

                if (xrefCluster.containsKey(mi)){
                    Collection<InteractorXref> interactorXrefs = xrefCluster.get(mi);

                    CvDatabase database = interactorXrefs.iterator().next().getCvDatabase();

                    // delete xrefs not in uniprot
                    for (InteractorXref xref : interactorXrefs){

                        String primaryId = xref.getPrimaryId();

                        UniprotXref match = null;
                        for (UniprotXref uniref : uniprotXrefs){
                            if (uniref.getAccession().equalsIgnoreCase(primaryId)){
                                match = uniref;
                            }
                        }

                        if (match != null){
                            deletedXrefs.add(xref);
                            ProteinTools.deleteInteractorXRef(protein, context, xref);
                        }
                        else {
                            // we can remove this uniprot ref, we don't need it anymore
                            uniprotXrefs.remove(match);
                        }
                    }

                    // create new xrefs with resting uniprot xrefs
                    for (UniprotXref uniprotRef : uniprotXrefs){
                        InteractorXref newXref = new InteractorXref(IntactContext.getCurrentInstance().getInstitution(), database, uniprotRef.getAccession(), null);
                        protein.addXref(newXref);

                        daoFactory.getXrefDao(InteractorXref.class).persist(newXref);

                        createdXrefs.add(newXref);
                    }
                }
                else {
                    CvDatabase database = dbDao.getByPsiMiRef( mi );

                    if (database != null){

                        for (UniprotXref ref : uniprotXrefs){
                            InteractorXref newXref = new InteractorXref(IntactContext.getCurrentInstance().getInstitution(), database, ref.getAccession(), null);
                            protein.addXref(newXref);

                            daoFactory.getXrefDao(InteractorXref.class).persist(newXref);

                            createdXrefs.add(newXref);
                        }

                    }
                    else {
                        log.debug("We are not copying across xref to " + db);
                    }
                }

            }
            else {
                log.debug("We are not copying across xref to " + db);

                // delete xrefs if exist for this database
                if (xrefCluster.containsKey(mi)){
                    deletedXrefs.addAll(xrefCluster.get(mi));
                }
            }
        }

        // we delete all xrefs which are in intact but not in uniprot excepted intact plus uniprot
        Collection<String> miToDelete = CollectionUtils.subtract(xrefCluster.keySet(), uniprotCluster.keySet());

        for (String mi : miToDelete){
            if (!mi.equals(CvDatabase.UNIPROT_MI_REF) && !mi.equals(CvDatabase.INTACT_MI_REF) && xrefCluster.containsKey(mi)){

                for (InteractorXref x : xrefCluster.get(mi)){
                    deletedXrefs.add(x);
                    ProteinTools.deleteInteractorXRef(protein, context, x);
                }
            }
        }

        // update uniprot xrefs

        XrefUpdaterReport report = updateUniprotXrefs(protein, uniprotProtein, context, processor);

        if (report == null && (!createdXrefs.isEmpty() || !deletedXrefs.isEmpty())){
            report = new XrefUpdaterReport(protein, createdXrefs, deletedXrefs);

        }
        else if (report != null && (!createdXrefs.isEmpty() || !deletedXrefs.isEmpty())) {
            report.getAddedXrefs().addAll(createdXrefs);
            report.getRemovedXrefs().addAll(deletedXrefs);
        }
        return report;
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
    public static XrefUpdaterReport updateXrefCollection( Protein protein, CvDatabase database, Collection<Xref> newXrefs, DataContext context, ProteinUpdateProcessor processor) {

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
            addNewXref( protein, xref, context );
        }

        for ( Xref xref : toDelete ) {
            // delete remaining outdated xrefs
            if( log.isDebugEnabled() ) {
                log.debug( "DELETING: " + xref );
            }
            Collection<InteractorXref> refs = new ArrayList(protein.getXrefs());
            Iterator<InteractorXref> protXrefsIterator = refs.iterator();
            while (protXrefsIterator.hasNext()) {
                InteractorXref interactorXref =  protXrefsIterator.next();

                if (interactorXref.getPrimaryId().equals(xref.getPrimaryId())) {
                    ProteinTools.deleteInteractorXRef(protein, context, interactorXref);
                }

            }
        }

        return new XrefUpdaterReport(protein,
                toCreate.toArray(new Xref[toCreate.size()]),
                toDelete.toArray(new Xref[toDelete.size()]));
    }

    private static boolean addNewXref( AnnotatedObject current, final Xref xref, DataContext context) {
        // Make sure the xref does not yet exist in the object
        if ( current.getXrefs().contains( xref ) ) {
            log.debug( "SKIPPED: [" + xref + "] already exists" );
            return false; // quit
        }

        // add the xref to the AnnotatedObject
        context.getDaoFactory()
                .getXrefDao().persist(xref);
        current.addXref( xref );

        return true;
    }

    public static InteractorXref getOlderUniprotIdentity(List<InteractorXref> uniprotIdentities){
        if (uniprotIdentities.isEmpty()){
            return null;
        }
        Iterator<InteractorXref> iterator = uniprotIdentities.iterator();

        InteractorXref older = iterator.next();
        while(iterator.hasNext()){
            InteractorXref ref = iterator.next();

            if (ref.getCreated().before(older.getCreated())){
                older = ref;
            }
        }

        return older;
    }

    public static XrefUpdaterReport fixDuplicateOfSameUniprotIdentity(List<InteractorXref> uniprotIdentities, Protein prot, DataContext context, ProteinUpdateProcessor processor){
        InteractorXref original = getOlderUniprotIdentity(uniprotIdentities);

        ProteinDao proteinDao =  context.getDaoFactory().getProteinDao();
        Collection<InteractorXref> deletedDuplicate = new ArrayList<InteractorXref>();

        if (original != null){
            for (InteractorXref ref : uniprotIdentities){
                if (ref.getPrimaryId() != null && ref.getPrimaryId().equalsIgnoreCase(original.getPrimaryId())){
                    deletedDuplicate.add(ref);
                    ProteinTools.deleteInteractorXRef(prot, context, ref);
                }
                else if (ref.getPrimaryId() == null && original.getPrimaryId() == null){
                    deletedDuplicate.add(ref);
                    ProteinTools.deleteInteractorXRef(prot, context, ref);
                }
            }
        }

        XrefUpdaterReport report = new XrefUpdaterReport(prot, new Xref []{}, deletedDuplicate.toArray(new Xref[deletedDuplicate.size()]));

        proteinDao.update((ProteinImpl) prot);

        return report;
    }

    public static XrefUpdaterReport updateUniprotXrefs( Protein protein, UniprotProtein uniprotProtein, DataContext context, ProteinUpdateProcessor processor ) {
        XrefUpdaterReport reports = null;

        List<Xref> deletedXrefs = new ArrayList<Xref>(protein.getXrefs().size());
        List<Xref> createdXrefs = new ArrayList<Xref>(uniprotProtein.getCrossReferences().size());

        List<InteractorXref> uniprotIdentities = ProteinTools.getAllUniprotIdentities(protein);
        if (uniprotIdentities.size() > 1){
            XrefUpdaterReport intermediary = fixDuplicateOfSameUniprotIdentity(uniprotIdentities, protein, context, processor);
            if (intermediary != null){
                reports = intermediary;
            }
        }

        CvDatabase uniprot;
        CvXrefQualifier identity;
        CvXrefQualifier secondary = null;
        String dbRelease = uniprotProtein.getReleaseVersion();

        InteractorXref uniprotXref = ProteinUtils.getUniprotXref(protein);
        Institution owner = CvHelper.getInstitution();

        if (uniprotXref != null){
            uniprot = uniprotXref.getCvDatabase();
            identity = uniprotXref.getCvXrefQualifier();
        }
        else {
            uniprot = CvHelper.getDatabaseByMi( CvDatabase.UNIPROT_MI_REF );
            identity = CvHelper.getQualifierByMi( CvXrefQualifier.IDENTITY_MI_REF );

            uniprotXref = new InteractorXref( owner, uniprot, uniprotProtein.getPrimaryAc(), null, dbRelease, identity );
            protein.addXref(uniprotXref);

            context.getDaoFactory().getXrefDao(InteractorXref.class).persist(uniprotXref);

            createdXrefs.add(uniprotXref);
        }

        log.debug( "Found " + uniprotProtein.getSecondaryAcs().size() + " secondary ACs" );
        // update secondary xrefs
        List<String> secondaryUniprot = new ArrayList<String>(uniprotProtein.getSecondaryAcs());
        for ( InteractorXref xref : protein.getXrefs() ) {
            if (xref.getCvDatabase() != null && xref.getCvDatabase().getIdentifier().equals(CvDatabase.UNIPROT_MI_REF)){
                if (xref.getCvXrefQualifier() != null && xref.getCvXrefQualifier().getIdentifier().equals(CvXrefQualifier.SECONDARY_AC_MI_REF)){
                    secondary = xref.getCvXrefQualifier();

                    // secondary xref already exists
                    if (secondaryUniprot.contains(xref.getPrimaryId())){
                        secondaryUniprot.remove(xref.getPrimaryId());
                    }
                    // secondary xref does not exist, we dele it
                    else {
                        deletedXrefs.add(xref);
                        ProteinTools.deleteInteractorXRef(protein, context, xref);
                    }
                }
            }
        }

        if (secondary == null){
            secondary = CvHelper.getQualifierByMi(CvXrefQualifier.SECONDARY_AC_MI_REF);
        }
        // create missing secondary xrefs
        for (String secondaryToAdd : secondaryUniprot){
            InteractorXref interactorXref =   new InteractorXref( owner, uniprot, secondaryToAdd, null, dbRelease, secondary );
            protein.addXref(interactorXref);

            context.getDaoFactory().getXrefDao(InteractorXref.class).persist(interactorXref);
            createdXrefs.add( interactorXref );
        }

        if (reports == null && (!createdXrefs.isEmpty() || !deletedXrefs.isEmpty())){
            reports = new XrefUpdaterReport(protein, createdXrefs, deletedXrefs);
        }
        else if (reports != null && (!createdXrefs.isEmpty() || !deletedXrefs.isEmpty())) {
            reports.getAddedXrefs().addAll(createdXrefs);
            reports.getRemovedXrefs().addAll(deletedXrefs);
        }
        return reports;
    }

    public static XrefUpdaterReport updateProteinTranscriptUniprotXrefs( Protein intactTranscript,
                                                                         UniprotProteinTranscript uniprotProteinTranscript,
                                                                         UniprotProtein uniprotProtein,
                                                                         DataContext context,
                                                                         ProteinUpdateProcessor processor) {
        XrefUpdaterReport reports = null;

        List<Xref> deletedXrefs = new ArrayList<Xref>(intactTranscript.getXrefs().size());
        List<Xref> createdXrefs = new ArrayList<Xref>(uniprotProteinTranscript.getSecondaryAcs().size() + 1);

        List<InteractorXref> uniprotIdentities = ProteinTools.getAllUniprotIdentities(intactTranscript);
        if (uniprotIdentities.size() > 1){
            XrefUpdaterReport intermediary = fixDuplicateOfSameUniprotIdentity(uniprotIdentities, intactTranscript, context, processor);
            if (intermediary != null){
                reports = intermediary;
            }
        }

        CvDatabase uniprot;
        CvXrefQualifier identity;
        CvXrefQualifier secondary = null;
        String dbRelease = uniprotProtein.getReleaseVersion();

        InteractorXref uniprotXref = ProteinUtils.getUniprotXref(intactTranscript);
        Institution owner = CvHelper.getInstitution();

        if (uniprotXref != null){
            uniprot = uniprotXref.getCvDatabase();
            identity = uniprotXref.getCvXrefQualifier();
        }
        else {
            uniprot = CvHelper.getDatabaseByMi( CvDatabase.UNIPROT_MI_REF );
            identity = CvHelper.getQualifierByMi( CvXrefQualifier.IDENTITY_MI_REF );

            uniprotXref = new InteractorXref( owner, uniprot, uniprotProteinTranscript.getPrimaryAc(), null, dbRelease, identity );
            intactTranscript.addXref(uniprotXref);

            context.getDaoFactory().getXrefDao(InteractorXref.class).persist(uniprotXref);

            createdXrefs.add(uniprotXref);
        }

        log.debug("Found " + uniprotProteinTranscript.getSecondaryAcs().size() + " secondary ACs");
        // update secondary xrefs
        List<String> secondaryUniprot = new ArrayList<String>(uniprotProteinTranscript.getSecondaryAcs());
        List<InteractorXref> refs = new ArrayList(intactTranscript.getXrefs()) ;
        for ( InteractorXref xref : refs ) {
            if (xref.getCvDatabase() != null && xref.getCvDatabase().getIdentifier().equals(CvDatabase.UNIPROT_MI_REF)){
                if (xref.getCvXrefQualifier() != null && xref.getCvXrefQualifier().getIdentifier().equals(CvXrefQualifier.SECONDARY_AC_MI_REF)){
                    secondary = xref.getCvXrefQualifier();

                    // secondary xref already exists
                    if (secondaryUniprot.contains(xref.getPrimaryId())){
                        secondaryUniprot.remove(xref.getPrimaryId());
                    }
                    // secondary xref does not exist, we dele it
                    else {
                        deletedXrefs.add(xref);
                        ProteinTools.deleteInteractorXRef(intactTranscript, context, xref);
                    }
                }
            }
        }

        if (secondary == null){
            secondary = CvHelper.getQualifierByMi(CvXrefQualifier.SECONDARY_AC_MI_REF);
        }
        // create missing secondary xrefs
        for (String secondaryToAdd : secondaryUniprot){
            InteractorXref interactorXref =   new InteractorXref( owner, uniprot, secondaryToAdd, null, dbRelease, secondary );
            intactTranscript.addXref(interactorXref);

            context.getDaoFactory().getXrefDao(InteractorXref.class).persist(interactorXref);
            createdXrefs.add( interactorXref );
        }

        if (reports == null && (!createdXrefs.isEmpty() || !deletedXrefs.isEmpty())){
            reports = new XrefUpdaterReport(intactTranscript, createdXrefs, deletedXrefs);
        }
        else if (reports != null && (!createdXrefs.isEmpty() || !deletedXrefs.isEmpty())){
            reports.getAddedXrefs().addAll(createdXrefs);
            reports.getRemovedXrefs().addAll(deletedXrefs);
        }
        return reports;
    }
}