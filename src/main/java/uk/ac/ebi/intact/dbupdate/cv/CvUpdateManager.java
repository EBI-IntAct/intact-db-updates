package uk.ac.ebi.intact.dbupdate.cv;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.tools.ontology_manager.impl.local.OntologyLoaderException;
import uk.ac.ebi.intact.bridges.ontology_manager.IntactOntologyManager;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyAccess;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyTermI;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.AnnotationDao;
import uk.ac.ebi.intact.core.persistence.dao.CvObjectDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.cv.errors.CvUpdateError;
import uk.ac.ebi.intact.dbupdate.cv.errors.CvUpdateErrorFactory;
import uk.ac.ebi.intact.dbupdate.cv.errors.DefaultCvUpdateErrorFactory;
import uk.ac.ebi.intact.dbupdate.cv.errors.UpdateError;
import uk.ac.ebi.intact.dbupdate.cv.events.*;
import uk.ac.ebi.intact.dbupdate.cv.importer.CvImporter;
import uk.ac.ebi.intact.dbupdate.cv.importer.CvImporterImpl;
import uk.ac.ebi.intact.dbupdate.cv.listener.CvUpdateListener;
import uk.ac.ebi.intact.dbupdate.cv.listener.ReportWriterListener;
import uk.ac.ebi.intact.dbupdate.cv.remapper.ObsoleteCvRemapper;
import uk.ac.ebi.intact.dbupdate.cv.remapper.ObsoleteCvRemapperImpl;
import uk.ac.ebi.intact.dbupdate.cv.updater.CvUpdateException;
import uk.ac.ebi.intact.dbupdate.cv.updater.CvUpdater;
import uk.ac.ebi.intact.dbupdate.cv.updater.CvUpdaterImpl;
import uk.ac.ebi.intact.dbupdate.cv.utils.CvUpdateUtils;
import uk.ac.ebi.intact.model.*;

import javax.persistence.Query;
import javax.swing.event.EventListenerList;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;

/**
 * The cv update manager
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>11/11/11</pre>
 */
public class CvUpdateManager {
    private static final Log log = LogFactory.getLog(CvUpdateManager.class);

    private CvUpdater cvUpdater;

    private CvImporter cvImporter;

    private ObsoleteCvRemapper cvRemapper;

    private IntactOntologyManager intactOntologyManager;

    private File reportDirectory;

    private CvUpdateErrorFactory errorFactory;

    // to allow listener
    protected EventListenerList listeners = new EventListenerList();

    private CvUpdateContext updateContext;

    private Set<String> processedIntactAcs = new HashSet<String>();

    public CvUpdateManager(URL ontologyConfigPath) throws IOException, OntologyLoaderException {

        if (ontologyConfigPath == null){
            throw new IllegalArgumentException("The url to the ontology config file cannot be null.");
        }

        InputStream ontology = ontologyConfigPath.openStream();

        reportDirectory = new File("reports");
        registerBasicListeners(reportDirectory);

        intactOntologyManager = new IntactOntologyManager(ontology);

        updateContext = new CvUpdateContext(this);
    }

    public CvUpdateManager(URL ontologyConfigPath, String reportDirectoryName) throws IOException, OntologyLoaderException {
        if (ontologyConfigPath == null){
            throw new IllegalArgumentException("The url to the ontology config file cannot be null.");
        }
        if (reportDirectoryName == null){
            throw new IllegalArgumentException("The name of the directory where to write the reports cannot be null.");
        }

        InputStream ontology = ontologyConfigPath.openStream();

        reportDirectory = new File(reportDirectoryName);
        registerBasicListeners(reportDirectory);

        intactOntologyManager = new IntactOntologyManager(ontology);
        updateContext = new CvUpdateContext(this);
    }

    public CvUpdateManager() throws IOException, OntologyLoaderException {

        InputStream ontology = CvUpdateManager.class.getResource("/ontologies.xml").openStream();

        reportDirectory = new File("reports");
        registerBasicListeners(reportDirectory);

        intactOntologyManager = new IntactOntologyManager(ontology);
        updateContext = new CvUpdateContext(this);
    }

    public CvUpdateManager(URL ontologyConfigPath, String reportDirectoryName, CvUpdater cvUpdater, CvImporter cvImporter, ObsoleteCvRemapper cvRemapper) throws IOException, OntologyLoaderException {
        if (ontologyConfigPath == null){
            throw new IllegalArgumentException("The url to the ontology config file cannot be null.");
        }
        if (reportDirectoryName == null){
            throw new IllegalArgumentException("The name of the directory where to write the reports cannot be null.");
        }

        InputStream ontology = ontologyConfigPath.openStream();

        reportDirectory = new File(reportDirectoryName);
        registerBasicListeners(reportDirectory);

        intactOntologyManager = new IntactOntologyManager(ontology);

        this.cvUpdater = cvUpdater;
        this.cvImporter = cvImporter;
        this.cvRemapper = cvRemapper;

        updateContext = new CvUpdateContext(this);
    }

    public CvUpdateManager(CvUpdater cvUpdater, CvImporter cvImporter, ObsoleteCvRemapper cvRemapper) throws IOException, OntologyLoaderException {
        InputStream ontology = CvUpdateManager.class.getResource("/ontologies.xml").openStream();

        intactOntologyManager = new IntactOntologyManager(ontology);

        this.cvUpdater = cvUpdater;
        this.cvImporter = cvImporter;

        reportDirectory = new File("reports");
        registerBasicListeners(reportDirectory);

        this.cvRemapper = cvRemapper;

        updateContext = new CvUpdateContext(this);
    }

    /**
     * Update all MI and MOD terms.
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public void updateAll() throws IllegalAccessException, InstantiationException {
        clear();

        updateAndCreateAllTerms("MI");
        updateAllTerms("MOD");
    }

    /**
     * Update and create all the terms of a specific ontology
     * - update existing terms
     * - create missing parents
     * - create terms from the ontology which are not yet in IntAct
     * - update terms from other ontology if some obsolete terms have been remapped to them
     * - check for duplicates
     * @param ontologyId
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public void updateAndCreateAllTerms(String ontologyId) throws InstantiationException, IllegalAccessException {
        cvUpdater.clear();
        cvRemapper.clear();
        cvImporter.clear();
        this.updateContext.clear();

        // get the ontologyAcces for this ontology id
        IntactOntologyAccess ontologyAccess = this.intactOntologyManager.getOntologyAccess(ontologyId);
        this.updateContext.setOntologyAccess(ontologyAccess);

        if (ontologyAccess == null){
            throw new IllegalArgumentException("Cannot update terms of ontology " + ontologyId + ". The ontologies possible to update are in the configuration file (/resources/ontologies.xml)");
        }

        // update existing terms and remap obsolete terms if possible
        updateExistingTerms(ontologyId);

        // create missing parents
        createMissingParents();

        // create missing terms in ontology
        createMissingTerms(ontologyId);

        // update all remapped terms to other ontologies
        updateTermsRemappedToOtherOntologies();

        // check if duplicated terms exist
        checkDuplicatedCvTerms(ontologyAccess);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    /**
     * Checks if we have some duplicated terms in the ontology
     */
    public void checkDuplicatedCvTerms(IntactOntologyAccess access){
        List<Object[]> duplicatedTerms = getDuplicatedCvObjects(access);

        for (Object [] object : duplicatedTerms){
            String duplicate1 = (String) object[0];
            String duplicate2 = (String) object[1];

            CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.duplicated_cv, duplicate1 + " is a duplicate of " + duplicate2 + "in the ontology " + access.getOntologyID(), null, null, null);

            UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
            fireOnUpdateError(evt);
        }

    }

    /**
     * This method will update all the terms of a given ontology :
     * - update existing terms
     * - create missing parents
     * - update terms from other ontology if some obsolete terms have been remapped to them
     * - check for duplicates
     * @param ontologyId
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public void updateAllTerms(String ontologyId) throws InstantiationException, IllegalAccessException {
        cvUpdater.clear();
        cvRemapper.clear();
        cvImporter.clear();

        this.updateContext.clear();
        IntactOntologyAccess ontologyAccess = this.intactOntologyManager.getOntologyAccess(ontologyId);

        this.updateContext.setOntologyAccess(ontologyAccess);

        if (ontologyAccess == null){
            throw new IllegalArgumentException("Cannot update terms of ontology " + ontologyId + ". The ontologies possible to update are in the configuration file (/resources/ontologies.xml)");
        }

        // update existing terms
        updateExistingTerms(ontologyId);

        // create missing parents
        createMissingParents();

        // update all remapped terms to other ontologies
        updateTermsRemappedToOtherOntologies();

        // check if duplicated terms exist
        checkDuplicatedCvTerms(ontologyAccess);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    /**
     * Hide all the given cvs with the given message
     */
    public void hideTerms(Collection<CvObject> cvs, String message){
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();
        AnnotationDao annDao = factory.getAnnotationDao();

        for (CvObject cv : cvs){
            cv = factory.getEntityManager().merge(cv);
            boolean hasHidden = false;

            for (Annotation annotation : cv.getAnnotations()){
                if (annotation.getCvTopic() != null && CvTopic.HIDDEN.equalsIgnoreCase(annotation.getCvTopic().getShortLabel())){
                    hasHidden = true;
                }
            }

            if (!hasHidden){
                annDao.persist(CvUpdateUtils.hideTerm(cv, message));
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    /**
     * Delete hidden annotations for each cv given in the collection
     */
    public void removeHiddenFrom(Collection<CvObject> cvs){
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();
        AnnotationDao annDao = factory.getAnnotationDao();

        for (CvObject cv : cvs){
            cv = factory.getEntityManager().merge(cv);

            Collection<Annotation> annotations = new ArrayList<Annotation>(cv.getAnnotations());

            for (Annotation annotation : annotations){
                if (annotation.getCvTopic() != null && CvTopic.HIDDEN.equalsIgnoreCase(annotation.getCvTopic().getShortLabel())){
                    cv.removeAnnotation(annotation);
                    annDao.delete(annotation);
                }
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    /**
     * Import a new Cv. The cv is persisted
     * All the parents of this cv will be imported and hidden if not existing in the database
     */
    public CvDagObject importCvTerm(String identifier, String ontologyId, boolean includeChildren){
        clear();

        if (ontologyId == null){
            throw new IllegalArgumentException("The ontology id is mandatory. Can be MI or MOD");
        }
        if (identifier == null){
            throw new IllegalArgumentException("The identifier id is mandatory. Can be from MI or MOD");
        }

        IntactOntologyAccess access = intactOntologyManager.getOntologyAccess(ontologyId);
        if (access == null){
            throw new IllegalArgumentException("The ontology identifier " + ontologyId + " is not recognized and we cannot import the cv object.");
        }
        updateContext.setOntologyAccess(access);

        IntactOntologyTermI ontologyTerm = access.getTermForAccession(identifier);
        if (ontologyTerm == null){
            log.error("The term identifier " + identifier + " cannot be found in ontology " + ontologyId);
            return null;
        }
        updateContext.setOntologyTerm(ontologyTerm);
        updateContext.setIdentifier(identifier);

        CvDagObject importedCv = null;
        try {
            cvImporter.importCv(updateContext, includeChildren);

            importedCv = updateContext.getCvTerm();

            CvObjectDao<CvDagObject> cvDao = IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvDagObject.class);

            // update missing parents from other ontologies
            for (Map.Entry<String, Set<CvDagObject>> entry : cvImporter.getMissingRootParents().entrySet()){
                processMissingParent(cvDao, entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            log.error("Impossible to import " + identifier, e);
            CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.fatal, "Cv object " + identifier + " cannot be imported into the database", identifier, null, null);

            UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
            fireOnUpdateError(evt);
        }

        return importedCv;
    }

    /**
     * Create missing parents and update reported children
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private void createMissingParents() throws IllegalAccessException, InstantiationException {
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        CvObjectDao<CvDagObject> cvDao = factory.getCvObjectDao(CvDagObject.class);

        // import missing parents and update children
        for (Map.Entry<String, Set<CvDagObject>> missing : cvUpdater.getMissingParents().entrySet()){

            processMissingParent(cvDao, missing.getKey(), missing.getValue());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    /**
     * For each missing parent, will import the parent and update the reported children
     */
    private void processMissingParent(CvObjectDao<CvDagObject> cvDao, String missingAc, Set<CvDagObject> childrenToUpdate) throws InstantiationException, IllegalAccessException {
        updateContext.clear();
        updateContext.setIdentifier(missingAc);

        boolean hasFoundOntology = false;
        IntactOntologyAccess access = null;

        for (String ontologyId : intactOntologyManager.getOntologyIDs()){
            IntactOntologyAccess otherAccess = intactOntologyManager.getOntologyAccess(ontologyId);

            Matcher matcher = otherAccess.getDatabaseRegexp().matcher(missingAc);

            if (matcher.find() && matcher.group().equalsIgnoreCase(missingAc)){
                hasFoundOntology = true;
                if (access == null){
                    access = otherAccess;
                }
                else {
                    hasFoundOntology = false;
                }
            }
        }

        if (!hasFoundOntology){
            if (access == null){
                CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.ontology_access_not_found, "Cv object " + missingAc + " cannot be remapped to an existing ontology and " + childrenToUpdate.size() + " children cannot be updated.", missingAc, null, null);

                UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
                fireOnUpdateError(evt);

            }
            else {
                CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.several_matching_ontology_accesses, "Cv object " + missingAc + " cannot be remapped to a single existing ontology so " + childrenToUpdate.size() + " children cannot be updated.", missingAc, null, null);

                UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
                fireOnUpdateError(evt);

            }
        }
        else {
            updateContext.setOntologyAccess(access);

            IntactOntologyTermI ontologyTerm = access.getTermForAccession(missingAc);

            if (ontologyTerm != null){
                updateContext.setOntologyTerm(ontologyTerm);

                log.info("Importing missing parent cv " + ontologyTerm.getTermAccession());
                try {
                    cvImporter.importCv(updateContext, false);
                    processedIntactAcs.addAll(cvImporter.getProcessedTerms());

                    if (updateContext.getCvTerm() != null){
                        for (CvDagObject child : childrenToUpdate){
                            log.info("Updating child cv " + child.getAc() + ", label = " + child.getShortLabel() + ", identifier = " + child.getIdentifier());

                            IntactContext.getCurrentInstance().getDaoFactory().getEntityManager().merge(child);

                            child.addParent(updateContext.getCvTerm());
                            cvDao.update(child);
                        }

                    }
                    else {
                        CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.impossible_import, "Cv object " + missingAc + " cannot be imported into the database", missingAc, null, null);

                        UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
                        fireOnUpdateError(evt);
                    }
                } catch (Exception e) {
                    log.error("Impossible to import the missing parent " + missingAc);
                    CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.fatal, "Cv object " + missingAc + " cannot be imported", missingAc, null, null);

                    UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
                    fireOnUpdateError(evt);
                }
            }
            else {
                CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.non_existing_term, "Cv object " + missingAc + " does not exist in the ontology " + access.getOntologyID(), missingAc, null, null);

                UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
                fireOnUpdateError(evt);

            }
        }
    }

    /**
     * Create all the terms of a given ontology which do not exist in IntAct
     * @param ontologyId
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private void createMissingTerms(String ontologyId) throws IllegalAccessException, InstantiationException {
        cvImporter.getProcessedTerms().addAll(cvUpdater.getProcessedTerms());

        IntactOntologyAccess ontologyAccess = this.intactOntologyManager.getOntologyAccess(ontologyId);

        if (ontologyAccess != null){

            Collection<IntactOntologyTermI> rootTerms = ontologyAccess.getRootTerms();

            // import root terms if not obsolete
            for (IntactOntologyTermI root : rootTerms){

                importNonObsoleteRootAndChildren(ontologyAccess, root);
            }

            CvObjectDao<CvDagObject> cvDao = IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvDagObject.class);

            // update missing parents from other ontologies
            for (Map.Entry<String, Set<CvDagObject>> entry : cvImporter.getMissingRootParents().entrySet()){
                processMissingParent(cvDao, entry.getKey(), entry.getValue());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    /**
     * Imports all the children of a root term of a given ontology if the root term is not obsolete
     */
    private void importNonObsoleteRootAndChildren(IntactOntologyAccess ontologyAccess, IntactOntologyTermI root) throws InstantiationException, IllegalAccessException {
        if (!ontologyAccess.isObsolete(root)){
            updateContext.clear();
            updateContext.setOntologyAccess(ontologyAccess);
            updateContext.setOntologyTerm(root);
            updateContext.setIdentifier(root.getTermAccession());

            log.info("Importing missing child terms of " + root.getTermAccession());
            try {
                cvImporter.importCv(updateContext, true);

                processedIntactAcs.addAll(cvImporter.getProcessedTerms());

            } catch (Exception e) {
                CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.impossible_import, "Cv object " + root.getTermAccession() + " cannot be imported into the database", root.getTermAccession(), null, null);

                UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
                fireOnUpdateError(evt);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    /**
     * Updates all the terms which were obsolete and have been remapped to another ontology
     */
    private void updateTermsRemappedToOtherOntologies() throws IllegalAccessException, InstantiationException {

        updateContext.clear();

        for (Map.Entry<String, Set<CvDagObject>> entry : cvRemapper.getRemappedCvToUpdate().entrySet()){

            IntactOntologyAccess ontologyAccess = intactOntologyManager.getOntologyAccess(entry.getKey());

            if (ontologyAccess != null){

                for (CvDagObject cvObject : entry.getValue()){
                    if (!processedIntactAcs.contains(cvObject.getAc())){
                        processedIntactAcs.add(cvObject.getAc()) ;

                        log.info("Update remapped term " + cvObject.getAc() + ", label = " + cvObject.getShortLabel() + ", identifier = " + cvObject.getIdentifier());
                        IntactContext.getCurrentInstance().getDaoFactory().getEntityManager().merge(cvObject);
                        try {
                            updateCv(cvObject, ontologyAccess);
                        } catch (Exception e) {
                            CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.fatal, "Impossible to update the cv ", cvObject.getIdentifier(), cvObject.getAc(), cvObject.getShortLabel());

                            UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
                            fireOnUpdateError(evt);
                        }
                    }
                }
            }
            else {
                CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.ontology_access_not_found, "Impossible to find an ontology access for ontology id " + entry.getKey() + ". " + entry.getValue().size() + " remapped terms cannot be updated.", null, null, null);

                UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
                fireOnUpdateError(evt);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    /**
     * Collects cv intact acs associated with a specific ontology
     */
    private List<String> getValidCvObjects(IntactOntologyAccess ontologyAccess){
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();
        DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();

        Query query = factory.getEntityManager().createQuery("select distinct c.ac from CvDagObject c left join c.xrefs as x " +
                "where (x.cvDatabase.identifier = :database and x.cvXrefQualifier.identifier = :identity) " +
                "or REGEXP_LIKE( c.identifier, :ontologyLikeId )");
        query.setParameter("database", ontologyAccess.getDatabaseIdentifier());
        query.setParameter("identity", CvXrefQualifier.IDENTITY_MI_REF);
        query.setParameter("ontologyLikeId", ontologyAccess.getDatabaseRegexp().pattern());

        return query.getResultList();
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    /**
     * Collects duplicated cv intact acs
     */
    private List<Object[]> getDuplicatedCvObjects(IntactOntologyAccess ontologyAccess){
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();
        DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();

        Query query = factory.getEntityManager().createQuery("select c.ac, c2.ac from CvDagObject c left join c.xrefs as x, CvDagObject c2 left join c2.xrefs as x2 " +
                "where c.ac <> c2.ac " +
                "and c.objClass <> c2.objClass " +
                "and (x.cvDatabase.identifier = :database and x.cvXrefQualifier.identifier = :identity and " +
                "((x2.cvDatabase.identifier = :database and x2.cvXrefQualifier.identifier = :identity and x.primaryId = x2.primaryId) or x.primaryId = c2.identifier))" +
                "or (x2.cvDatabase.identifier = :database and x2.cvXrefQualifier.identifier = :identity and " +
                "x2.primaryId = c.identifier) or (c2.identifier = c.identifier and REGEXP_LIKE( c.identifier, :ontologyLikeId ))");
        query.setParameter("database", ontologyAccess.getDatabaseIdentifier());
        query.setParameter("identity", CvXrefQualifier.IDENTITY_MI_REF);

        return query.getResultList();
    }

    /**
     * Updates all existing cvs of a given ontology.
     */
    private void updateExistingTerms(String ontologyId){
        IntactOntologyAccess currentOntologyAccess = intactOntologyManager.getOntologyAccess(ontologyId);

        if (currentOntologyAccess != null){
            this.updateContext.setOntologyAccess(currentOntologyAccess);

            List<String> cvObjectAcs = getValidCvObjects(currentOntologyAccess);

            for (String validCv : cvObjectAcs){

                if (!processedIntactAcs.contains(validCv)){
                    try {
                        updateCv(validCv, currentOntologyAccess);
                    } catch (Exception e) {
                        log.error("Impossible to update the cv " + validCv, e);
                        CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.fatal, "Impossible to update this cv", null, validCv, null);

                        UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
                        fireOnUpdateError(evt);
                    }
                }
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    /**
     * Updated a cv object given its intact accession. This method will not create any missing parents
     */
    public void updateCv(String cvObjectAc, IntactOntologyAccess ontologyAccess) throws CvUpdateException {

        processedIntactAcs.add(cvObjectAc);

        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        CvDagObject cvObject = factory.getCvObjectDao(CvDagObject.class).getByAc(cvObjectAc);

        if (cvObject != null){
            updateCv(cvObject, ontologyAccess);
        }
        // fire an error because term ac cannot be found in the database
        else {
            CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.not_found_intact_ac, "Intact Ac does not match any Cv term in the database", null, cvObject.getAc(), null);

            UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
            fireOnUpdateError(evt);
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    /**
     * Updates a Cv object but does not create any missing parents.
     */
    public void updateCv(CvDagObject cvObject, IntactOntologyAccess ontologyAccess) throws CvUpdateException {

        log.info("Update cv " + cvObject.getAc() + ", label = " + cvObject.getShortLabel() + ", identifier = " + cvObject.getIdentifier());

        // initialize context
        this.updateContext.clear();
        this.updateContext.setCvTerm(cvObject);
        this.updateContext.setOntologyAccess(ontologyAccess);

        // set the identity and identity xref
        String identity = extractIdentityFrom(cvObject, ontologyAccess);

        // we can update the cv
        if (identity != null){
            updateContext.setIdentifier(identity);

            IntactOntologyTermI ontologyTerm = ontologyAccess.getTermForAccession(cvObject.getIdentifier());

            if (ontologyTerm != null){
                Class<? extends CvDagObject> classForTerm = cvImporter.findCvClassFor(ontologyTerm, ontologyAccess);

                if (classForTerm != null && cvObject.getClass().equals(classForTerm)){
                    // set obsolete and ontology term of the context
                    boolean isObsolete = ontologyAccess.isObsolete(ontologyTerm);

                    this.updateContext.setOntologyTerm(ontologyTerm);
                    this.updateContext.setTermObsolete(isObsolete);

                    if (isObsolete){
                        cvRemapper.remapObsoleteCvTerm(updateContext);
                    }

                    // if it has been remapped successfully to an existing cv but from another ontology so we stop the update of this cv here
                    // and finish later
                    if (updateContext.getOntologyTerm() != null){
                        cvUpdater.updateTerm(this.updateContext);
                    }
                }
                else if (classForTerm == null){
                    CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.cv_class_not_found, "Impossible to find a cv class for cv identity " + identity, identity, cvObject.getAc(), cvObject.getShortLabel());

                    UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
                    fireOnUpdateError(evt);
                }
                else {
                    CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.invalid_cv_class, "The cv " + cvObject.getAc() + " is of type " + cvObject.getClass().getCanonicalName() + " but the valid class type should be " + classForTerm.getCanonicalName(), identity, cvObject.getAc(), cvObject.getShortLabel());

                    UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
                    fireOnUpdateError(evt);
                }
            }
            else {
                CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.non_existing_term, "Cv object " + identity + " does not exist in the ontology " + ontologyAccess.getOntologyID(), identity, cvObject.getAc(), cvObject.getShortLabel());

                UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
                fireOnUpdateError(evt);
            }
        }
    }

    /**
     * Extracts the identifier of the cv term. Returns null if we don't have any identity xref
     * @param cvObject
     * @param ontologyAccess
     * @return
     */
    private String extractIdentityFrom(CvDagObject cvObject, IntactOntologyAccess ontologyAccess) {
        String identity = null;
        Collection<CvObjectXref> identities = CvUpdateUtils.extractIdentityXrefFrom(cvObject, ontologyAccess.getDatabaseIdentifier());

        if (identities.isEmpty()){
            identity = cvObject.getIdentifier();

            if (identity == null){
                CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.null_identifier, "The identifier of this object is null " + ontologyAccess.getDatabaseIdentifier(), identity, cvObject.getAc(), cvObject.getShortLabel());

                UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
                fireOnUpdateError(evt);
            }
        }
        else if (identities.size() == 1){
            updateContext.setIdentityXref(identities.iterator().next());

            identity = updateContext.getIdentityXref().getPrimaryId();
        }
        else {
            CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.multi_identities, "Cv object contains " + identities.size() + " different identity xrefs to " + ontologyAccess.getDatabaseIdentifier(), identity, cvObject.getAc(), cvObject.getShortLabel());

            UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
            fireOnUpdateError(evt);
        }
        return identity;
    }

    public CvUpdater getCvUpdater() {

        if (this.cvUpdater == null){
            cvUpdater = new CvUpdaterImpl();

        }
        return cvUpdater;
    }

    public void setCvUpdater(CvUpdater cvUpdater) {
        this.cvUpdater = cvUpdater;
    }

    public CvImporter getCvImporter() {
        if (this.cvImporter == null){
            cvImporter = new CvImporterImpl();
        }
        return cvImporter;
    }

    public void setCvImporter(CvImporter cvImporter) {
        this.cvImporter = cvImporter;
    }

    public ObsoleteCvRemapper getCvRemapper() {
        return cvRemapper;
    }

    public void setCvRemapper(ObsoleteCvRemapper cvRemapper) {

        if (this.cvRemapper == null){
            cvRemapper = new ObsoleteCvRemapperImpl();
        }
        this.cvRemapper = cvRemapper;
    }

    // listener methods

    public EventListenerList getListeners() {
        return listeners;
    }

    private void registerBasicListeners(File logDirectory) throws IOException {
        if (listeners.getListenerCount() == 0) {
            this.listeners.add(CvUpdateListener.class, new ReportWriterListener(logDirectory));
        }
    }

    public File getReportDirectory() {
        return reportDirectory;
    }

    public CvUpdateErrorFactory getErrorFactory() {

        if (errorFactory == null){
            errorFactory = new DefaultCvUpdateErrorFactory();
        }
        return errorFactory;
    }

    public void setErrorFactory(CvUpdateErrorFactory errorFactory) {
        this.errorFactory = errorFactory;
    }

    public void fireOnUpdateError(UpdateErrorEvent evt){

        for (CvUpdateListener listener : listeners.getListeners(CvUpdateListener.class)){
            listener.onUpdateError(evt);
        }
    }

    public void fireOnDeletedTerm(DeletedTermEvent evt){

        for (CvUpdateListener listener : listeners.getListeners(CvUpdateListener.class)){
            listener.onDeletedCvTerm(evt);
        }
    }

    public void fireOnUpdateCase(UpdatedEvent evt){

        for (CvUpdateListener listener : listeners.getListeners(CvUpdateListener.class)){
            listener.onUpdatedCvTerm(evt);
        }
    }

    public void fireOnObsoleteImpossibleToRemap(ObsoleteTermImpossibleToRemapEvent evt){

        for (CvUpdateListener listener : listeners.getListeners(CvUpdateListener.class)){
            listener.onObsoleteTermImpossibleToRemap(evt);
        }
    }

    public void fireOnRemappedObsolete(ObsoleteRemappedEvent evt){

        for (CvUpdateListener listener : listeners.getListeners(CvUpdateListener.class)){
            listener.onObsoleteRemappedTerm(evt);
        }
    }

    public void fireOnCreatedTerm(CreatedTermEvent evt){

        for (CvUpdateListener listener : listeners.getListeners(CvUpdateListener.class)){
            listener.onCreatedCvTerm(evt);
        }
    }

    public IntactOntologyManager getIntactOntologyManager() {
        return intactOntologyManager;
    }

    public void clear(){
        cvUpdater.clear();
        cvRemapper.clear();
        cvImporter.clear();

        this.updateContext.clear();
        this.processedIntactAcs.clear();
    }
}
