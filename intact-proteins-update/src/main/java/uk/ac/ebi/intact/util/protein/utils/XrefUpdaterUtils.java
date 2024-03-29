/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */package uk.ac.ebi.intact.util.protein.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.persistence.dao.CvObjectDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.core.persistence.dao.XrefDao;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinLike;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;
import uk.ac.ebi.intact.uniprot.model.UniprotXref;
import uk.ac.ebi.intact.uniprot.service.DefaultCrossReferenceFilter;
import uk.ac.ebi.intact.util.protein.CvHelper;
import uk.ac.ebi.intact.util.protein.utils.comparator.InteractorXrefComparator;
import uk.ac.ebi.intact.util.protein.utils.comparator.UniprotXrefComparator;

import java.util.*;

import static uk.ac.ebi.intact.core.context.IntactContext.getCurrentInstance;

/**
 * Utilities for updating Xrefs.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 1.1.2
 */

//TODO: This class needs some refactoring, plenty of duplicated code
public final class XrefUpdaterUtils {

    //We keep for now the map of databases here, but the strategy of filtering should be revisit.
    private static final Map<String, String> databaseName2mi = new DefaultCrossReferenceFilter().getDb2Mi();

    private XrefUpdaterUtils() {
    }

    /**
     * Sets up a logger for that class.
     */
    public static final Log log = LogFactory.getLog( XrefUpdaterUtils.class );

    public static XrefUpdaterReport updateAllProteinXrefs(Protein intactProtein,
                                                          UniprotProtein uniprotProtein,
                                                          DataContext context) {
        return updateAllXrefs(intactProtein, uniprotProtein, uniprotProtein.getReleaseVersion(), context);

    }
    public static XrefUpdaterReport updateAllProteinTranscriptXrefs( Protein intactProtein,
                                                                     UniprotProteinTranscript uniprotTranscript,
                                                                     UniprotProtein master,
                                                                     DataContext context
    ) {
        return updateAllXrefs(intactProtein, uniprotTranscript, master.getReleaseVersion(), context);

    }

    private static XrefUpdaterReport updateAllXrefs(Protein intactProtein,
                                                    UniprotProteinLike uniprotProtein,
                                                    String releaseVersion,
                                                    DataContext context) {
        // update uniprot xrefs
        XrefUpdaterReport report = updateUniprotXrefs(intactProtein, uniprotProtein, releaseVersion, context);

        final TreeSet<InteractorXref> sortedInteractorXrefs = new TreeSet<>(new InteractorXrefComparator());
        sortedInteractorXrefs.addAll(intactProtein.getXrefs());
        Iterator<InteractorXref> intactInteractorXrefIterator = sortedInteractorXrefs.iterator();

        final TreeSet<UniprotXref> sortedUniprotXrefs = new TreeSet<>(new UniprotXrefComparator());
        sortedUniprotXrefs.addAll(uniprotProtein.getCrossReferences());
        Iterator<UniprotXref> uniprotXrefIterator = sortedUniprotXrefs.iterator();

        List<Xref> createdXrefs = new ArrayList<>(uniprotProtein.getCrossReferences().size());
        List<Xref> deletedXrefs = new ArrayList<>(intactProtein.getXrefs().size());

        DaoFactory daoFactory = context.getDaoFactory();
        CvObjectDao<CvDatabase> dbDao = daoFactory.getCvObjectDao(CvDatabase.class);
        CvObjectDao<CvXrefQualifier> qualifierDao = daoFactory.getCvObjectDao(CvXrefQualifier.class);
        XrefDao<InteractorXref> refDao = daoFactory.getXrefDao(InteractorXref.class);

        InteractorXref currentIntactXref = null;
        UniprotXref currentUniprotXref = null;
        String uniprotXrefDatabaseId = null;
        String uniprotXrefDatabaseName = null;
        CvDatabase intactCvDatabase = null;

        if (intactInteractorXrefIterator.hasNext() && uniprotXrefIterator.hasNext()){
            currentIntactXref = intactInteractorXrefIterator.next();
            currentUniprotXref = uniprotXrefIterator.next();

            uniprotXrefDatabaseName = currentUniprotXref.getDatabase().toLowerCase();
            uniprotXrefDatabaseId = databaseName2mi.get(uniprotXrefDatabaseName);
            intactCvDatabase = currentIntactXref.getCvDatabase();

            if (uniprotXrefDatabaseId != null && intactCvDatabase != null){
                do{
                    int dbComparator = intactCvDatabase.getIdentifier().compareTo(uniprotXrefDatabaseId);

                    if (dbComparator == 0) {
                        int acComparator = currentIntactXref.getPrimaryId().compareTo(currentUniprotXref.getAccession());

                        if (acComparator == 0) {
                            // We add the comparison for the secondaryId and qualifier because now we have added information on these fields,
                            // if it is not the same we remove the xref and we update it with UniProt one.
                            final String currentIntactXrefSecondaryId = currentIntactXref.getSecondaryId();
                            final CvXrefQualifier currentIntactXrefQualifier = currentIntactXref.getCvXrefQualifier();
                            final String currentUniprotXrefDescription = currentUniprotXref.getDescription();
                            final String currentUniprotXrefQualifier = currentUniprotXref.getQualifier();

                            // If all of them are null or equal we don't need to do anything,
                            // at soon as one qualifier or description is different we replace it with the one coming form Uniprot
                            if (!((currentIntactXrefSecondaryId == null && currentUniprotXrefDescription == null
                                    && currentIntactXrefQualifier == null && currentUniprotXrefQualifier == null)
                                    || ((currentIntactXrefSecondaryId != null && currentIntactXrefSecondaryId.equalsIgnoreCase(currentUniprotXrefDescription))
                                    && (currentIntactXrefQualifier!= null && currentIntactXrefQualifier.getShortLabel().equalsIgnoreCase(currentUniprotXrefQualifier))))) {

                                // If they are not exactly the same we take the value from UniProt because in principle it will be the most up to date one
                                deletedXrefs.add(currentIntactXref);
                                intactProtein.removeXref(currentIntactXref);
                                refDao.delete(currentIntactXref);

                                //Note: for Ensembl and GO we extract information about the description or type of identifier. For the moment that translate to the SecondaryId in the database.
                                CvXrefQualifier cvQualifier = qualifierDao.getByIdentifier(currentUniprotXrefQualifier);
                                if (cvQualifier == null) { // we try by shortlabel, useful because IA:XXX ids can be different depending of the database
                                    cvQualifier = qualifierDao.getByShortLabel(currentUniprotXrefQualifier);
                                }
                                InteractorXref newXref = new InteractorXref(
                                        getCurrentInstance().getInstitution(), intactCvDatabase, currentUniprotXref.getAccession(),
                                        currentUniprotXrefDescription, releaseVersion, cvQualifier);
                                intactProtein.addXref(newXref);
                                refDao.persist(newXref);
                                createdXrefs.add(newXref);
                            }

                            if (intactInteractorXrefIterator.hasNext() && uniprotXrefIterator.hasNext()) {
                                currentIntactXref = intactInteractorXrefIterator.next();
                                currentUniprotXref = uniprotXrefIterator.next();
                                uniprotXrefDatabaseName = currentUniprotXref.getDatabase().toLowerCase();
                                uniprotXrefDatabaseId = databaseName2mi.get(uniprotXrefDatabaseName);
                                intactCvDatabase = currentIntactXref.getCvDatabase();
                            } else {
                                currentIntactXref = null;
                                currentUniprotXref = null;
                                uniprotXrefDatabaseName = null;
                                uniprotXrefDatabaseId = null;
                                intactCvDatabase = null;
                            }
                        }
                        else if (acComparator < 0) {
                            //intact has no match in uniprot
                            if (!CvDatabase.UNIPROT_MI_REF.equalsIgnoreCase(intactCvDatabase.getIdentifier()) && !CvDatabase.INTACT_MI_REF.equalsIgnoreCase(intactCvDatabase.getIdentifier())
                                    && !(currentIntactXref.getCvXrefQualifier() != null && ("intact-secondary".equalsIgnoreCase(currentIntactXref.getCvXrefQualifier().getShortLabel())
                                    || CvXrefQualifier.SECONDARY_AC.equalsIgnoreCase(currentIntactXref.getCvXrefQualifier().getShortLabel())))){
                                deletedXrefs.add(currentIntactXref);
                                intactProtein.removeXref(currentIntactXref);
                                refDao.delete(currentIntactXref);
                            }

                            if (intactInteractorXrefIterator.hasNext()){
                                currentIntactXref = intactInteractorXrefIterator.next();
                                intactCvDatabase = currentIntactXref.getCvDatabase();
                            }
                            else {
                                currentIntactXref = null;
                                intactCvDatabase = null;
                            }
                        }
                        else {
                            //uniprot has no match in intact

                            //Note: for Ensembl and GO we extract information about the description or type of identifier. For the moment that translate to the SecondaryId ir qualifier in the database.
                            CvXrefQualifier cvQualifier = qualifierDao.getByIdentifier(currentUniprotXref.getQualifier());
                            if (cvQualifier == null) { // we try by shortlabel, useful because IA:XXX ids can be different depending of the database
                                cvQualifier = qualifierDao.getByShortLabel(currentUniprotXref.getQualifier());
                            }
                            InteractorXref newXref = new InteractorXref(getCurrentInstance().getInstitution(), intactCvDatabase, currentUniprotXref.getAccession(),
                                    currentUniprotXref.getDescription(), releaseVersion, cvQualifier);
                            intactProtein.addXref(newXref);

                            refDao.persist(newXref);

                            createdXrefs.add(newXref);

                            if (uniprotXrefIterator.hasNext()){
                                currentUniprotXref = uniprotXrefIterator.next();
                                uniprotXrefDatabaseName = currentUniprotXref.getDatabase().toLowerCase();
                                uniprotXrefDatabaseId = databaseName2mi.get(uniprotXrefDatabaseName);
                            }
                            else {
                                currentUniprotXref = null;
                                uniprotXrefDatabaseName = null;
                                uniprotXrefDatabaseId = null;
                            }
                        }
                    }
                    else if (dbComparator < 0) {
                        //intact has no match in uniprot
                        if (!CvDatabase.UNIPROT_MI_REF.equalsIgnoreCase(intactCvDatabase.getIdentifier()) && !CvDatabase.INTACT_MI_REF.equalsIgnoreCase(intactCvDatabase.getIdentifier())
                                && !(currentIntactXref.getCvXrefQualifier() != null && ("intact-secondary".equalsIgnoreCase(currentIntactXref.getCvXrefQualifier().getShortLabel())
                                || CvXrefQualifier.SECONDARY_AC.equalsIgnoreCase(currentIntactXref.getCvXrefQualifier().getShortLabel())))){
                            deletedXrefs.add(currentIntactXref);
                            intactProtein.removeXref(currentIntactXref);
                            refDao.delete(currentIntactXref);
                        }
                        if (intactInteractorXrefIterator.hasNext()){
                            currentIntactXref = intactInteractorXrefIterator.next();
                            intactCvDatabase = currentIntactXref.getCvDatabase();
                        }
                        else {
                            currentIntactXref = null;
                            intactCvDatabase = null;
                        }
                    }
                    else {
                        //uniprot has no match in intact
                        CvDatabase cvDb = dbDao.getByIdentifier(uniprotXrefDatabaseId);
                        if (cvDb == null) { // we try by shortlabel, useful because IA:XXX ids can be different can be different depending of the databas
                            cvDb = dbDao.getByShortLabel(uniprotXrefDatabaseName);
                        }

                        if (cvDb != null){
                            CvXrefQualifier cvQualifier = qualifierDao.getByIdentifier(currentUniprotXref.getQualifier());
                            if (cvQualifier == null) { // we try by shortlabel, useful because IA:XXX ids can be different depending of the database
                                cvQualifier = qualifierDao.getByShortLabel(currentUniprotXref.getQualifier());
                            }

                            InteractorXref newXref = new InteractorXref(getCurrentInstance().getInstitution(), cvDb, currentUniprotXref.getAccession(),
                                    currentUniprotXref.getDescription(), releaseVersion, cvQualifier);
                            intactProtein.addXref(newXref);
                            refDao.persist(newXref);
                            createdXrefs.add(newXref);
                        }
                        else {
                            log.debug("We are not copying across xref to " + uniprotXrefDatabaseId +". The database doesn't exist in IntAct");
                        }

                        if (uniprotXrefIterator.hasNext()){
                            currentUniprotXref = uniprotXrefIterator.next();
                            uniprotXrefDatabaseName = currentUniprotXref.getDatabase().toLowerCase();
                            uniprotXrefDatabaseId = databaseName2mi.get(uniprotXrefDatabaseName);
                        }
                        else {
                            currentUniprotXref = null;
                            uniprotXrefDatabaseName = null;
                            uniprotXrefDatabaseId = null;
                        }
                    }
                } while (currentIntactXref != null && currentUniprotXref != null && uniprotXrefDatabaseId != null && intactCvDatabase != null);
            }
        }

        if (currentIntactXref != null || intactInteractorXrefIterator.hasNext()){
            if (currentIntactXref == null ){
                currentIntactXref = intactInteractorXrefIterator.next();
                intactCvDatabase = currentIntactXref.getCvDatabase();
            }

            do {
                //intact has no match in uniprot
                if (intactCvDatabase == null || (intactCvDatabase != null && !CvDatabase.UNIPROT_MI_REF.equalsIgnoreCase(intactCvDatabase.getIdentifier()) && !CvDatabase.INTACT_MI_REF.equalsIgnoreCase(intactCvDatabase.getIdentifier())
                        && !(currentIntactXref.getCvXrefQualifier() != null && ("intact-secondary".equalsIgnoreCase(currentIntactXref.getCvXrefQualifier().getShortLabel())
                        || CvXrefQualifier.SECONDARY_AC.equalsIgnoreCase(currentIntactXref.getCvXrefQualifier().getShortLabel()))))){
                    deletedXrefs.add(currentIntactXref);

                    intactProtein.removeXref(currentIntactXref);

                    refDao.delete(currentIntactXref);
                }

                if (intactInteractorXrefIterator.hasNext()){
                    currentIntactXref = intactInteractorXrefIterator.next();
                    intactCvDatabase = currentIntactXref.getCvDatabase();
                }
                else {
                    currentIntactXref = null;
                    intactCvDatabase = null;
                }
            }while (currentIntactXref != null);
        }

        if (currentUniprotXref != null || uniprotXrefIterator.hasNext()){
            if (currentUniprotXref == null ){
                currentUniprotXref = uniprotXrefIterator.next();
                uniprotXrefDatabaseName = currentUniprotXref.getDatabase().toLowerCase();
                uniprotXrefDatabaseId = databaseName2mi.get(uniprotXrefDatabaseName);
            }

            if (uniprotXrefDatabaseId != null){
                do {
                    //uniprot has no match in intact
                    CvDatabase cvDb = dbDao.getByIdentifier(uniprotXrefDatabaseId);
                    if (cvDb == null) { // we try by shortlabel, useful because IA:XXX can be different
                        cvDb = dbDao.getByShortLabel(uniprotXrefDatabaseName);
                    }

                    if (cvDb != null){
                        CvXrefQualifier cvQualifier = qualifierDao.getByIdentifier(currentUniprotXref.getQualifier());
                        if (cvQualifier == null) { // we try by shortlabel, useful because IA:XXX ids can be different depending of the database
                            cvQualifier = qualifierDao.getByShortLabel(currentUniprotXref.getQualifier());
                        }

                        InteractorXref newXref = new InteractorXref(getCurrentInstance().getInstitution(), cvDb, currentUniprotXref.getAccession(),
                                currentUniprotXref.getDescription(), releaseVersion, cvQualifier);

                        intactProtein.addXref(newXref);
                        refDao.persist(newXref);
                        createdXrefs.add(newXref);

                    }
                    else {
                        log.debug("We are not copying across xref to " + uniprotXrefDatabaseId +". The database doesn't exist in IntAct");
                    }

                    if (uniprotXrefIterator.hasNext()){
                        currentUniprotXref = uniprotXrefIterator.next();
                        uniprotXrefDatabaseName = currentUniprotXref.getDatabase().toLowerCase();
                        uniprotXrefDatabaseId = databaseName2mi.get(uniprotXrefDatabaseName);
                    }
                    else {
                        currentUniprotXref = null;
                        uniprotXrefDatabaseName = null;
                        uniprotXrefDatabaseId = null;
                    }
                }
                while (currentUniprotXref != null && uniprotXrefDatabaseId != null);
            }
        }

        if (report == null && (!createdXrefs.isEmpty() || !deletedXrefs.isEmpty())){
            report = new XrefUpdaterReport(intactProtein, createdXrefs, deletedXrefs);

        }
        else if (report != null && (!createdXrefs.isEmpty() || !deletedXrefs.isEmpty())) {
            report.getAddedXrefs().addAll(createdXrefs);
            report.getRemovedXrefs().addAll(deletedXrefs);
        }

        return report;

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

    public static XrefUpdaterReport fixDuplicateOfSameUniprotIdentity(List<InteractorXref> uniprotIdentities, Protein prot, DataContext context){
        InteractorXref original = getOlderUniprotIdentity(uniprotIdentities);

        ProteinDao proteinDao =  context.getDaoFactory().getProteinDao();
        Collection<Xref> deletedDuplicate = new ArrayList<>();

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

        XrefUpdaterReport report = new XrefUpdaterReport(prot, Collections.emptyList(), deletedDuplicate);

        proteinDao.update((ProteinImpl) prot);

        return report;
    }

    public static XrefUpdaterReport updateUniprotXrefs( Protein protein, UniprotProtein uniprotProtein, DataContext context) {
        return updateUniprotXrefs(protein, uniprotProtein, uniprotProtein.getReleaseVersion(), context);
    }

    public static XrefUpdaterReport updateProteinTranscriptUniprotXrefs( Protein intactTranscript,
                                                                         UniprotProteinTranscript uniprotProteinTranscript,
                                                                         UniprotProtein uniprotProtein,
                                                                         DataContext context) {
        return updateUniprotXrefs(intactTranscript, uniprotProteinTranscript, uniprotProtein.getReleaseVersion(), context);
    }

    private static XrefUpdaterReport updateUniprotXrefs(Protein protein, UniprotProteinLike uniprotProtein, String releaseVersion, DataContext context) {
        XrefUpdaterReport reports = null;

        List<Xref> deletedXrefs = new ArrayList<>(protein.getXrefs().size());
        List<Xref> createdXrefs = new ArrayList<>(uniprotProtein.getCrossReferences().size());

        List<InteractorXref> uniprotIdentities = ProteinTools.getAllUniprotIdentities(protein);
        if (uniprotIdentities.size() > 1){
            reports = fixDuplicateOfSameUniprotIdentity(uniprotIdentities, protein, context);
        }

        CvDatabase uniprot;
        CvXrefQualifier identity;
        CvXrefQualifier secondary = null;

        InteractorXref uniprotXref = ProteinUtils.getUniprotXref(protein);
        Institution owner = CvHelper.getInstitution();

        if (uniprotXref != null){
            uniprot = uniprotXref.getCvDatabase();
        }
        else {
            uniprot = CvHelper.getDatabaseByMi( CvDatabase.UNIPROT_MI_REF);
            identity = CvHelper.getQualifierByMi( CvXrefQualifier.IDENTITY_MI_REF );

            uniprotXref = new InteractorXref( owner, uniprot, uniprotProtein.getPrimaryAc(), null, releaseVersion, identity );
            protein.addXref(uniprotXref);

            context.getDaoFactory().getXrefDao(InteractorXref.class).persist(uniprotXref);

            createdXrefs.add(uniprotXref);
        }

        log.debug( "Found " + uniprotProtein.getSecondaryAcs().size() + " secondary ACs" );
        // update secondary xrefs
        List<String> secondaryUniprot = new ArrayList<>(uniprotProtein.getSecondaryAcs());
        for ( InteractorXref xref : protein.getXrefs() ) {
            if (xref.getCvDatabase() != null && xref.getCvDatabase().getIdentifier().equals(CvDatabase.UNIPROT_MI_REF)){
                if (xref.getCvXrefQualifier() != null && xref.getCvXrefQualifier().getIdentifier().equals(CvXrefQualifier.SECONDARY_AC_MI_REF)){
                    secondary = xref.getCvXrefQualifier();

                    // secondary xref already exists
                    if (secondaryUniprot.contains(xref.getPrimaryId())){
                        secondaryUniprot.remove(xref.getPrimaryId());
                    }
                    // secondary xref does not exist, we delete it
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
            InteractorXref interactorXref =   new InteractorXref( owner, uniprot, secondaryToAdd, null, releaseVersion, secondary);
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


}