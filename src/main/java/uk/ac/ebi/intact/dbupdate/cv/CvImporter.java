package uk.ac.ebi.intact.dbupdate.cv;

import uk.ac.ebi.intact.bridges.ontology_manager.TermAnnotation;
import uk.ac.ebi.intact.bridges.ontology_manager.TermDbXref;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyAccess;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyTermI;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.CvObjectDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.model.*;
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

public class CvImporter {

    private Map<String, Class<? extends CvDagObject>> classMap;

    private Set<String> processedTerms;

    private Set<String> rootTermsToExclude;

    private CvDagObject importedTerm;

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

    public void importCv(IntactOntologyTermI ontologyTerm, IntactOntologyAccess ontologyAccess, boolean importChildren, DaoFactory factory) throws InstantiationException, IllegalAccessException {

        Class<? extends CvDagObject> termClass = findCvClassFor(ontologyTerm, ontologyAccess);

        importCv(ontologyTerm, ontologyAccess, importChildren, factory, termClass);
    }

    public void importCv(IntactOntologyTermI ontologyTerm, IntactOntologyAccess ontologyAccess, boolean importChildren, DaoFactory factory, Class<? extends CvDagObject> termClass) throws InstantiationException, IllegalAccessException {
        processedTerms.clear();
        importedTerm = null;

        CvObjectDao<CvDagObject> cvDao = factory.getCvObjectDao(CvDagObject.class);

        if (termClass == null){
            throw new IllegalArgumentException("Impossible to instantiate a CVObject for the term " + ontologyTerm.getTermAccession() + " because no class has been defined for this term or one of its parents.");
        }

        if (importChildren){
            Set<IntactOntologyTermI> deepestNodes = collectDeepestChildren(ontologyAccess, ontologyTerm);

            for (IntactOntologyTermI child : deepestNodes){

                CvDagObject cvObject = cvDao.getByIdentifier(child.getTermAccession());
                if (cvObject == null){
                    cvObject = createCvObjectFrom(child, ontologyAccess, termClass, factory, false);
                }

                processedTerms.add(child.getTermAccession());

                // update/ create parents
                importParents(cvObject, child, termClass, ontologyAccess, factory, ontologyTerm.getTermAccession());
            }
        }
        else {
            CvDagObject cvObject = cvDao.getByIdentifier(ontologyTerm.getTermAccession());
            if (cvObject == null){
                cvObject = createCvObjectFrom(ontologyTerm, ontologyAccess, termClass, factory, false);
            }

            processedTerms.add(ontologyTerm.getTermAccession());

            // update/ create parents
            importParents(cvObject, ontologyTerm, termClass, ontologyAccess, factory, true);

            importedTerm = cvObject;
        }
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

    private CvDagObject createCvObjectFrom(IntactOntologyTermI ontologyTerm, IntactOntologyAccess ontologyAccess, Class<? extends CvDagObject> termClass, DaoFactory factory, boolean hideParents) throws IllegalAccessException, InstantiationException {
        IntactContext context = IntactContext.getCurrentInstance();

        String accession = ontologyTerm.getTermAccession();

        CvDagObject cvObject = termClass.newInstance();

        // set shortLabel
        cvObject.setShortLabel(ontologyTerm.getShortLabel());

        // set fullName
        cvObject.setFullName(ontologyTerm.getFullName());

        // set identifier
        cvObject.setIdentifier(accession);

        CvXrefQualifier identity = factory.getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.IDENTITY_MI_REF);
        CvDatabase db = factory.getCvObjectDao(CvDatabase.class).getByIdentifier(ontologyAccess.getDatabaseIdentifier());
        CvObjectXref cvXref = XrefUtils.createIdentityXref(null, accession, identity, db);
        factory.getXrefDao(CvObjectXref.class).persist(cvXref);
        cvObject.addXref(cvXref);

        Map<String, CvDatabase> processedDatabases = new HashMap<String, CvDatabase>();
        processedDatabases.put(ontologyAccess.getDatabaseIdentifier(), db);

        Map<String, CvXrefQualifier> processedXrefQualifiers = new HashMap<String, CvXrefQualifier>();
        processedXrefQualifiers.put(CvXrefQualifier.IDENTITY_MI_REF, identity);

        // create xrefs
        for (TermDbXref termRef : ontologyTerm.getDbXrefs()){
            CvDatabase database;
            if (processedDatabases.containsKey(termRef.getDatabaseId())){
                database = processedDatabases.get(termRef.getDatabaseId());
            }
            else {
                database = factory.getCvObjectDao(CvDatabase.class).getByIdentifier(termRef.getDatabaseId());
                processedDatabases.put(termRef.getDatabaseId(), database);
            }

            CvXrefQualifier qualifier;
            if (processedXrefQualifiers.containsKey(termRef.getQualifierId())){
                qualifier = processedXrefQualifiers.get(termRef.getQualifierId());
            }
            else {
                qualifier = factory.getCvObjectDao(CvXrefQualifier.class).getByIdentifier(termRef.getQualifierId());
                processedXrefQualifiers.put(termRef.getQualifierId(), qualifier);
            }

            CvObjectXref ref = XrefUtils.createIdentityXref(null, termRef.getAccession(), qualifier, database);
            factory.getXrefDao(CvObjectXref.class).persist(ref);
            cvObject.addXref(ref);
        }

        // create aliases
        CvAliasType aliasType = factory.getCvObjectDao(CvAliasType.class).getByShortLabel(CvUpdater.ALIAS_TYPE);

        for (String alias : ontologyTerm.getAliases()){

            CvObjectAlias aliasObject = new CvObjectAlias(context.getInstitution(), null, aliasType, alias);
            factory.getAliasDao(CvObjectAlias.class).persist(aliasObject);
            cvObject.addAlias(aliasObject);
        }

        // create annotations
        for (TermAnnotation annotation : ontologyTerm.getAnnotations()){

            CvTopic topic = factory.getCvObjectDao(CvTopic.class).getByIdentifier(annotation.getTopicId());

            Annotation annotationObject = new Annotation(topic, annotation.getDescription());
            factory.getAnnotationDao().persist(annotationObject);
            cvObject.addAnnotation(annotationObject);
        }

        // create definition
        if (ontologyTerm.getDefinition() != null){
            CvTopic topic = factory.getCvObjectDao(CvTopic.class).getByShortLabel(CvTopic.DEFINITION);

            Annotation annotationObject = new Annotation(topic, ontologyTerm.getDefinition());
            factory.getAnnotationDao().persist(annotationObject);
            cvObject.addAnnotation(annotationObject);
        }

        // create url
        if (ontologyTerm.getURL() != null){
            CvTopic topic = factory.getCvObjectDao(CvTopic.class).getByIdentifier(CvTopic.URL_MI_REF);

            Annotation annotationObject = new Annotation(topic, ontologyTerm.getURL());
            factory.getAnnotationDao().persist(annotationObject);
            cvObject.addAnnotation(annotationObject);
        }

        // create comments
        if (!ontologyTerm.getComments().isEmpty()){
            CvTopic topic = factory.getCvObjectDao(CvTopic.class).getByIdentifier(CvTopic.COMMENT_MI_REF);

            for (String comment : ontologyTerm.getComments()){
                Annotation annotationObject = new Annotation(topic, comment);
                factory.getAnnotationDao().persist(annotationObject);
                cvObject.addAnnotation(annotationObject);
            }
        }

        boolean hidden = false;

        // create obsolete
        if (ontologyAccess.isObsolete(ontologyTerm)){
            CvTopic topic = factory.getCvObjectDao(CvTopic.class).getByIdentifier(CvTopic.OBSOLETE_MI_REF);

            Annotation annotationObject = new Annotation(topic, ontologyTerm.getObsoleteMessage());
            factory.getAnnotationDao().persist(annotationObject);
            cvObject.addAnnotation(annotationObject);

            hideTerm(cvObject, "obsolete term", factory);
            hidden = true;
        }

        if (!hidden && hideParents){
            hideTerm(cvObject, "unused term", factory);
        }

        factory.getCvObjectDao(CvDagObject.class).persist(cvObject);

        return cvObject;
    }

    private void importParents(CvDagObject cvChild, IntactOntologyTermI child, Class<? extends CvDagObject> termClass, IntactOntologyAccess ontologyAccess, DaoFactory factory, boolean hideParents) throws InstantiationException, IllegalAccessException {

        Set<IntactOntologyTermI> parents = ontologyAccess.getDirectParents(child);

        CvObjectDao<CvDagObject> cvDao = factory.getCvObjectDao(CvDagObject.class);

        if (!parents.isEmpty()){

            for (IntactOntologyTermI parent : parents){
                if (!rootTermsToExclude.contains(parent.getTermAccession()) && !processedTerms.contains(parent.getTermAccession())){
                    CvDagObject cvObject = cvDao.getByIdentifier(parent.getTermAccession());
                    if (cvObject == null){
                        cvObject = createCvObjectFrom(parent, ontologyAccess, termClass, factory, hideParents);
                    }
                    processedTerms.add(parent.getTermAccession());

                    // update children
                    cvObject.addChild(cvChild);
                    cvDao.update(cvChild);

                    // update/ create parents
                    importParents(cvObject, parent, termClass, ontologyAccess, factory, hideParents);
                }
            }
        }
    }

    private void importParents(CvDagObject cvChild, IntactOntologyTermI child, Class<? extends CvDagObject> termClass, IntactOntologyAccess ontologyAccess, DaoFactory factory, String currentTerm) throws InstantiationException, IllegalAccessException {

        Set<IntactOntologyTermI> parents = ontologyAccess.getDirectParents(child);

        CvObjectDao<CvDagObject> cvDao = factory.getCvObjectDao(CvDagObject.class);

        if (!parents.isEmpty()){

            for (IntactOntologyTermI parent : parents){
                if (!rootTermsToExclude.contains(parent.getTermAccession()) && !processedTerms.contains(parent.getTermAccession())){

                    if (parent.getTermAccession().equals(currentTerm)){

                        CvDagObject cvObject = cvDao.getByIdentifier(parent.getTermAccession());
                        if (cvObject == null){
                            cvObject = createCvObjectFrom(parent, ontologyAccess, termClass, factory, false);
                        }
                        processedTerms.add(parent.getTermAccession());

                        // update children
                        cvObject.addChild(cvChild);
                        cvDao.update(cvChild);

                        // update/ create parents
                        importParents(cvObject, parent, termClass, ontologyAccess, factory, true);

                        importedTerm = cvObject;
                    }
                    else {
                        CvDagObject cvObject = cvDao.getByIdentifier(parent.getTermAccession());
                        if (cvObject == null){
                            cvObject = createCvObjectFrom(parent, ontologyAccess, termClass, factory, false);
                        }
                        processedTerms.add(parent.getTermAccession());

                        // update children
                        cvObject.addChild(cvChild);
                        cvDao.update(cvChild);

                        // update/ create parents
                        importParents(cvObject, parent, termClass, ontologyAccess, factory, currentTerm);
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

    public CvDagObject getImportedTerm() {
        return importedTerm;
    }
}
