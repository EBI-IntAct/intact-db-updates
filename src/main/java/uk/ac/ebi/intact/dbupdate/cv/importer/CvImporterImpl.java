package uk.ac.ebi.intact.dbupdate.cv.importer;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyAccess;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyTermI;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.cv.CvUpdateContext;
import uk.ac.ebi.intact.dbupdate.cv.CvUpdateManager;
import uk.ac.ebi.intact.dbupdate.cv.errors.CvUpdateError;
import uk.ac.ebi.intact.dbupdate.cv.errors.UpdateError;
import uk.ac.ebi.intact.dbupdate.cv.events.CreatedTermEvent;
import uk.ac.ebi.intact.dbupdate.cv.events.UpdateErrorEvent;
import uk.ac.ebi.intact.dbupdate.cv.updater.*;
import uk.ac.ebi.intact.dbupdate.cv.utils.CvUpdateUtils;
import uk.ac.ebi.intact.model.*;

import javax.annotation.PostConstruct;
import javax.persistence.Query;
import java.util.*;

/**
 * This class allows to import a Cv object.
 *
 * This class does not persist the imported cv, it has to be done separately
 *
 * WARNING : the methods importCv is persisting the cvs it has created
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>10/11/11</pre>
 */
public class CvImporterImpl implements CvImporter{

    private Map<String, Class<? extends CvDagObject>> classMap;

    private Map<String, Set<CvDagObject>> missingRootParents;

    private Map<String, CvDagObject> loadedTerms;

    private Set<IntactOntologyTermI> hiddenParents;
    private Set<IntactOntologyTermI> unHiddenChildren;

    private CvUpdater cvUpdater;
    private CvUpdateContext importUpdateContext;

    public CvImporterImpl(){
        classMap = new HashMap<String, Class<? extends CvDagObject>>();
        missingRootParents = new HashMap<String, Set<CvDagObject>>();
        loadedTerms = new HashMap<String, CvDagObject>();
        hiddenParents = new HashSet<IntactOntologyTermI>();
        unHiddenChildren = new HashSet<IntactOntologyTermI>();
        importUpdateContext = new CvUpdateContext(null);
    }

    @PostConstruct
    private void initializeClassMap(){
        classMap.put( "MI:0001", CvInteraction.class );
        classMap.put( "MI:0190", CvInteractionType.class );
        classMap.put( "MI:0002", CvIdentification.class );
        classMap.put( "MI:0003", CvFeatureIdentification.class );
        classMap.put( "MI:0116", CvFeatureType.class );
        classMap.put( "MI:0313", CvInteractorType.class );
        classMap.put( "MI:0346", CvExperimentalPreparation.class );
        classMap.put( "MI:0333", CvFuzzyType.class );
        classMap.put( "MI:0353", CvXrefQualifier.class );
        classMap.put( "MI:0444", CvDatabase.class );
        classMap.put( "MI:0495", CvExperimentalRole.class );
        classMap.put( "MI:0500", CvBiologicalRole.class );
        classMap.put( "MI:0300", CvAliasType.class );
        classMap.put( "MI:0590", CvTopic.class );
        classMap.put( "MI:0640", CvParameterType.class );
        classMap.put( "MI:0647", CvParameterUnit.class );

        classMap.put( "MOD:00000", CvFeatureType.class );
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void importCv(CvUpdateContext updateContext, boolean importChildren) throws InstantiationException, IllegalAccessException, CvUpdateException {
        IntactOntologyAccess ontologyAccess = updateContext.getOntologyAccess();
        IntactOntologyTermI ontologyTerm = updateContext.getOntologyTerm();

        Class<? extends CvDagObject> termClass = findCvClassFor(ontologyTerm, ontologyAccess);

        importCv(updateContext, importChildren, termClass);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void importCv(CvUpdateContext updateContext, boolean importChildren, Class<? extends CvDagObject> termClass) throws InstantiationException, IllegalAccessException, CvUpdateException {
        loadedTerms.clear();
        hiddenParents.clear();
        unHiddenChildren.clear();

        IntactOntologyAccess ontologyAccess = updateContext.getOntologyAccess();
        IntactOntologyTermI ontologyTerm = updateContext.getOntologyTerm();

        Collection<IntactOntologyTermI> rootTerms = ontologyAccess.getRootTerms();

        if (termClass == null){
            CvUpdateManager updateManager = updateContext.getManager();

            CvUpdateError error = updateManager.getErrorFactory().createCvUpdateError(UpdateError.cv_class_not_found, "Impossible to instantiate a CVObject for the term " + ontologyTerm.getTermAccession() + " because no class has been defined for this term or one of its parents.", ontologyTerm.getTermAccession(), null, null);

            UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
            updateManager.fireOnUpdateError(evt);
            throw new IllegalArgumentException();
        }
        else {

            // if we import the children, we collect the deepest children first to go back to the top parent
            if (importChildren){
                // the hidden parents are all the parents of this term
                hiddenParents.addAll(ontologyAccess.getAllParents(ontologyTerm));
                // all the unhidden children of this term
                unHiddenChildren.addAll(ontologyAccess.getAllChildren(ontologyTerm));

                // collect deepest children
                Set<IntactOntologyTermI> deepestNodes = collectDeepestChildren(ontologyAccess, ontologyTerm);

                // the term has children, so we import starting from the children
                if (!deepestNodes.isEmpty()){
                    // for each child, create term and then create parent recursively
                    for (IntactOntologyTermI child : deepestNodes){

                        updateOrCreateChild(updateContext, termClass, child, rootTerms);
                    }
                }
                // we import the term
                else {
                    importTermWithoutChildren(updateContext, termClass, ontologyAccess, ontologyTerm, rootTerms);
                }
            }
            // we just create this term and its parent if they don't exist
            else {
                importTermWithoutChildren(updateContext, termClass, ontologyAccess, ontologyTerm, rootTerms);
            }
        }

        loadedTerms.clear();
        hiddenParents.clear();
        unHiddenChildren.clear();
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    private void importTermWithoutChildren(CvUpdateContext updateContext, Class<? extends CvDagObject> termClass, IntactOntologyAccess ontologyAccess, IntactOntologyTermI ontologyTerm, Collection<IntactOntologyTermI> rootTerms) throws IllegalAccessException, InstantiationException, CvUpdateException {
        // we don't import root terms but we can import obsolete terms if necessary. The obsolete terms are root terms
        if (!rootTerms.contains(ontologyTerm) || ontologyAccess.isObsolete(ontologyTerm)){
            List<CvDagObject> cvObjects = fetchIntactCv(ontologyTerm.getTermAccession(), ontologyAccess.getDatabaseIdentifier(), termClass.getSimpleName());

            CvDagObject cvObject = null;

            // create a new cv and imports its parents to keep a proper hierarchy
            if (cvObjects.isEmpty()){
                cvObject = createCvObjectFrom(ontologyTerm, ontologyAccess, termClass, false, updateContext);

                importParents(cvObject, ontologyTerm, termClass, updateContext, true, rootTerms);
            }
            // the cv already exists, we don't have to import the parents. We just returns the existing term
            else if (cvObjects.size() == 1){
                cvObject = cvObjects.iterator().next();
            }

            // we update the context to set the cv term which has been created
            if (cvObject != null){
                loadedTerms.put(ontologyTerm.getTermAccession(), cvObject);

                updateContext.setCvTerm(cvObject);
            }
            else {
                CvUpdateManager cvManager = updateContext.getManager();

                CvUpdateError error = cvManager.getErrorFactory().createCvUpdateError(UpdateError.duplicated_cv, "Cv object " + ontologyTerm.getTermAccession() + " can match " + cvObjects.size() + " in Intact and the import parent has been done on all these terms.", ontologyTerm.getTermAccession(), null, null);

                UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
                cvManager.fireOnUpdateError(evt);
            }
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    private void updateOrCreateChild(CvUpdateContext updateContext, Class<? extends CvDagObject> termClass, IntactOntologyTermI child, Collection<IntactOntologyTermI> roots) throws IllegalAccessException, InstantiationException, CvUpdateException {
        IntactOntologyTermI ontologyTerm = updateContext.getOntologyTerm();
        IntactOntologyAccess ontologyAccess = updateContext.getOntologyAccess();

        // we only create the child if it has not been loaded yet and it is not a root term
        if (!loadedTerms.containsKey(child.getTermAccession()) && !roots.contains(child)){

            List<CvDagObject> cvObjects = fetchIntactCv(child.getTermAccession(), ontologyAccess.getDatabaseIdentifier(), termClass.getSimpleName());

            CvDagObject cvObject = null;

            // we create a new term and import its parents
            if (cvObjects.isEmpty()){
                cvObject = createCvObjectFrom(child, ontologyAccess, termClass, false, updateContext);

                loadedTerms.put(child.getTermAccession(), cvObject);

                if (child.getTermAccession().equals(updateContext.getIdentifier())){
                    updateContext.setCvTerm(cvObject);
                    // import parents and hide them
                    importParents(cvObject, child, termClass, updateContext, true, roots);
                }
                else {
                    // import parents but do not hide them
                    importParents(cvObject, child, termClass, updateContext, ontologyTerm.getTermAccession(), roots);
                }
            }
            // the child already exists
            else if (cvObjects.size() == 1){
                cvObject = cvObjects.iterator().next();

                loadedTerms.put(child.getTermAccession(), cvObject);

                // the child exists in the database and is the term we wanted to import. No need to import parents
                if (child.getTermAccession().equals(updateContext.getIdentifier())){
                    updateContext.setCvTerm(cvObject);
                }
                // the child already exists but we need to import the parents which are children of the term we want to import
                else {
                    // import parents
                    importParents(cvObject, child, termClass, updateContext, ontologyTerm.getTermAccession(), roots);
                }
            }
            // duplicated terms, we cannot do anything
            else if (cvObjects.size() > 1){
                CvUpdateManager cvManager = updateContext.getManager();

                CvUpdateError error = cvManager.getErrorFactory().createCvUpdateError(UpdateError.duplicated_cv, "Cv object " + child.getTermAccession() + " can match " + cvObjects.size() + " in Intact and the import parent has been done on all these terms.", ontologyTerm.getTermAccession(), null, null);

                UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
                cvManager.fireOnUpdateError(evt);
            }
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    private List<CvDagObject> fetchIntactCv(String id, String db, String cvClass){
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        Query query = factory.getEntityManager().createQuery("select distinct c from "+cvClass+" c left join c.xrefs as x " +
                "where (x.cvDatabase.identifier = :database and x.cvXrefQualifier.identifier = :identity and x.primaryId = :identifier) " +
                "or (" +
                "c.identifier = :identifier and " +
                "(" +
                "(x.ac not in " +
                "(select x2.ac from CvObjectXref x2 where x2.cvDatabase.identifier = :database and x2.cvXrefQualifier.identifier = :identity))" +
                " or c.xrefs is empty" +
                ") " +
                ")");
        query.setParameter("database", db);
        query.setParameter("identity", CvXrefQualifier.IDENTITY_MI_REF);
        query.setParameter("identifier", id);

        return query.getResultList();
    }

    public Class<? extends CvDagObject> findCvClassFor(IntactOntologyTermI ontologyTerm, IntactOntologyAccess ontologyAccess){

        if (classMap.containsKey(ontologyTerm.getTermAccession())){
            return classMap.get(ontologyTerm.getTermAccession());
        }
        else {
            Set<IntactOntologyTermI> parents = ontologyAccess.getDirectParents(ontologyTerm);

            if (!parents.isEmpty()){

                for (IntactOntologyTermI parent : parents){
                    Class<? extends CvDagObject> termClass = findCvClassFor(parent, ontologyAccess);

                    if (termClass != null){
                        return termClass;
                    }
                }
            }
        }

        return null;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public CvDagObject createCvObjectFrom(IntactOntologyTermI ontologyTerm, IntactOntologyAccess ontologyAccess, Class<? extends CvDagObject> termClass, boolean hideParents, CvUpdateContext updateContext) throws IllegalAccessException, InstantiationException, CvUpdateException {

        String accession = ontologyTerm.getTermAccession();

        CvDagObject cvObject = termClass.newInstance();

        importUpdateContext.clear();
        importUpdateContext.setCvTerm(cvObject);
        importUpdateContext.setIdentifier(accession);
        importUpdateContext.setOntologyAccess(ontologyAccess);
        importUpdateContext.setOntologyTerm(ontologyTerm);

        cvUpdater.updateTerm(importUpdateContext);

        // parents should not be obsolete. An obsolete term does not have parents children anymore
        if (hideParents){
            CvUpdateUtils.hideTerm(cvObject, "term not used");
        }

        // fire evt
        CvUpdateManager manager = updateContext.getManager();

        CreatedTermEvent evt = new CreatedTermEvent(this, ontologyTerm.getTermAccession(), cvObject.getShortLabel(), cvObject.getAc(), hideParents, "Created child term");
        manager.fireOnCreatedTerm(evt);

        return cvObject;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    private void importParents(CvDagObject cvChild, IntactOntologyTermI child, Class<? extends CvDagObject> termClass, CvUpdateContext updateContext, boolean hideParents, Collection<IntactOntologyTermI> roots) throws InstantiationException, IllegalAccessException, CvUpdateException {
        IntactOntologyAccess ontologyAccess = updateContext.getOntologyAccess();

        Set<IntactOntologyTermI> parents = ontologyAccess.getDirectParents(child);

        if (!parents.isEmpty()){

            boolean isSubRootTerm = false;

            // create parents or update them
            for (IntactOntologyTermI parent : parents){
                // if the parent is not excluded from the import
                if (!roots.contains(parent)){

                    // the parent was already processed, we just need to update the parent if it does not contain this child
                    // we can stop processing the parents as this term was already processed
                    if (loadedTerms.containsKey(parent.getTermAccession())){
                        CvDagObject importedParent = loadedTerms.get(parent.getTermAccession());

                        // add children (only if it didn't exist)
                        importedParent.addChild(cvChild);

                        // update changes
                        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(importedParent);
                    }
                    // we need to retrieve or create the parent
                    else {
                        List<CvDagObject> cvObjects = fetchIntactCv(parent.getTermAccession(), ontologyAccess.getDatabaseIdentifier(), termClass.getSimpleName());

                        // duplicated terms, we cannot do anything
                        if (cvObjects.size() > 1){
                            CvUpdateManager cvManager = updateContext.getManager();

                            CvUpdateError error = cvManager.getErrorFactory().createCvUpdateError(UpdateError.duplicated_cv, "Cv object " + parent.getTermAccession() + " can match " + cvObjects.size() + " in Intact and the import parent has been done on all these terms.", parent.getTermAccession(), null, null);

                            UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
                            cvManager.fireOnUpdateError(evt);

                            // update any changes done to the children
                            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(cvChild);
                        }
                        // we have one existing parent so we don't need to import the parents of the parent.
                        // we just plug the child to the existing parent
                        else if (cvObjects.size() == 1){
                            CvDagObject dagObject = cvObjects.iterator().next();

                            loadedTerms.put(parent.getTermAccession(), dagObject);

                            // add children (only if it didn't exist)
                            dagObject.addChild(cvChild);

                            // update changes
                            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(dagObject);
                        }
                        // create the parent and import its parents
                        else {
                            CvDagObject dagObject = createCvObjectFrom(parent, ontologyAccess, termClass, hideParents, updateContext);

                            loadedTerms.put(parent.getTermAccession(), dagObject);

                            // add children (only if it didn't exist)
                            dagObject.addChild(cvChild);

                            // update/ create parents
                            importParents(dagObject, parent, termClass, updateContext, hideParents, roots);
                        }
                    }
                }
                // the parent is root term. We do save the child now and all the children of the child will be saved ass well
                else {
                    isSubRootTerm = true;
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(cvChild);
                }
            }

            if (isSubRootTerm && ontologyAccess.getParentFromOtherOntology() != null){
                if (missingRootParents.containsKey(ontologyAccess.getParentFromOtherOntology())){
                    missingRootParents.get(ontologyAccess.getParentFromOtherOntology()).add(cvChild);
                }
                else {
                    Set<CvDagObject> children = new HashSet<CvDagObject>();
                    children.add(cvChild);
                    missingRootParents.put(ontologyAccess.getParentFromOtherOntology(), children);
                }
            }
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    private void importParents(CvDagObject cvChild, IntactOntologyTermI child, Class<? extends CvDagObject> termClass, CvUpdateContext updateContext, String currentTerm, Collection<IntactOntologyTermI> roots) throws InstantiationException, IllegalAccessException, CvUpdateException {
        IntactOntologyAccess ontologyAccess = updateContext.getOntologyAccess();

        Set<IntactOntologyTermI> parents = ontologyAccess.getDirectParents(child);

        if (!parents.isEmpty()){

            boolean isSubRootTerm = false;

            for (IntactOntologyTermI parent : parents){

                // we don't import root terms
                if (!roots.contains(parent)){

                    // the term has already been loaded so we just update it and don't import the parents
                    if (this.loadedTerms.containsKey(parent.getTermAccession())){
                        CvDagObject importedParent = loadedTerms.get(parent.getTermAccession());

                        // update children
                        importedParent.addChild(cvChild);

                        // update changes
                        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(importedParent);
                    }
                    // the term has not been loaded yet
                    else {
                        List<CvDagObject> cvObjects = fetchIntactCv(parent.getTermAccession(), ontologyAccess.getDatabaseIdentifier(), termClass.getSimpleName());

                        // we have duplicated terms, we don't do anything
                        if (cvObjects.size() > 1){
                            CvUpdateManager cvManager = updateContext.getManager();

                            CvUpdateError error = cvManager.getErrorFactory().createCvUpdateError(UpdateError.duplicated_cv, "Cv object " + parent.getTermAccession() + " can match " + cvObjects.size() + " in Intact and the import parent has been done on all these terms.", parent.getTermAccession(), null, null);

                            UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
                            cvManager.fireOnUpdateError(evt);

                            // update any changes done to the children
                            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(cvChild);
                        }
                        // one existing term.
                        else if (cvObjects.size() == 1){
                            CvDagObject dagObject = cvObjects.iterator().next();

                            loadedTerms.put(parent.getTermAccession(), dagObject);

                            // add children (only if it didn't exist)
                            dagObject.addChild(cvChild);

                            // we collected the proper term, no need to import the parents
                            if (parent.getTermAccession().equals(currentTerm)){

                                updateContext.setCvTerm(dagObject);
                                // update changes
                                IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(dagObject);
                            }
                            // we want to import the parents which are the children of the term we want to import
                            else if (unHiddenChildren.contains(parent)) {
                                importParents(dagObject, parent, termClass, updateContext, currentTerm, roots);
                            }
                            // we can stop to import parents which are not children of the term we want to import and
                            // only plug the child to the existing parent
                            else {
                                // update changes
                                IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(dagObject);
                            }
                        }
                        // term does not exist, we need to import parents
                        else {

                            // the parent is the term we want to import, we need to import the parents and hide them
                            if (parent.getTermAccession().equals(currentTerm)){

                                CvDagObject dagObject = createCvObjectFrom(parent, ontologyAccess, termClass, false, updateContext);

                                loadedTerms.put(parent.getTermAccession(), dagObject);

                                // add children (only if it didn't exist)
                                dagObject.addChild(cvChild);

                                updateContext.setCvTerm(dagObject);

                                // update/ create parents and hide them
                                importParents(dagObject, parent, termClass, updateContext, true, roots);
                            }
                            // the parent is still a children of the term we want to import, so we keep importing parents
                            else {
                                // we hide the term if : it is a parent of the term we want to import or it is a parent of a child of the term we want to import
                                // which is not a children of the term we want to import
                                boolean isHidden = hiddenParents.contains(parent) || !unHiddenChildren.contains(parent);

                                CvDagObject dagObject = createCvObjectFrom(parent, ontologyAccess, termClass, isHidden, updateContext);

                                loadedTerms.put(parent.getTermAccession(), dagObject);

                                // add children (only if it didn't exist)
                                dagObject.addChild(cvChild);

                                // update/ create parents
                                importParents(dagObject, parent, termClass, updateContext, currentTerm, roots);
                            }
                        }
                    }
                }
                // the parent is root term. We do save the child now and all the children of the child will be saved ass well
                else {
                    isSubRootTerm = true;
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(cvChild);
                }
            }

            if (isSubRootTerm && ontologyAccess.getParentFromOtherOntology() != null){
                if (missingRootParents.containsKey(ontologyAccess.getParentFromOtherOntology())){
                    missingRootParents.get(ontologyAccess.getParentFromOtherOntology()).add(cvChild);
                }
                else {
                    Set<CvDagObject> children = new HashSet<CvDagObject>();
                    children.add(cvChild);
                    missingRootParents.put(ontologyAccess.getParentFromOtherOntology(), children);
                }
            }
        }
    }

    public Set<IntactOntologyTermI> collectDeepestChildren(IntactOntologyAccess ontologyAccess, IntactOntologyTermI parent){

        Set<IntactOntologyTermI> directChildren = ontologyAccess.getDirectChildren(parent);

        if (!directChildren.isEmpty()){
            Set<IntactOntologyTermI> directChildrenCopy = new HashSet<IntactOntologyTermI>(directChildren);

            for (IntactOntologyTermI child : directChildrenCopy){
                directChildren.addAll(collectDeepestChildren(ontologyAccess, child));
            }
        }
        else {
            directChildren = new HashSet<IntactOntologyTermI>(1);
            directChildren.add(parent);
        }

        return directChildren;
    }

    public Map<String, Class<? extends CvDagObject>> getClassMap() {
        return classMap;
    }

    public Map<String, Set<CvDagObject>> getMissingRootParents() {
        return missingRootParents;
    }

    public CvUpdater getCvUpdater() {
        if (cvUpdater == null){
            cvUpdater = new CvInitializer();
        }
        return cvUpdater;
    }

    public void setCvUpdater(CvUpdater cvUpdater) {
        if (cvUpdater == null){
            cvUpdater = new CvUpdaterImpl();
        }
        this.cvUpdater = cvUpdater;
    }

    public void clear(){
        this.loadedTerms.clear();
        this.missingRootParents.clear();
        this.hiddenParents.clear();
        this.unHiddenChildren.clear();
        cvUpdater.clear();
        importUpdateContext.clear();
    }
}
