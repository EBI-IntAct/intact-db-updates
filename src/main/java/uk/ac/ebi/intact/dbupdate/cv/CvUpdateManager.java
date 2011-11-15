package uk.ac.ebi.intact.dbupdate.cv;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.tools.ontology_manager.impl.local.OntologyLoaderException;
import uk.ac.ebi.intact.bridges.ontology_manager.IntactOntologyManager;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyAccess;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyTermI;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.CvObjectDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.cv.errors.CvUpdateErrorFactory;
import uk.ac.ebi.intact.dbupdate.cv.errors.DefaultCvUpdateErrorFactory;
import uk.ac.ebi.intact.dbupdate.cv.events.ObsoleteRemappedEvent;
import uk.ac.ebi.intact.dbupdate.cv.events.ObsoleteTermImpossibleToRemapEvent;
import uk.ac.ebi.intact.dbupdate.cv.events.UpdateErrorEvent;
import uk.ac.ebi.intact.dbupdate.cv.events.UpdatedEvent;
import uk.ac.ebi.intact.dbupdate.cv.listener.*;
import uk.ac.ebi.intact.model.CvDagObject;
import uk.ac.ebi.intact.model.CvXrefQualifier;

import javax.persistence.Query;
import javax.swing.event.EventListenerList;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private Map<String, String> rooTerms;

    private File reportDirectory;

    private CvUpdateErrorFactory errorFactory;

    // to allow listener
    protected EventListenerList listeners = new EventListenerList();

    private CvUpdateContext updateContext;

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

    @Transactional
    public void updateAndCreateAllTerms(String ontologyId) throws InstantiationException, IllegalAccessException {

        IntactOntologyAccess ontologyAccess = this.intactOntologyManager.getOntologyAccess(ontologyId);

        if (ontologyAccess == null){
            throw new IllegalArgumentException("Cannot update terms of ontology " + ontologyId + ". The ontologies possible to update are in the configuration file (/resources/ontologies.xml)");
        }

        // update existing terms
        cvUpdater.clear();

        updateExistingTerms(ontologyAccess);

        // create missing parents
        createMissingParents(ontologyAccess);

        // create missing terms in ontology
        createMissingTerms(ontologyAccess);
    }

    @Transactional
    public void updateAllTerms(String ontologyId) throws InstantiationException, IllegalAccessException {

        IntactOntologyAccess ontologyAccess = this.intactOntologyManager.getOntologyAccess(ontologyId);

        if (ontologyAccess == null){
            throw new IllegalArgumentException("Cannot update terms of ontology " + ontologyId + ". The ontologies possible to update are in the configuration file (/resources/ontologies.xml)");
        }

        // update existing terms
        cvUpdater.getMissingParents().clear();
        cvUpdater.getProcessedTerms().clear();

        updateExistingTerms(ontologyAccess);

        // create missing parents
        createMissingParents(ontologyAccess);
    }

    public void updateAll() throws IllegalAccessException, InstantiationException {
        updateAndCreateAllTerms("MI");
        updateAllTerms("MOD");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void createMissingParents(IntactOntologyAccess access) throws IllegalAccessException, InstantiationException {
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        CvObjectDao<CvDagObject> cvDao = factory.getCvObjectDao(CvDagObject.class);

        for (Map.Entry<String, List<CvDagObject>> missing : cvUpdater.getMissingParents().entrySet()){
            IntactOntologyTermI ontologyTerm = access.getTermForAccession(missing.getKey());

            cvImporter.importCv(ontologyTerm, access, false, factory);

            if (cvImporter.getImportedTerm() != null){
                for (CvDagObject child : missing.getValue()){
                    child.addParent(cvImporter.getImportedTerm());
                    cvDao.update(cvImporter.getImportedTerm());
                    cvDao.update(child);
                }
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void createMissingTerms(IntactOntologyAccess access) throws IllegalAccessException, InstantiationException {
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        CvObjectDao<CvDagObject> cvDao = factory.getCvObjectDao(CvDagObject.class);

        String root = rooTerms.get(access.getOntologyID());

        if (root != null){
            IntactOntologyTermI ontologyTermRoot = access.getTermForAccession(root);

            cvImporter.importCv(ontologyTermRoot, access, true, factory);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private List<String> getValidCvObjects(IntactOntologyAccess ontologyAccess){
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        Query query = factory.getEntityManager().createQuery("select c.ac from CvDagObject c join c.xrefs as x " +
                "where x.cvDatabase.identifier = :database and x.cvXrefQualifier.identifier = :identity and c.identifier = x.primaryId");
        query.setParameter("database", ontologyAccess.getDatabaseIdentifier());
        query.setParameter("identity", CvXrefQualifier.IDENTITY_MI_REF);

        return query.getResultList();
    }

    @Transactional
    private void updateExistingTerms(IntactOntologyAccess ontologyAccess){
        List<String> cvObjectAcs = getValidCvObjects(ontologyAccess);

        for (String validCv : cvObjectAcs){
            updateCv(validCv, ontologyAccess);
        }

        // deal with cvs having conflicts
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void updateCv(String cvObjectAc, IntactOntologyAccess ontologyAccess){
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        this.updateContext.clear();

        CvDagObject cvObject = factory.getCvObjectDao(CvDagObject.class).getByAc(cvObjectAc);

        if (cvObject != null){
            this.updateContext.setCvTerm(cvObject);
            this.updateContext.setOntologyAccess(ontologyAccess);

            IntactOntologyTermI ontologyTerm = ontologyAccess.getTermForAccession(cvObject.getIdentifier());

            if (ontologyTerm != null){
                boolean isObsolete = ontologyAccess.isObsolete(ontologyTerm);

                this.updateContext.setOntologyTerm(ontologyTerm);
                this.updateContext.setTermObsolete(isObsolete);

                boolean hasBeenRemapped = false;

                if (isObsolete){
                    cvRemapper.remapObsoleteCvTerm(updateContext);
                }

                if (!isObsolete || (isObsolete && !hasBeenRemapped)){
                    cvUpdater.updateTerm(this.updateContext);
                }
                else if (isObsolete && hasBeenRemapped){
                    IntactOntologyTermI remappedOntologyTerm = ontologyAccess.getTermForAccession(cvObject.getIdentifier());

                    if (remappedOntologyTerm != null){
                        cvUpdater.updateTerm(this.updateContext);
                    }
                }
            }
        }
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
}
