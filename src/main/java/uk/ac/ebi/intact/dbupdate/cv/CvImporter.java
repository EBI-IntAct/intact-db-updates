package uk.ac.ebi.intact.dbupdate.cv;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.bridges.ontology_manager.TermAnnotation;
import uk.ac.ebi.intact.bridges.ontology_manager.TermDbXref;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyAccess;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyTermI;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.CvObjectDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.cv.events.CreatedTermEvent;
import uk.ac.ebi.intact.dbupdate.cv.utils.CvUpdateUtils;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.XrefUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class allows to import a Cv object to a database
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>10/11/11</pre>
 */
@org.springframework.stereotype.Component
public class CvImporter {

    private Map<String, Class<? extends CvDagObject>> classMap;

    private Set<String> processedTerms;

    private Set<String> rootTermsToExclude;

    public CvImporter(){
        classMap = new HashMap<String, Class<? extends CvDagObject>>();
        initializeClassMap();
        processedTerms = new HashSet<String>();
        rootTermsToExclude = new HashSet<String>();

        initializeRootTermsToExclude();
    }

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

    private void initializeRootTermsToExclude(){
        rootTermsToExclude.add("MI:0000");
        rootTermsToExclude.add("MOD:00000");
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void importCv(CvUpdateContext updateContext, boolean importChildren) throws InstantiationException, IllegalAccessException {
        IntactOntologyAccess ontologyAccess = updateContext.getOntologyAccess();
        IntactOntologyTermI ontologyTerm = updateContext.getOntologyTerm();

        Class<? extends CvDagObject> termClass = findCvClassFor(ontologyTerm, ontologyAccess);

        importCv(updateContext, importChildren, termClass);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void importCv(CvUpdateContext updateContext, boolean importChildren, Class<? extends CvDagObject> termClass) throws InstantiationException, IllegalAccessException {
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();
        IntactOntologyAccess ontologyAccess = updateContext.getOntologyAccess();
        IntactOntologyTermI ontologyTerm = updateContext.getOntologyTerm();
        CvObjectDao<CvDagObject> cvDao = factory.getCvObjectDao(CvDagObject.class);

        if (termClass == null){
            throw new IllegalArgumentException("Impossible to instantiate a CVObject for the term " + ontologyTerm.getTermAccession() + " because no class has been defined for this term or one of its parents.");
        }

        // if we import the children, we collect the deepest children first to go back to the top parent
        if (importChildren){
            // collect deepest children
            Set<IntactOntologyTermI> deepestNodes = collectDeepestChildren(ontologyAccess, ontologyTerm);

            // for each child, create term and then create parent recursively
            for (IntactOntologyTermI child : deepestNodes){

                updateOrCreateChild(updateContext, termClass, child);
            }
        }
        // we just create this term and its parent if they don't exist
        else {
            CvDagObject cvObject = cvDao.getByIdentifier(ontologyTerm.getTermAccession());
            if (cvObject == null){
                cvObject = createCvObjectFrom(ontologyTerm, ontologyAccess, termClass, false);
            }

            processedTerms.add(ontologyTerm.getTermAccession());

            // update/ create parents
            importParents(cvObject, ontologyTerm, termClass, updateContext, true);

            // we update the context to set the cv term which has been created
            updateContext.setCvTerm(cvObject);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void updateOrCreateChild(CvUpdateContext updateContext, Class<? extends CvDagObject> termClass, IntactOntologyTermI child) throws IllegalAccessException, InstantiationException {
        IntactOntologyTermI ontologyTerm = updateContext.getOntologyTerm();
        CvObjectDao<CvDagObject> cvDao = IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvDagObject.class);

        if (!processedTerms.contains(child.getTermAccession())){
            processedTerms.add(child.getTermAccession());

            CvDagObject cvObject = cvDao.getByIdentifier(child.getTermAccession());
            if (cvObject == null){
                cvObject = createAndPersistNewCv(updateContext, termClass, child, false);
            }

            if (child.getTermAccession().equals(updateContext.getIdentifier())){
                updateContext.setCvTerm(cvObject);
                // import parents and hide them
                importParents(cvObject, child, termClass, updateContext, true);
            }
            else {
                // import parents without hidding them
                importParents(cvObject, child, termClass, updateContext, ontologyTerm.getTermAccession());
            }
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    private CvDagObject createAndPersistNewCv(CvUpdateContext updateContext, Class<? extends CvDagObject> termClass, IntactOntologyTermI child, boolean hide) throws IllegalAccessException, InstantiationException {
        IntactOntologyAccess ontologyAccess = updateContext.getOntologyAccess();

        CvDagObject cvObject;
        cvObject = createCvObjectFrom(child, ontologyAccess, termClass, false);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(cvObject);

        // fire evt
        CvUpdateManager manager = updateContext.getManager();

        CreatedTermEvent evt = new CreatedTermEvent(this, child.getTermAccession(), cvObject.getShortLabel(), cvObject.getAc(), hide, "Created child term");
        manager.fireOnCreatedTerm(evt);
        return cvObject;
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

    public CvDagObject createCvObjectFrom(IntactOntologyTermI ontologyTerm, IntactOntologyAccess ontologyAccess, Class<? extends CvDagObject> termClass, boolean hideParents) throws IllegalAccessException, InstantiationException {

        String accession = ontologyTerm.getTermAccession();

        CvDagObject cvObject = termClass.newInstance();

        // set shortLabel
        cvObject.setShortLabel(ontologyTerm.getShortLabel());

        // set fullName
        cvObject.setFullName(ontologyTerm.getFullName());

        // set identifier
        cvObject.setIdentifier(accession);

        CvObjectXref identity = CvUpdateUtils.createIdentityXref(cvObject, ontologyAccess.getDatabaseIdentifier(), CvXrefQualifier.IDENTITY_MI_REF);

        Map<String, CvDatabase> processedDatabases = new HashMap<String, CvDatabase>();

        Map<String, CvXrefQualifier> processedXrefQualifiers = new HashMap<String, CvXrefQualifier>();

        // create xrefs
        for (TermDbXref termRef : ontologyTerm.getDbXrefs()){
            CvDatabase database;
            if (processedDatabases.containsKey(termRef.getDatabaseId())){
                database = processedDatabases.get(termRef.getDatabaseId());
            }
            else {
                database = CvObjectUtils.createCvObject(cvObject.getOwner(), CvDatabase.class, termRef.getDatabaseId(), termRef.getDatabase());
                processedDatabases.put(termRef.getDatabaseId(), database);
            }

            CvXrefQualifier qualifier;
            if (processedXrefQualifiers.containsKey(termRef.getQualifierId())){
                qualifier = processedXrefQualifiers.get(termRef.getQualifierId());
            }
            else {
                qualifier = CvObjectUtils.createCvObject(cvObject.getOwner(), CvXrefQualifier.class, termRef.getQualifierId(), termRef.getQualifier());
                processedXrefQualifiers.put(termRef.getQualifierId(), qualifier);
            }

            CvObjectXref ref = XrefUtils.createIdentityXref(null, termRef.getAccession(), qualifier, database);
            cvObject.addXref(ref);
        }

        // create aliases
        CvAliasType aliasType = CvObjectUtils.createCvObject(cvObject.getOwner(), CvAliasType.class, null, CvUpdater.ALIAS_TYPE);

        for (String alias : ontologyTerm.getAliases()){

            CvObjectAlias aliasObject = new CvObjectAlias(cvObject.getOwner(), null, aliasType, alias);
            cvObject.addAlias(aliasObject);
        }

        // create annotations
        for (TermAnnotation annotation : ontologyTerm.getAnnotations()){

            CvTopic topic = CvObjectUtils.createCvObject(cvObject.getOwner(), CvTopic.class, annotation.getTopicId(), annotation.getTopic());

            Annotation annotationObject = new Annotation(topic, annotation.getDescription());
            cvObject.addAnnotation(annotationObject);
        }

        // create definition
        if (ontologyTerm.getDefinition() != null){
            CvTopic topic = CvObjectUtils.createCvObject(cvObject.getOwner(), CvTopic.class, null, CvTopic.DEFINITION);

            Annotation annotationObject = new Annotation(topic, ontologyTerm.getDefinition());
            cvObject.addAnnotation(annotationObject);
        }

        // create url
        if (ontologyTerm.getURL() != null){
            CvTopic topic = CvObjectUtils.createCvObject(cvObject.getOwner(), CvTopic.class, CvTopic.URL_MI_REF, CvTopic.URL);

            Annotation annotationObject = new Annotation(topic, ontologyTerm.getURL());
            cvObject.addAnnotation(annotationObject);
        }

        // create comments
        if (!ontologyTerm.getComments().isEmpty()){
            CvTopic topic = CvObjectUtils.createCvObject(cvObject.getOwner(), CvTopic.class, CvTopic.COMMENT_MI_REF, CvTopic.COMMENT);

            for (String comment : ontologyTerm.getComments()){
                Annotation annotationObject = new Annotation(topic, comment);
                cvObject.addAnnotation(annotationObject);
            }
        }

        // parents should not be obsolete. An obsolete term does not have parents children anymore

        if (hideParents){
            CvUpdateUtils.hideTerm(cvObject, "term not used");
        }

        return cvObject;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    private void importParents(CvDagObject cvChild, IntactOntologyTermI child, Class<? extends CvDagObject> termClass, CvUpdateContext updateContext, boolean hideParents) throws InstantiationException, IllegalAccessException {
        IntactOntologyAccess ontologyAccess = updateContext.getOntologyAccess();

        Set<IntactOntologyTermI> parents = ontologyAccess.getDirectParents(child);

        CvObjectDao<CvDagObject> cvDao = IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvDagObject.class);

        if (!parents.isEmpty()){

            // create parents or update them
            for (IntactOntologyTermI parent : parents){
                // if the parent is not excluded from the import
                if (!rootTermsToExclude.contains(parent.getTermAccession()) && !processedTerms.contains(parent.getTermAccession())){
                    boolean needToImportParents = true;

                    // the parent was not already processed so we need to process the parents
                    if (!processedTerms.contains(parent.getTermAccession())){
                        processedTerms.add(parent.getTermAccession());
                    }
                    // the parent was already processed, we just need to update the parent if it does not contain this child
                    // we can stop processing the parents as this term was already processed
                    else {
                        needToImportParents = false;
                    }

                    CvDagObject cvObject = cvDao.getByIdentifier(parent.getTermAccession());
                    if (cvObject == null){
                        cvObject = createAndPersistNewCv(updateContext, termClass, parent, hideParents);
                    }

                    // add children (only if it didn't exist)
                    cvObject.addChild(cvChild);
                    cvDao.update(cvChild);

                    // update/ create parents
                    if (needToImportParents){
                        importParents(cvObject, parent, termClass, updateContext, hideParents);

                    }
                }
            }
        }
    }

    private void importParents(CvDagObject cvChild, IntactOntologyTermI child, Class<? extends CvDagObject> termClass, CvUpdateContext updateContext, String currentTerm) throws InstantiationException, IllegalAccessException {
        IntactOntologyAccess ontologyAccess = updateContext.getOntologyAccess();

        Set<IntactOntologyTermI> parents = ontologyAccess.getDirectParents(child);

        CvObjectDao<CvDagObject> cvDao = IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvDagObject.class);

        if (!parents.isEmpty()){

            for (IntactOntologyTermI parent : parents){
                if (!rootTermsToExclude.contains(parent.getTermAccession()) && !processedTerms.contains(parent.getTermAccession())){
                    boolean needToImportParents = true;

                    if (!this.processedTerms.contains(parent.getTermAccession())){
                        processedTerms.add(parent.getTermAccession());
                    }
                    else {
                        needToImportParents = false;
                    }

                    CvDagObject cvObject = cvDao.getByIdentifier(parent.getTermAccession());
                    if (cvObject == null){
                        cvObject = createAndPersistNewCv(updateContext, termClass, parent, false);
                    }

                    // update children
                    cvObject.addChild(cvChild);
                    cvDao.update(cvChild);

                    if (parent.getTermAccession().equals(currentTerm)){

                        updateContext.setCvTerm(cvObject);

                        // update/ create parents and hide them
                        if (needToImportParents){
                            importParents(cvObject, parent, termClass, updateContext, true);
                        }
                    }
                    else if (needToImportParents) {
                        // update/ create parents
                        importParents(cvObject, parent, termClass, updateContext, currentTerm);
                    }
                }
            }
        }
    }

    public Set<IntactOntologyTermI> collectDeepestChildren(IntactOntologyAccess ontologyAccess, IntactOntologyTermI parent){

        Set<IntactOntologyTermI> directChildren = ontologyAccess.getDirectChildren(parent);

        if (!directChildren.isEmpty()){

            for (IntactOntologyTermI child : directChildren){
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

    public Set<String> getProcessedTerms() {
        return processedTerms;
    }

    public void hideTerm(CvDagObject c, String message, DaoFactory factory){
        CvTopic topicFromDb = factory.getCvObjectDao(CvTopic.class).getByShortLabel(CvTopic.HIDDEN);

        Annotation newAnnotation = new Annotation(topicFromDb, message);
        c.addAnnotation(newAnnotation);

        factory.getAnnotationDao().persist(newAnnotation);
    }

    public void clear(){
        this.processedTerms.clear();
    }
}
