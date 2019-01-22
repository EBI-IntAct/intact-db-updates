package uk.ac.ebi.intact.dbupdate.cv;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.bridges.ontologymanager.MIOntologyAccess;
import psidev.psi.mi.jami.bridges.ontologymanager.MIOntologyManager;
import psidev.psi.mi.jami.bridges.ontologymanager.MIOntologyTermI;
import psidev.psi.tools.ontology_manager.impl.local.OntologyLoaderException;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.AnnotationDao;
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
import uk.ac.ebi.intact.dbupdate.cv.updater.*;
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
 * The cv update manager. Gives methods to update and import cvs
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

    private MIOntologyManager miOntologyManager;

    private File reportDirectory;

    private CvUpdateErrorFactory errorFactory;

    private CvParentUpdater basicParentUpdater;

    // to allow listener
    protected EventListenerList listeners = new EventListenerList();

    private CvUpdateContext updateContext;

    private Set<String> rootTermsToExclude;

    public CvUpdateManager(URL ontologyConfigPath) throws IOException, OntologyLoaderException {

        if (ontologyConfigPath == null){
            throw new IllegalArgumentException("The url to the ontology config file cannot be null.");
        }

        InputStream ontology = ontologyConfigPath.openStream();

        try{
            miOntologyManager = new MIOntologyManager(ontology);

            updateContext = new CvUpdateContext(this);
        }
        finally {
            ontology.close();
        }
    }

    public CvUpdateManager(URL ontologyConfigPath, String reportDirectoryName) throws IOException, OntologyLoaderException {
        if (ontologyConfigPath == null){
            throw new IllegalArgumentException("The url to the ontology config file cannot be null.");
        }
        if (reportDirectoryName == null){
            throw new IllegalArgumentException("The name of the directory where to write the reports cannot be null.");
        }

        InputStream ontology = ontologyConfigPath.openStream();

        try{
            reportDirectory = new File(reportDirectoryName);
            registerBasicListeners(reportDirectory);

            miOntologyManager = new MIOntologyManager(ontology);
            updateContext = new CvUpdateContext(this);
        }
        finally {
            ontology.close();
        }
    }

    public CvUpdateManager() throws IOException, OntologyLoaderException {

        InputStream ontology = CvUpdateManager.class.getResource("/ontologies.xml").openStream();

        try{
            miOntologyManager = new MIOntologyManager(ontology);
            updateContext = new CvUpdateContext(this);
        }
        finally {
            ontology.close();
        }
    }

    public CvUpdateManager(URL ontologyConfigPath, String reportDirectoryName, CvUpdater cvUpdater, CvImporter cvImporter, ObsoleteCvRemapper cvRemapper) throws IOException, OntologyLoaderException {
        if (ontologyConfigPath == null){
            throw new IllegalArgumentException("The url to the ontology config file cannot be null.");
        }
        if (reportDirectoryName == null){
            throw new IllegalArgumentException("The name of the directory where to write the reports cannot be null.");
        }

        InputStream ontology = ontologyConfigPath.openStream();

        try{
            reportDirectory = new File(reportDirectoryName);
            registerBasicListeners(reportDirectory);

            miOntologyManager = new MIOntologyManager(ontology);

            this.cvUpdater = cvUpdater;
            this.cvImporter = cvImporter;
            this.cvRemapper = cvRemapper;

            updateContext = new CvUpdateContext(this);
        }
        finally {
            ontology.close();
        }
    }

    public CvUpdateManager(CvUpdater cvUpdater, CvImporter cvImporter, ObsoleteCvRemapper cvRemapper) throws IOException, OntologyLoaderException {
        InputStream ontology = CvUpdateManager.class.getResource("/ontologies.xml").openStream();

        try{
            miOntologyManager = new MIOntologyManager(ontology);

            this.cvUpdater = cvUpdater;
            this.cvImporter = cvImporter;

            this.cvRemapper = cvRemapper;

            updateContext = new CvUpdateContext(this);
        }
        finally{
            ontology.close();
        }
    }

    /**
     * Checks if we have some duplicated terms in the ontology
     */
    public void checkDuplicatedCvTerms(MIOntologyAccess access){
        List<Object[]> duplicatedTerms = getDuplicatedCvObjects(access);

        for (Object [] object : duplicatedTerms){
            String duplicate1 = (String) object[0];
            String duplicate2 = (String) object[1];

            CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.duplicated_cv, duplicate1 + " is a duplicate of " + duplicate2 + "in the ontology " + access.getOntologyID(), null, null, null);

            UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
            fireOnUpdateError(evt);
        }

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

        if (rootTermsToExclude.contains(identifier)){
            throw new IllegalArgumentException("The root term " + identifier + " is excluded. We cannot import this term because too unspecific");
        }

        MIOntologyAccess access = getMiOntologyManager().getOntologyAccess(ontologyId);
        if (access == null){
            throw new IllegalArgumentException("The ontology identifier " + ontologyId + " is not recognized and we cannot import the cv object.");
        }
        updateContext.setOntologyAccess(access);

        MIOntologyTermI ontologyTerm = access.getTermForAccession(identifier);
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

            // update missing parents from other ontologies
            for (Map.Entry<String, Set<CvDagObject>> entry : getCvImporter().getMissingRootParents().entrySet()){
                createMissingParentsFor(entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            log.error("Impossible to import " + identifier, e);
            CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.fatal, "Cv object " + identifier + " cannot be imported into the database. Exception is " + ExceptionUtils.getFullStackTrace(e), identifier, null, null);

            UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
            fireOnUpdateError(evt);
        }

        return importedCv;
    }

    /**
     * For each missing parent, will import the parent and update the reported children
     */
    public void createMissingParentsFor(String missingAc, Set<CvDagObject> childrenToUpdate) throws CvUpdateException, IllegalAccessException, InstantiationException {

        updateContext.clear();
        updateContext.setIdentifier(missingAc);

        boolean hasFoundOntology = false;
        MIOntologyAccess access = null;

        for (String ontologyId : getMiOntologyManager().getOntologyIDs()){
            MIOntologyAccess otherAccess = getMiOntologyManager().getOntologyAccess(ontologyId);

            if (otherAccess != null && otherAccess.getDatabaseRegexp() != null){
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

            MIOntologyTermI ontologyTerm = access.getTermForAccession(missingAc);

            if (ontologyTerm != null && !rootTermsToExclude.contains(ontologyTerm.getTermAccession())){
                updateContext.setOntologyTerm(ontologyTerm);

                log.info("Importing missing parent cv " + ontologyTerm.getTermAccession());
                getCvImporter().importCv(updateContext, false);

                if (updateContext.getCvTerm() != null){

                    for (CvDagObject child : childrenToUpdate){
                        log.info("Updating child cv " + child.getAc() + ", label = " + child.getShortLabel() + ", identifier = " + child.getIdentifier());

                        // we reload the child because we need to update it. We cannot merge it to the session because if the collection of parent/children was not initialized and we change it,
                        // hibernate throw an assertion failure because cannot update the status of the lazy collection. We cannot merge an object with collection lazy, size > 0 and then update the collection
                        getCvUpdater().updateChildrenHavingMissingParent(child.getAc(), updateContext.getCvTerm().getAc());
                    }

                }
                else {
                    CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.impossible_import, "Cv object " + missingAc + " cannot be imported into the database", missingAc, null, null);

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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    /**
     * Imports all the children of a root term of a given ontology if the root term is not obsolete
     */
    public void importNonObsoleteRootAndChildren(MIOntologyAccess ontologyAccess, MIOntologyTermI subRoot) throws CvUpdateException, IllegalAccessException, InstantiationException {
        if (!rootTermsToExclude.contains(subRoot.getTermAccession())){
            updateContext.clear();
            updateContext.setOntologyAccess(ontologyAccess);
            updateContext.setOntologyTerm(subRoot);
            updateContext.setIdentifier(subRoot.getTermAccession());

            log.info("Importing missing child terms of " + subRoot.getTermAccession());

            getCvImporter().importCv(updateContext, true);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    /**
     * Collects cv intact acs associated with a specific ontology
     */
    public List<String> getValidCvObjects(String ontologyId){
        MIOntologyAccess ontologyAccess = miOntologyManager.getOntologyAccess(ontologyId);

        if (ontologyAccess != null){
            DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();
            DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();

            Query query = factory.getEntityManager().createQuery("select distinct c.ac from CvDagObject c where c.ac in " +
                    "(select distinct c2.ac from CvDagObject c2 join c2.xrefs as x " +
                    "where x.cvDatabase.identifier = :database and x.cvXrefQualifier.identifier = :identity) " +
                    "or c.ac in " +
                    "( select distinct c3.ac from CvDagObject c3 where c3.identifier like :ontologyLikeId)");
            query.setParameter("database", ontologyAccess.getDatabaseIdentifier());
            query.setParameter("identity", CvXrefQualifier.IDENTITY_MI_REF);
            query.setParameter("ontologyLikeId", ontologyAccess.getOntologyID() + ":%");

            return query.getResultList();
        }

        return Collections.EMPTY_LIST;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    /**
     * Collects cv intact acs for cv objects that do not have a identity xref to another ontology
     */
    public List<String> getValidCvObjectsWithoutIdentity(){
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        Query query = factory.getEntityManager().createQuery("select distinct c.ac from CvDagObject c where c.ac not in " +
                "(select distinct c2.ac from CvDagObject c2 join c2.xrefs as x " +
                "where x.cvDatabase.identifier <> :database and x.cvXrefQualifier.identifier = :identity) " +
                "or c.ac in " +
                "( select distinct c3.ac from CvDagObject c3 where c3.xrefs is empty)");
        query.setParameter("database", CvDatabase.INTACT_MI_REF);
        query.setParameter("identity", CvXrefQualifier.IDENTITY_MI_REF);

        return query.getResultList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    /**
     * Collects duplicated cv intact acs
     */
    public List<Object[]> getDuplicatedCvObjects(MIOntologyAccess ontologyAccess){
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        Query query = factory.getEntityManager().createQuery("select distinct c.ac, c2.ac from CvDagObject c join c.xrefs as x," +
                " CvDagObject c2 join c2.xrefs as x2 " +
                "where c.ac <> c2.ac " +
                "and c.objClass = c2.objClass " +
                "and c2.identifier = c.identifier " +
                "and c.identifier like :ontologyLikeId");
        query.setParameter("ontologyLikeId", ontologyAccess.getOntologyID() + ":%");

        return query.getResultList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    /**
     * Updated a cv object given its intact accession. This method will not create any missing parents
     */
    public void updateCv(String cvObjectAc, String ontologyId) throws CvUpdateException {

        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        CvDagObject cvObject = factory.getCvObjectDao(CvDagObject.class).getByAc(cvObjectAc);

        if (cvObject != null){
            updateCv(cvObject, ontologyId);
        }
        // fire an error because term ac cannot be found in the database
        else {
            CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.not_found_intact_ac, "Intact Ac does not match any Cv term in the database", null, cvObject.getAc(), null);

            UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
            fireOnUpdateError(evt);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    /**
     * Updated a IntAct cv object given its intact accession. This method will not create missing parents but will re-attach to existing parents when needed
     */
    public void updateIntactCv(String cvObjectAc, String ontologyId) throws CvUpdateException {

        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        CvDagObject cvObject = factory.getCvObjectDao(CvDagObject.class).getByAc(cvObjectAc);

        if (cvObject != null){
            updateIntactCv(cvObject, ontologyId);
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
    public void updateCv(CvDagObject cvObject, String ontologyId) throws CvUpdateException {

        MIOntologyAccess ontologyAccess = miOntologyManager.getOntologyAccess(ontologyId);

        if (ontologyAccess != null){
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

                MIOntologyTermI ontologyTerm = ontologyAccess.getTermForAccession(cvObject.getIdentifier());

                if (ontologyTerm != null){
                    Set<Class<? extends CvDagObject>> classesForTerm = cvImporter.findCvClassFor(ontologyTerm, ontologyAccess);

                    // set obsolete and ontology term of the context
                    boolean isObsolete = ontologyAccess.isObsolete(ontologyTerm);

                    if (!classesForTerm.isEmpty() && classesForTerm.contains(cvObject.getClass())){

                        this.updateContext.setOntologyTerm(ontologyTerm);
                        this.updateContext.setTermObsolete(isObsolete);

                        if (isObsolete){
                            getCvRemapper().remapObsoleteCvTerm(updateContext);
                        }

                        // if it has been remapped successfully to an existing cv but from another ontology so we stop the update of this cv here
                        // and finish later
                        if (updateContext.getOntologyTerm() != null){
                            getCvUpdater().updateTerm(this.updateContext);
                        }
                    }
                    // obsolete term lost its hierarchy so it is normal that we cannot find a cv class for it. Try to remap it
                    else if (classesForTerm.isEmpty() && isObsolete){
                        this.updateContext.setOntologyTerm(ontologyTerm);
                        this.updateContext.setTermObsolete(isObsolete);

                        getCvRemapper().remapObsoleteCvTerm(updateContext);

                        // if it has been remapped successfully to an existing cv but from another ontology so we stop the update of this cv here
                        // and finish later
                        if (updateContext.getOntologyTerm() != null){
                            getCvUpdater().updateTerm(this.updateContext);
                        }
                    }
                    else if (classesForTerm.isEmpty() && !isObsolete){
                        CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.cv_class_not_found, "Impossible to find a cv class for cv identity " + identity, identity, cvObject.getAc(), cvObject.getShortLabel());

                        UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
                        fireOnUpdateError(evt);
                    }
                    else {
                        CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.invalid_cv_class, "The cv " + cvObject.getAc() + " is of type " + cvObject.getClass().getCanonicalName() + " but the valid class type should be " + classesForTerm.iterator().next().getCanonicalName(), identity, cvObject.getAc(), cvObject.getShortLabel());

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
        else {
            throw new IllegalArgumentException("Cannot update terms of ontology " + ontologyId + ". The ontologies possible to update are in the configuration file (/resources/ontologies.xml)");
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    /**
     * Updates a Intact Cv object but does not create any missing parents.
     */
    public void updateIntactCv(CvDagObject cvObject, String ontologyId) throws CvUpdateException {

        MIOntologyAccess ontologyAccess = getMiOntologyManager().getOntologyAccess(ontologyId);
        // initialize context
        this.updateContext.clear();
        this.updateContext.setCvTerm(cvObject);
        this.updateContext.setOntologyAccess(ontologyAccess);

        if (ontologyAccess != null){
            log.info("Update IntAct cv " + cvObject.getAc() + ", label = " + cvObject.getShortLabel() + ", identifier = " + cvObject.getIdentifier());
            UpdatedEvent updateEvt = new UpdatedEvent(this, null, cvObject.getAc(), null, null, false);

            getBasicParentUpdater().updateParents(this.updateContext, updateEvt);
        }
        else {
            throw new IllegalArgumentException("Cannot update terms of ontology " + ontologyId + ". The ontologies possible to update are in the configuration file (/resources/ontologies.xml)");
        }
    }

    /**
     * Extracts the identifier of the cv term. Returns null if we don't have any identity xref
     * @param cvObject
     * @param ontologyAccess
     * @return
     */
    private String extractIdentityFrom(CvDagObject cvObject, MIOntologyAccess ontologyAccess) {
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

    public CvParentUpdater getBasicParentUpdater() {
        if (this.basicParentUpdater == null){
            basicParentUpdater = new CvIntactParentUpdaterImpl();
            ((CvIntactParentUpdaterImpl)basicParentUpdater).initializeClassMap();

        }
        return basicParentUpdater;
    }

    public void setBasicParentUpdater(CvParentUpdater basicParentUpdater) {
        this.basicParentUpdater = basicParentUpdater;
    }

    public CvImporter getCvImporter() {
        if (this.cvImporter == null){
            cvImporter = new CvImporterImpl();
            ((CvImporterImpl)cvImporter).initializeClassMap();
        }
        return cvImporter;
    }

    public void setCvImporter(CvImporter cvImporter) {
        this.cvImporter = cvImporter;
    }

    public ObsoleteCvRemapper getCvRemapper() {
        if (this.cvRemapper == null){
            cvRemapper = new ObsoleteCvRemapperImpl();
        }
        return cvRemapper;
    }

    public void setCvRemapper(ObsoleteCvRemapper cvRemapper) {
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

    public void registerBasicListeners() throws IOException {
        if (listeners.getListenerCount() == 0) {
            this.listeners.add(CvUpdateListener.class, new ReportWriterListener(reportDirectory));
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

    public MIOntologyManager getMiOntologyManager() {
        return miOntologyManager;
    }

    public void setReportDirectory(File reportDirectory) {
        this.reportDirectory = reportDirectory;
    }

    public Set<String> getRootTermsToExclude() {

        if (rootTermsToExclude == null){
            rootTermsToExclude = new HashSet<String>();
            rootTermsToExclude.add("MI:0000");

            // exclude cooperative interactions
            rootTermsToExclude.add("MI:1149");
            rootTermsToExclude.add("MI:1150");
            rootTermsToExclude.add("MI:1160");
            rootTermsToExclude.add("MI:1164");
            rootTermsToExclude.add("MI:1166");
            rootTermsToExclude.add("MI:1165");
            rootTermsToExclude.add("MI:1159");
            rootTermsToExclude.add("MI:1175");
            rootTermsToExclude.add("MI:1161");
            rootTermsToExclude.add("MI:1162");
            rootTermsToExclude.add("MI:1163");
            rootTermsToExclude.add("MI:1167");
            rootTermsToExclude.add("MI:1168");
            rootTermsToExclude.add("MI:1169");
            rootTermsToExclude.add("MI:1153");
            rootTermsToExclude.add("MI:1155");
            rootTermsToExclude.add("MI:1154");
            rootTermsToExclude.add("MI:1152");
            rootTermsToExclude.add("MI:1156");
            rootTermsToExclude.add("MI:1157");
            rootTermsToExclude.add("MI:1158");
            rootTermsToExclude.add("MI:1170");
            rootTermsToExclude.add("MI:1172");
            rootTermsToExclude.add("MI:1173");
            rootTermsToExclude.add("MI:1171");
            rootTermsToExclude.add("MI:1174");
        }

        return rootTermsToExclude;
    }

    public void setRootTermsToExclude(Set<String> rootTermsToExclude) {
        this.rootTermsToExclude = rootTermsToExclude;
    }

    public Map<String, Set<CvDagObject>> getRemappedObsoleteTermsToUpdate(){
        return new HashMap<String, Set<CvDagObject>>(this.cvRemapper.getRemappedCvToUpdate());
    }

    public Map<String, Set<CvDagObject>> getMissingParentsToCreate(){
        return new HashMap<String, Set<CvDagObject>>(this.cvUpdater.getMissingParents());
    }

    public Map<String, Set<CvDagObject>> getTermsFromOtherOntologiesToCreate(){
        return new HashMap<String, Set<CvDagObject>>(this.cvImporter.getMissingRootParents());
    }

    public MIOntologyAccess getIntactOntologyAccessFor(String ontologyId){
        return this.miOntologyManager.getOntologyAccess(ontologyId);
    }

    public void clear(){
        cvUpdater.clear();
        cvRemapper.clear();
        cvImporter.clear();

        this.updateContext.clear();
    }
}
