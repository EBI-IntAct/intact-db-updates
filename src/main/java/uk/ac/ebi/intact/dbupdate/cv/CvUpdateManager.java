package uk.ac.ebi.intact.dbupdate.cv;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.tools.ontology_manager.impl.local.OntologyLoaderException;
import psidev.psi.tools.ontology_manager.interfaces.OntologyAccess;
import uk.ac.ebi.intact.bridges.ontology_manager.IntactOntologyManager;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyAccess;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyTermI;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.CvObjectDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.cv.errors.CvUpdateError;
import uk.ac.ebi.intact.dbupdate.cv.errors.CvUpdateErrorFactory;
import uk.ac.ebi.intact.dbupdate.cv.errors.DefaultCvUpdateErrorFactory;
import uk.ac.ebi.intact.dbupdate.cv.errors.UpdateError;
import uk.ac.ebi.intact.dbupdate.cv.events.*;
import uk.ac.ebi.intact.dbupdate.cv.listener.CvUpdateListener;
import uk.ac.ebi.intact.dbupdate.cv.listener.ReportWriterListener;
import uk.ac.ebi.intact.model.CvDagObject;
import uk.ac.ebi.intact.model.CvObjectXref;
import uk.ac.ebi.intact.model.CvXrefQualifier;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.XrefUtils;

import javax.persistence.Query;
import javax.swing.event.EventListenerList;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * The cv update manager
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>11/11/11</pre>
 */
@Component
public class CvUpdateManager {
    private static final Log log = LogFactory.getLog(CvUpdateManager.class);

    private CvUpdater cvUpdater;
    private CvImporter cvImporter;
    private ObsoleteCvRemapper cvRemapper;

    private IntactOntologyManager intactOntologyManager;

    private Map<String, String> rooTerms;

    private File reportDirectory;

    private CvUpdateErrorFactory errorFactory;

    // to allow listener
    protected EventListenerList listeners = new EventListenerList();

    private CvUpdateContext updateContext;

    private Set<String> processedIntactAcs = new HashSet<String>();

    public CvUpdateManager() throws IOException, OntologyLoaderException {
        InputStream ontology = CvUpdateManager.class.getResource("/ontologies.xml").openStream();

        reportDirectory = new File("reports");
        registerBasicListeners(reportDirectory);

        intactOntologyManager = new IntactOntologyManager(ontology);

        cvUpdater = new CvUpdater();
        cvImporter = new CvImporter();
        cvRemapper = new ObsoleteCvRemapper();

        rooTerms = new HashMap<String, String>();
        initializeRootTerms();

        errorFactory = new DefaultCvUpdateErrorFactory();

        updateContext = new CvUpdateContext(this);
    }

    public CvUpdateManager(String reportDirectoryName) throws IOException, OntologyLoaderException {
        InputStream ontology = CvUpdateManager.class.getResource("/ontologies.xml").openStream();

        reportDirectory = new File(reportDirectoryName);
        registerBasicListeners(reportDirectory);

        intactOntologyManager = new IntactOntologyManager(ontology);

        cvUpdater = new CvUpdater();
        cvImporter = new CvImporter();
        cvRemapper = new ObsoleteCvRemapper();

        rooTerms = new HashMap<String, String>();
        initializeRootTerms();

        errorFactory = new DefaultCvUpdateErrorFactory();
        updateContext = new CvUpdateContext(this);
    }

    public CvUpdateManager(String reportDirectoryName, CvUpdater cvUpdater, CvImporter cvImporter, ObsoleteCvRemapper cvRemapper) throws IOException, OntologyLoaderException {
        InputStream ontology = CvUpdateManager.class.getResource("/ontologies.xml").openStream();

        reportDirectory = new File(reportDirectoryName);
        registerBasicListeners(reportDirectory);

        intactOntologyManager = new IntactOntologyManager(ontology);

        this.cvUpdater = cvUpdater != null ? cvUpdater : new CvUpdater();
        this.cvImporter = cvImporter != null ? cvImporter : new CvImporter();
        this.cvRemapper = cvRemapper != null ? cvRemapper : new ObsoleteCvRemapper();

        rooTerms = new HashMap<String, String>();
        initializeRootTerms();

        errorFactory = new DefaultCvUpdateErrorFactory();
        updateContext = new CvUpdateContext(this);
    }

    public CvUpdateManager(CvUpdater cvUpdater, CvImporter cvImporter, ObsoleteCvRemapper cvRemapper) throws IOException, OntologyLoaderException {
        InputStream ontology = CvUpdateManager.class.getResource("/ontologies.xml").openStream();

        intactOntologyManager = new IntactOntologyManager(ontology);

        this.cvUpdater = cvUpdater != null ? cvUpdater : new CvUpdater();
        this.cvImporter = cvImporter != null ? cvImporter : new CvImporter();
        this.cvRemapper = cvRemapper != null ? cvRemapper : new ObsoleteCvRemapper();

        rooTerms = new HashMap<String, String>();
        initializeRootTerms();

        errorFactory = new DefaultCvUpdateErrorFactory();
        updateContext = new CvUpdateContext(this);
    }

    private void initializeRootTerms(){
        rooTerms.put("MI", "MI:0000");
        rooTerms.put("MOD", "MOD:00000");
    }

    @Transactional(propagation = Propagation.SUPPORTS)
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
        updateExistingTerms();

        // create missing parents
        createMissingParents();

        // create missing terms in ontology
        createMissingTerms();

        // update all remapped terms to other ontologies
        updateTermsRemappedToOtherOntologies();

        // check if duplicated terms exist
        checkDuplicatedCvTerms(ontologyAccess);
    }

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

    @Transactional(propagation = Propagation.SUPPORTS)
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
        updateExistingTerms();

        // create missing parents
        createMissingParents();

        // update all remapped terms to other ontologies
        updateTermsRemappedToOtherOntologies();

        // check if duplicated terms exist
        checkDuplicatedCvTerms(ontologyAccess);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void updateAll() throws IllegalAccessException, InstantiationException {
        clear();

        updateAndCreateAllTerms("MI");
        updateAllTerms("MOD");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void createMissingParents() throws IllegalAccessException, InstantiationException {
        IntactOntologyAccess access = this.updateContext.getOntologyAccess();

        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        CvObjectDao<CvDagObject> cvDao = factory.getCvObjectDao(CvDagObject.class);

        // import missing parents and update children
        for (Map.Entry<String, List<CvDagObject>> missing : cvUpdater.getMissingParents().entrySet()){
            updateContext.clear();
            updateContext.setOntologyAccess(access);

            IntactOntologyTermI ontologyTerm = access.getTermForAccession(missing.getKey());

            if (ontologyTerm != null){
                updateContext.setOntologyTerm(ontologyTerm);

                log.info("Importing missing parent cv " + ontologyTerm.getTermAccession());
                cvImporter.importCv(updateContext, false);

                if (updateContext.getCvTerm() != null){
                    for (CvDagObject child : missing.getValue()){
                        log.info("Updating child cv " + child.getAc() + ", label = " + child.getShortLabel() + ", identifier = " + child.getIdentifier());

                        IntactContext.getCurrentInstance().getDaoFactory().getEntityManager().merge(child);

                        child.addParent(updateContext.getCvTerm());
                        cvDao.update(updateContext.getCvTerm());
                        cvDao.update(child);
                    }

                    processedIntactAcs.add(updateContext.getCvTerm().getAc());
                }
                else {
                    CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.impossible_import, "Cv object " + missing.getKey() + " cannot be imported into the database", missing.getKey(), null, null);

                    UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
                    fireOnUpdateError(evt);
                }
            }
            else {
                CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.non_existing_term, "Cv object " + missing.getKey() + " does not exist in the ontology " + access.getOntologyID(), missing.getKey(), null, null);

                UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
                fireOnUpdateError(evt);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void createMissingTerms() throws IllegalAccessException, InstantiationException {
        cvImporter.getProcessedTerms().addAll(cvUpdater.getProcessedTerms());

        IntactOntologyAccess ontologyAccess = this.updateContext.getOntologyAccess();
        Collection<IntactOntologyTermI> rootTerms = ontologyAccess.getRootTerms();

        for (IntactOntologyTermI root : rootTerms){
            updateContext.clear();
            updateContext.setOntologyAccess(ontologyAccess);
            updateContext.setOntologyTerm(root);

            log.info("Importing missing child terms of " + root.getTermAccession());
            cvImporter.importCv(updateContext, true);

            if (updateContext.getCvTerm() != null){
                processedIntactAcs.add(updateContext.getCvTerm().getAc());
            }
            else {
                CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.impossible_import, "Cv object " + root.getTermAccession() + " cannot be imported into the database", root.getTermAccession(), null, null);

                UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
                fireOnUpdateError(evt);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void updateTermsRemappedToOtherOntologies() throws IllegalAccessException, InstantiationException {
        updateContext.clear();

        for (Map.Entry<String, Set<CvDagObject>> entry : cvRemapper.getRemappedCvToUpdate().entrySet()){
            IntactOntologyAccess ontologyAccess = intactOntologyManager.getOntologyAccess(entry.getKey());

            if (ontologyAccess != null){
                updateContext.setOntologyAccess(ontologyAccess);

                for (CvDagObject cvObject : entry.getValue()){
                    if (!processedIntactAcs.contains(cvObject.getAc())){
                        log.info("Update remapped term " + cvObject.getAc() + ", label = " + cvObject.getShortLabel() + ", identifier = " + cvObject.getIdentifier());
                        IntactContext.getCurrentInstance().getDaoFactory().getEntityManager().merge(cvObject);
                        updateCv(cvObject, ontologyAccess);
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
    private List<String> getValidCvObjects(IntactOntologyAccess ontologyAccess){
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        Query query = factory.getEntityManager().createQuery("select distinct c.ac from CvDagObject c left join c.xrefs as x " +
                "where (x.cvDatabase.identifier = :database and x.cvXrefQualifier.identifier = :identity) " +
                "or REGEXP_LIKE( c.identifier, :ontologyLikeId )");
        query.setParameter("database", ontologyAccess.getDatabaseIdentifier());
        query.setParameter("identity", CvXrefQualifier.IDENTITY_MI_REF);
        query.setParameter("ontologyLikeId", ontologyAccess.getDatabaseRegexp().pattern());

        return query.getResultList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private List<Object[]> getDuplicatedCvObjects(IntactOntologyAccess ontologyAccess){
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        Query query = factory.getEntityManager().createQuery("select c.ac, c2.ac from CvDagObject c left join c.xrefs as x, CvDagObject c2 left join c2.xrefs as x2 " +
                "where c.ac <> c2.ac " +
                "and (x.cvDatabase.identifier = :database and x.cvXrefQualifier.identifier = :identity and " +
                "((x2.cvDatabase.identifier = :database and x2.cvXrefQualifier.identifier = :identity and x.primaryId = x2.primaryId) or x.primaryId = c2.identifier))" +
                "or (x2.cvDatabase.identifier = :database and x2.cvXrefQualifier.identifier = :identity and " +
                "x2.primaryId = c.identifier) or (c2.identifier = c.identifier and REGEXP_LIKE( c.identifier, :ontologyLikeId ))");
        query.setParameter("database", ontologyAccess.getDatabaseIdentifier());
        query.setParameter("identity", CvXrefQualifier.IDENTITY_MI_REF);

        return query.getResultList();
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    private void updateExistingTerms(){
        IntactOntologyAccess currentOntologyAccess = updateContext.getOntologyAccess();

        List<String> cvObjectAcs = getValidCvObjects(currentOntologyAccess);

        for (String validCv : cvObjectAcs){
            this.updateContext.clear();
            this.updateContext.setOntologyAccess(currentOntologyAccess);

            if (!processedIntactAcs.contains(validCv)){
                updateCv(validCv, currentOntologyAccess);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void updateCv(String cvObjectAc, IntactOntologyAccess ontologyAccess){

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
    private void updateCv(CvDagObject cvObject, IntactOntologyAccess ontologyAccess){

        log.info("Update cv " + cvObject.getAc() + ", label = " + cvObject.getShortLabel() + ", identifier = " + cvObject.getIdentifier());

        // initialize context
        this.updateContext.setCvTerm(cvObject);

        // set the identity and identity xref
        String identity = null;
        Collection<CvObjectXref> identities = extractIdentityXrefFrom(cvObject, ontologyAccess.getDatabaseIdentifier());

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

        // we can update the cv
        if (identity != null){
            IntactOntologyTermI ontologyTerm = ontologyAccess.getTermForAccession(cvObject.getIdentifier());

            if (ontologyTerm != null){
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
                    processedIntactAcs.add(cvObject.getAc());
                }
            }
            else {
                CvUpdateError error = errorFactory.createCvUpdateError(UpdateError.non_existing_term, "Cv object " + identity + " does not exist in the ontology " + ontologyAccess.getOntologyID(), identity, cvObject.getAc(), cvObject.getShortLabel());

                UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
                fireOnUpdateError(evt);
            }
        }
    }

    private Collection<CvObjectXref> extractIdentityXrefFrom(CvDagObject cv, String databaseId){
        Collection<CvObjectXref> existingIdentities = XrefUtils.getIdentityXrefs(cv);
        Collection<CvObjectXref> identities = new ArrayList<CvObjectXref>(existingIdentities.size());

        for (CvObjectXref ref : existingIdentities){
            if (ref.getCvDatabase() != null && ref.getCvDatabase().getIdentifier().equalsIgnoreCase(databaseId)){
                identities.add(ref);
            }
        }

        return identities;
    }

    public CvUpdater getCvUpdater() {
        return cvUpdater;
    }

    public void setCvUpdater(CvUpdater cvUpdater) {
        this.cvUpdater = cvUpdater;
    }

    public CvImporter getCvImporter() {
        return cvImporter;
    }

    public void setCvImporter(CvImporter cvImporter) {
        this.cvImporter = cvImporter;
    }

    public ObsoleteCvRemapper getCvRemapper() {
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

    public File getReportDirectory() {
        return reportDirectory;
    }

    public CvUpdateErrorFactory getErrorFactory() {
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

    public void clear(){
        cvUpdater.clear();
        cvRemapper.clear();
        cvImporter.clear();

        this.updateContext.clear();
        this.processedIntactAcs.clear();
    }
}
