package uk.ac.ebi.intact.dbupdate.cv;

import org.apache.commons.collections.CollectionUtils;
import uk.ac.ebi.intact.bridges.ontology_manager.TermAnnotation;
import uk.ac.ebi.intact.bridges.ontology_manager.TermDbXref;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyAccess;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyTermI;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.*;
import uk.ac.ebi.intact.dbupdate.cv.errors.CvUpdateError;
import uk.ac.ebi.intact.dbupdate.cv.errors.UpdateError;
import uk.ac.ebi.intact.dbupdate.cv.events.UpdateErrorEvent;
import uk.ac.ebi.intact.dbupdate.cv.events.UpdatedEvent;
import uk.ac.ebi.intact.dbupdate.cv.utils.*;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.XrefUtils;

import java.util.*;
import java.util.regex.Matcher;

/**
 * this class is for updating a cv
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>10/11/11</pre>
 */

public class CvUpdater {

    private Map<String, Set<CvDagObject>> missingParents;
    private Set<String> processedTerms;

    public final static String ALIAS_TYPE="database alias";

    private TreeSet<CvObjectXref> sortedCvXrefs;
    private TreeSet<TermDbXref> sortedOntologyXrefs;
    private TreeSet<CvObjectAlias> sortedCvAliases;
    private TreeSet<String> sortedOntologyAliases;
    private TreeSet<TermAnnotation> sortedOntologyAnnotations;
    private TreeSet<Annotation> sortedCvAnnotations;

    public CvUpdater() {
        missingParents = new HashMap<String, Set<CvDagObject>>();
        processedTerms = new HashSet<String>();
        sortedCvXrefs = new TreeSet<CvObjectXref>(new CvXrefComparator());
        sortedOntologyXrefs = new TreeSet<TermDbXref>(new OntologyXrefComparator());
        sortedCvAliases = new TreeSet<CvObjectAlias>(new CvAliasComparator());
        sortedOntologyAliases = new TreeSet<String>();
        sortedCvAnnotations = new TreeSet<Annotation>(new CvAnnotationComparator());
        sortedOntologyAnnotations = new TreeSet<TermAnnotation>(new OntologyAnnotationComparator());
    }

    public void updateTerm(CvUpdateContext updateContext){
        // use dao factory
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        IntactOntologyAccess ontologyAccess = updateContext.getOntologyAccess();
        IntactOntologyTermI ontologyTerm = updateContext.getOntologyTerm();
        CvDagObject term = updateContext.getCvTerm();
        String identifier = updateContext.getIdentifier();
        boolean isObsolete = updateContext.isTermObsolete();

        // extract database of ontology
        String database = updateContext.getOntologyAccess().getDatabaseIdentifier();

        // add term to the list of updated terms
        processedTerms.add(identifier);

        boolean hasUpdatedIdentifier = false;
        boolean hasUpdatedShortLabel = false;
        boolean hasUpdatedFullName = false;

        // update shortLabel
        if (!ontologyTerm.getShortLabel().equalsIgnoreCase(term.getShortLabel())){
            hasUpdatedShortLabel = true;
            term.setShortLabel(ontologyTerm.getShortLabel());
        }

        // update fullName
        if (!ontologyTerm.getFullName().equalsIgnoreCase(term.getFullName())){
            hasUpdatedFullName = true;
            term.setFullName(ontologyTerm.getFullName());
        }

        // update identifier if necessary
        if (!identifier.equalsIgnoreCase(term.getIdentifier())){
            hasUpdatedIdentifier = true;
            // update identifier
            term.setIdentifier(identifier);
        }

        // create update evt
        String updatedShortLabel = hasUpdatedShortLabel ? ontologyTerm.getShortLabel() : null;
        String updatedFullName = hasUpdatedFullName ? ontologyTerm.getFullName() : null;
        UpdatedEvent updateEvt = new UpdatedEvent(this, identifier, term.getAc(), updatedShortLabel, updatedFullName, hasUpdatedIdentifier);

        // update identity xref if necessary
        CvObjectXref identityXref = updateContext.getIdentityXref();
        if (identityXref == null) {
            identityXref = CvUpdateUtils.createIdentityXref(term, database, identifier);
            updateEvt.getCreatedXrefs().add(identityXref);
            updateContext.setIdentityXref(identityXref);
        }
        else if (!identifier.equalsIgnoreCase(identityXref.getPrimaryId()) || !database.equalsIgnoreCase(identityXref.getCvDatabase().getIdentifier())
                || !CvXrefQualifier.IDENTITY_MI_REF.equalsIgnoreCase(identityXref.getCvXrefQualifier().getIdentifier())){
            identityXref.setPrimaryId(identifier);

            if (identityXref.getCvXrefQualifier()  == null || !CvXrefQualifier.IDENTITY_MI_REF.equalsIgnoreCase(identityXref.getCvXrefQualifier().getIdentifier())){
                CvXrefQualifier identity = factory.getCvObjectDao(CvXrefQualifier.class).getByIdentifier(CvXrefQualifier.IDENTITY_MI_REF);

                if (identity == null){
                    identity = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF, CvXrefQualifier.IDENTITY);
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(identity);
                }

                identityXref.setCvXrefQualifier(identity);
            }

            if (identityXref.getCvDatabase()  == null || !database.equalsIgnoreCase(identityXref.getCvDatabase().getIdentifier())){
                CvDatabase cvDatabase = factory.getCvObjectDao(CvDatabase.class).getByIdentifier(database);

                if (cvDatabase == null){
                    cvDatabase = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvDatabase.class, database, ontologyAccess.getOntologyID().toLowerCase());
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(cvDatabase);
                }

                identityXref.setCvDatabase(cvDatabase);
            }

            updateEvt.getUpdatedXrefs().add(identityXref);
        }

        // update xrefs
        updateXrefs(updateContext, updateEvt);

        // update aliases
        updateAliases(updateContext, updateEvt);

        // update annotations
        updateAnnotations(updateContext, updateEvt);

        // update parents only if not obsolete
        if (!isObsolete){
            updateParents(updateContext, updateEvt);
        }

        doUpdate(updateContext, factory, term, updateEvt);
    }

    public void doUpdate(CvUpdateContext updateContext, DaoFactory factory, CvDagObject term, UpdatedEvent updateEvt) {
        if (updateEvt.isTermUpdated()){
            // update/persist/delete xrefs
            /*XrefDao<CvObjectXref> xrefDao = factory.getXrefDao(CvObjectXref.class);

            for (CvObjectXref updated : updateEvt.getUpdatedXrefs()){
                xrefDao.update(updated);
            }
            for (CvObjectXref created : updateEvt.getCreatedXrefs()){
                xrefDao.persist(created);
            }
            for (CvObjectXref deleted : updateEvt.getDeletedXrefs()){
                xrefDao.delete(deleted);
            }

            // update/persist/delete aliases
            AliasDao<CvObjectAlias> aliasDao = factory.getAliasDao(CvObjectAlias.class);

            for (CvObjectAlias updated : updateEvt.getUpdatedAliases()){
                aliasDao.update(updated);
            }
            for (CvObjectAlias created : updateEvt.getCreatedAliases()){
                aliasDao.persist(created);
            }
            for (CvObjectAlias deleted : updateEvt.getDeletedAliases()){
                aliasDao.delete(deleted);
            }

            // update/persist/delete annotations
            AnnotationDao annotationDao = factory.getAnnotationDao();

            for (Annotation updated : updateEvt.getUpdatedAnnotations()){
                annotationDao.update(updated);
            }
            for (Annotation created : updateEvt.getCreatedAnnotations()){
                annotationDao.persist(created);
            }
            for (Annotation deleted : updateEvt.getDeletedAnnotations()){
                annotationDao.delete(deleted);
            }

            // update parents
            CvObjectDao<CvDagObject> cvDao = factory.getCvObjectDao(CvDagObject.class);

            for (CvDagObject created : updateEvt.getCreatedParents()){
                cvDao.update(created);
            }
            for (CvDagObject deleted : updateEvt.getDeletedParents()){
                cvDao.update(deleted);
            }

            // update term
            factory.getCvObjectDao(CvDagObject.class).update(term);*/

            // fire event
            CvUpdateManager manager = updateContext.getManager();
            manager.fireOnUpdateCase(updateEvt);
        }
    }

    public void updateParents(CvUpdateContext updateContext, UpdatedEvent updateEvt){

        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        IntactOntologyAccess ontologyAccess = updateContext.getOntologyAccess();
        IntactOntologyTermI ontologyTerm = updateContext.getOntologyTerm();
        CvDagObject term = updateContext.getCvTerm();

        // root terms to exclude
        Collection<IntactOntologyTermI> rootTerms = ontologyAccess.getRootTerms();

        // parents of the term in the ontology
        Set<IntactOntologyTermI> allParents = ontologyAccess.getDirectParents(ontologyTerm);
        // parents of the term in the ontology which are not root terms
        Set<IntactOntologyTermI> parents = new HashSet<IntactOntologyTermI>(CollectionUtils.subtract(allParents, rootTerms));
        // parents of the term in the ontology which are not root terms
        Set<IntactOntologyTermI> rootParents = new HashSet<IntactOntologyTermI>(CollectionUtils.intersection(allParents, rootTerms));

        // missing parents to create
        Set<IntactOntologyTermI> missingParents = new HashSet<IntactOntologyTermI>(parents);
        // parents to delete
        Collection<CvDagObject> cvParents = new ArrayList<CvDagObject>(term.getParents());

        // when updating parents, we only update parents from current ontology so we can filter parents which need to be excluded
        for (CvDagObject parent : cvParents){
            String identityValue = null;

            CvObjectXref identity = XrefUtils.getIdentityXref(parent, ontologyAccess.getDatabaseIdentifier());
            // this parent cannot be updated because is not from the same ontology
            if (identity == null){
                Matcher matcher = ontologyAccess.getDatabaseRegexp().matcher(parent.getIdentifier());

                if (matcher.find() && matcher.group().equalsIgnoreCase(parent.getIdentifier())){
                    identityValue = parent.getIdentifier();
                }
                else {
                    continue;
                }
            }
            else {
                identityValue = identity.getPrimaryId();
            }
            // check if parent exist
            if (identity != null) {
                boolean hasFound = false;

                // try to find a match in the ontology
                for (IntactOntologyTermI ontologyTermI : parents){
                    if (identityValue.equalsIgnoreCase(ontologyTermI.getTermAccession())){
                        missingParents.remove(ontologyTermI);
                        hasFound = true;
                    }
                }

                // parent which should be removed
                if (!hasFound){
                    // get term in the ontology
                    IntactOntologyTermI parentInOntology = ontologyAccess.getTermForAccession(identityValue);

                    if (parentInOntology == null && identity != null){
                        parentInOntology = ontologyAccess.getTermForAccession(identity.getPrimaryId());
                    }

                    // if term does not exist, we fire an error
                    if (parentInOntology == null){
                        CvUpdateManager updateManager = updateContext.getManager();

                        CvUpdateError error = updateManager.getErrorFactory().createCvUpdateError(UpdateError.non_existing_term, "The term " + updateContext.getIdentifier() + " has a parent " + identityValue + " which does not exist in the ontology.", identityValue, parent.getAc(), parent.getShortLabel());
                        UpdateErrorEvent errorEvt = new UpdateErrorEvent(this, error);

                        updateManager.fireOnUpdateError(errorEvt);
                    }
                    // if term is not obsolete, we remove it from the parents
                    else if (!ontologyAccess.isObsolete(parentInOntology)){
                        term.removeParent(parent);

                        updateEvt.getDeletedParents().add(parent);
                    }
                }
            }
        }

        // update parents for current ontology
        for (IntactOntologyTermI parent : missingParents){
            CvDagObject parentFromDb = factory.getCvObjectDao(CvDagObject.class).getByIdentifier(parent.getTermAccession());

            if (parentFromDb == null){
                if (this.missingParents.containsKey(parent.getTermAccession())){
                    this.missingParents.get(parent.getTermAccession()).add(term);
                }
                else {
                    Set<CvDagObject> objects = new HashSet<CvDagObject>();
                    objects.add(term);

                    this.missingParents.put(parent.getTermAccession(), objects);
                }
            }
            else {
                term.addParent(parentFromDb);
                updateEvt.getCreatedParents().add(parentFromDb);
            }
        }

        // update parents from other ontology
        if (ontologyAccess.getParentFromOtherOntology() != null && !rootParents.isEmpty()){
            String parentFromOtherOntology = ontologyAccess.getParentFromOtherOntology();
            boolean hasFoundParent = false;

            for (CvDagObject parent : cvParents){

                Collection<CvObjectXref> identities = XrefUtils.getIdentityXrefs(parent);
                // this parent cannot be updated because is not from the same ontology
                if (identities.isEmpty()){
                    if (parent.getIdentifier().equalsIgnoreCase(parentFromOtherOntology)){
                        hasFoundParent = true;
                    }
                }
                else {
                    for (CvObjectXref identit : identities){
                        if (identit.getPrimaryId().equals(parentFromOtherOntology)){
                            hasFoundParent = true;
                        }
                    }

                    hasFoundParent = parent.getIdentifier().equalsIgnoreCase(parentFromOtherOntology);
                }
            }

            if (!hasFoundParent){
                if (this.missingParents.containsKey(parentFromOtherOntology)){
                    this.missingParents.get(parentFromOtherOntology).add(term);
                }
                else {
                    Set<CvDagObject> objects = new HashSet<CvDagObject>();
                    objects.add(term);

                    this.missingParents.put(parentFromOtherOntology, objects);
                }
            }
        }
    }

    public void updateAnnotations(CvUpdateContext updateContext, UpdatedEvent updateEvt){
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        IntactOntologyTermI ontologyTerm = updateContext.getOntologyTerm();
        CvDagObject term = updateContext.getCvTerm();
        boolean isObsolete = updateContext.isTermObsolete();

        sortedCvAnnotations.clear();
        sortedCvAnnotations.addAll(term.getAnnotations());
        Iterator<Annotation> intactIterator = sortedCvAnnotations.iterator();

        sortedOntologyAnnotations.clear();
        sortedOntologyAnnotations.addAll(ontologyTerm.getAnnotations());
        Iterator<TermAnnotation> ontologyIterator = sortedOntologyAnnotations.iterator();

        // boolean value to know if url is in the annotations
        boolean hasFoundURL = false;
        // boolean value to know if definition is in the annotations
        boolean hasFoundDefinition = false;
        // boolean value to know if obsolete is in the annotations
        boolean hasFoundObsolete = false;
        // boolean value to know if hidden is in the annotations
        boolean isHidden = false;

        AnnotationDao annotationDao = factory.getAnnotationDao();

        Annotation currentIntact = null;
        TermAnnotation currentOntologyRef = null;
        CvTopic cvTopic = null;

        // the comments to create
        Set<String> comments = new HashSet<String>(ontologyTerm.getComments());

        // the CvTopic for comment
        CvTopic comment = null;

        if (intactIterator.hasNext() && ontologyIterator.hasNext()){
            currentIntact = intactIterator.next();
            currentOntologyRef = ontologyIterator.next();
            cvTopic = currentIntact.getCvTopic();

            if (cvTopic != null){
                do{
                    int topicComparator = cvTopic.getIdentifier().compareTo(currentOntologyRef.getTopicId());

                    // we have a db match
                    if (topicComparator == 0) {

                        int acComparator = currentIntact.getAnnotationText().compareTo(currentOntologyRef.getDescription());

                        // we have a primary id match
                        if (acComparator == 0) {
                            if (intactIterator.hasNext() && ontologyIterator.hasNext()){
                                currentIntact = intactIterator.next();
                                currentOntologyRef = ontologyIterator.next();
                                cvTopic = currentIntact.getCvTopic();
                            }
                            else {
                                currentIntact = null;
                                currentOntologyRef = null;
                                cvTopic = null;
                            }
                        }
                        //intact has no match in ontology
                        else if (acComparator > 0) {
                            updateEvt.getDeletedAnnotations().add(currentIntact);
                            term.removeAnnotation(currentIntact);

                            if (intactIterator.hasNext()){
                                currentIntact = intactIterator.next();
                                cvTopic = currentIntact.getCvTopic();
                            }
                            else {
                                currentIntact = null;
                                cvTopic = null;
                            }
                        }
                        //ontology has no match in intact
                        else {
                            CvTopic cvTop = cvTopic;

                            Annotation newAnnot = new Annotation(cvTop, currentOntologyRef.getDescription());
                            term.addAnnotation(newAnnot);

                            updateEvt.getCreatedAnnotations().add(newAnnot);

                            if (ontologyIterator.hasNext()){
                                currentOntologyRef = ontologyIterator.next();
                            }
                            else {
                                currentOntologyRef = null;
                            }
                        }
                    }
                    //intact has no match in ontology, we delete it excepted the identity xref
                    else if (topicComparator > 0) {
                        // we have a definition. Only one is allowed
                        if (CvTopic.DEFINITION.equalsIgnoreCase(cvTopic.getShortLabel())){
                            if (!hasFoundDefinition){
                                hasFoundDefinition = true;

                                // we update existing definition
                                if (!ontologyTerm.getDefinition().equalsIgnoreCase(currentIntact.getAnnotationText())){
                                    currentIntact.setAnnotationText(ontologyTerm.getDefinition());

                                    updateEvt.getUpdatedAnnotations().add(currentIntact);
                                }
                            }
                            else{
                                updateEvt.getDeletedAnnotations().add(currentIntact);
                                term.removeAnnotation(currentIntact);
                            }
                        }
                        // we have an obsolete annotation. Only one is allowed
                        else if (CvTopic.OBSOLETE_MI_REF.equalsIgnoreCase(cvTopic.getIdentifier())){
                            if (!hasFoundObsolete && isObsolete){
                                hasFoundObsolete = true;

                                if (ontologyTerm.getObsoleteMessage() ==  null && currentIntact.getAnnotationText() != null){
                                    currentIntact.setAnnotationText(null);

                                    updateEvt.getUpdatedAnnotations().add(currentIntact);
                                }
                                else if (ontologyTerm.getObsoleteMessage() !=  null && !ontologyTerm.getObsoleteMessage().equalsIgnoreCase(currentIntact.getAnnotationText())){
                                    currentIntact.setAnnotationText(ontologyTerm.getDefinition());

                                    updateEvt.getUpdatedAnnotations().add(currentIntact);
                                }
                            }
                            else{
                                updateEvt.getDeletedAnnotations().add(currentIntact);
                                term.removeAnnotation(currentIntact);
                            }
                        }
                        // the term is hidden. We do nothing for now
                        else if (CvTopic.HIDDEN.equalsIgnoreCase(cvTopic.getShortLabel())){
                            isHidden = true;
                        }
                        // we have a comment. Checks that it exists in the ontology
                        else if (CvTopic.COMMENT_MI_REF.equalsIgnoreCase(cvTopic.getIdentifier())){
                            comment = cvTopic;

                            // comment exist
                            if (comments.contains(currentIntact.getAnnotationText())){
                                comments.remove(currentIntact.getAnnotationText());
                            }
                            // comment does not exist but can be updated
                            else if (!comments.contains(currentIntact.getAnnotationText()) && comments.size() > 0){
                                currentIntact.setAnnotationText(comments.iterator().next());
                                updateEvt.getUpdatedAnnotations().add(currentIntact);

                                comments.remove(currentIntact.getAnnotationText());
                            }
                            // delete the comment
                            else {
                                updateEvt.getDeletedAnnotations().add(currentIntact);
                                term.removeAnnotation(currentIntact);
                            }
                        }
                        // we have a url. Only one url is allowed
                        else if (CvTopic.URL_MI_REF.equalsIgnoreCase(cvTopic.getIdentifier())){
                            if (!hasFoundURL){
                                hasFoundURL = true;

                                if (!ontologyTerm.getDefinition().equalsIgnoreCase(currentIntact.getAnnotationText())){
                                    currentIntact.setAnnotationText(ontologyTerm.getDefinition());

                                    updateEvt.getUpdatedAnnotations().add(currentIntact);
                                }
                            }
                        }
                        // checks specific annotations?
                        else {

                        }

                        if (intactIterator.hasNext()){
                            currentIntact = intactIterator.next();
                            cvTopic = currentIntact.getCvTopic();
                        }
                        else {
                            currentIntact = null;
                            cvTopic = null;
                        }
                    }
                    //ontology xref has no match in intact, needs to create it
                    else {
                        CvTopic cvTop = factory.getCvObjectDao(CvTopic.class).getByIdentifier(currentOntologyRef.getTopicId());

                        if (cvTop == null){
                            cvTop = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, currentOntologyRef.getTopicId(), currentOntologyRef.getTopic());
                            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(cvTop);
                        }

                        Annotation newAnnot = new Annotation(cvTop, currentOntologyRef.getDescription());
                        term.addAnnotation(newAnnot);

                        updateEvt.getCreatedAnnotations().add(newAnnot);

                        if (ontologyIterator.hasNext()){
                            currentOntologyRef = ontologyIterator.next();
                        }
                        else {
                            currentOntologyRef = null;
                        }   currentOntologyRef = null;
                    }
                } while (currentIntact != null && currentOntologyRef != null && cvTopic != null);
            }
        }

        // need to delete specific remaining intact annotations, keeps the others
        if (currentIntact != null || intactIterator.hasNext()){
            if (currentIntact == null ){
                currentIntact = intactIterator.next();
                cvTopic = currentIntact.getCvTopic();
            }

            do {
                // we have a definition. Only one is allowed
                if (CvTopic.DEFINITION.equalsIgnoreCase(cvTopic.getShortLabel())){
                    if (!hasFoundDefinition){
                        hasFoundDefinition = true;

                        // we update existing definition
                        if (!ontologyTerm.getDefinition().equalsIgnoreCase(currentIntact.getAnnotationText())){
                            currentIntact.setAnnotationText(ontologyTerm.getDefinition());

                            updateEvt.getUpdatedAnnotations().add(currentIntact);
                        }
                    }
                    else{
                        updateEvt.getDeletedAnnotations().add(currentIntact);
                        term.removeAnnotation(currentIntact);
                    }
                }
                // we have an obsolete annotation. Only one is allowed
                else if (CvTopic.OBSOLETE_MI_REF.equalsIgnoreCase(cvTopic.getIdentifier())){
                    if (!hasFoundObsolete && isObsolete){
                        hasFoundObsolete = true;

                        if (ontologyTerm.getObsoleteMessage() ==  null && currentIntact.getAnnotationText() != null){
                            currentIntact.setAnnotationText(null);

                            updateEvt.getUpdatedAnnotations().add(currentIntact);
                        }
                        else if (ontologyTerm.getObsoleteMessage() !=  null && !ontologyTerm.getObsoleteMessage().equalsIgnoreCase(currentIntact.getAnnotationText())){
                            currentIntact.setAnnotationText(ontologyTerm.getDefinition());

                            updateEvt.getUpdatedAnnotations().add(currentIntact);
                        }
                    }
                    else{
                        updateEvt.getDeletedAnnotations().add(currentIntact);
                        term.removeAnnotation(currentIntact);
                    }
                }
                // the term is hidden. We do nothing for now
                else if (CvTopic.HIDDEN.equalsIgnoreCase(cvTopic.getShortLabel())){
                    isHidden = true;
                }
                // we have a comment. Checks that it exists in the ontology
                else if (CvTopic.COMMENT_MI_REF.equalsIgnoreCase(cvTopic.getIdentifier())){
                    comment = cvTopic;

                    // comment exist
                    if (comments.contains(currentIntact.getAnnotationText())){
                        comments.remove(currentIntact.getAnnotationText());
                    }
                    // comment does not exist but can be updated
                    else if (!comments.contains(currentIntact.getAnnotationText()) && comments.size() > 0){
                        currentIntact.setAnnotationText(comments.iterator().next());
                        updateEvt.getUpdatedAnnotations().add(currentIntact);

                        comments.remove(currentIntact.getAnnotationText());
                    }
                    // delete the comment
                    else {
                        updateEvt.getDeletedAnnotations().add(currentIntact);
                        term.removeAnnotation(currentIntact);
                    }
                }
                // we have a url. Only one url is allowed
                else if (CvTopic.URL_MI_REF.equalsIgnoreCase(cvTopic.getIdentifier())){
                    if (!hasFoundURL){
                        hasFoundURL = true;

                        if (!ontologyTerm.getDefinition().equalsIgnoreCase(currentIntact.getAnnotationText())){
                            currentIntact.setAnnotationText(ontologyTerm.getDefinition());

                            updateEvt.getUpdatedAnnotations().add(currentIntact);
                        }
                    }
                }
                // checks specific annotations?
                else {

                }

                if (intactIterator.hasNext()){
                    currentIntact = intactIterator.next();
                    cvTopic = currentIntact.getCvTopic();
                }
                else {
                    currentIntact = null;
                    cvTopic = null;
                }
            }while (currentIntact != null && cvTopic != null);
        }

        if (currentOntologyRef != null || ontologyIterator.hasNext()){
            if (currentOntologyRef == null ){
                currentOntologyRef = ontologyIterator.next();
            }

            do {
                //ontology has no match in intact
                CvTopic cvTop = factory.getCvObjectDao(CvTopic.class).getByIdentifier(currentOntologyRef.getTopicId());

                if (cvTop == null){
                    cvTop = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, currentOntologyRef.getTopicId(), currentOntologyRef.getTopic());
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(cvTop);
                }

                Annotation newAnnot = new Annotation(cvTop, currentOntologyRef.getDescription());
                term.addAnnotation(newAnnot);

                updateEvt.getCreatedAnnotations().add(newAnnot);

                if (ontologyIterator.hasNext()){
                    currentOntologyRef = ontologyIterator.next();
                }
                else {
                    currentOntologyRef = null;
                }   currentOntologyRef = null;
            }
            while (currentOntologyRef != null);
        }

        // create missing definition
        if (!hasFoundDefinition && ontologyTerm.getDefinition() != null){
            CvTopic topicFromDb = factory.getCvObjectDao(CvTopic.class).getByShortLabel(CvTopic.DEFINITION);

            if (topicFromDb == null){
                topicFromDb = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, null, CvTopic.DEFINITION);
                IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(topicFromDb);
            }

            Annotation newAnnotation = new Annotation(topicFromDb, ontologyTerm.getDefinition());
            term.addAnnotation(newAnnotation);

            updateEvt.getCreatedAnnotations().add(newAnnotation);
        }

        // create missing url
        if (!hasFoundURL && ontologyTerm.getURL() != null){
            CvTopic topicFromDb = factory.getCvObjectDao(CvTopic.class).getByIdentifier(CvTopic.URL_MI_REF);

            if (topicFromDb == null){
                topicFromDb = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, CvTopic.URL_MI_REF, CvTopic.URL);
                IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(topicFromDb);
            }

            Annotation newAnnotation = new Annotation(topicFromDb, ontologyTerm.getURL());
            term.addAnnotation(newAnnotation);

            updateEvt.getCreatedAnnotations().add(newAnnotation);
        }

        // create missing obsolete
        if (!hasFoundObsolete && isObsolete){
            CvTopic topicFromDb = factory.getCvObjectDao(CvTopic.class).getByIdentifier(CvTopic.OBSOLETE_MI_REF);

            if (topicFromDb == null){
                topicFromDb = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, CvTopic.OBSOLETE_MI_REF, CvTopic.OBSOLETE);
                IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(topicFromDb);
            }

            Annotation newAnnotation = new Annotation(topicFromDb, ontologyTerm.getObsoleteMessage());
            term.addAnnotation(newAnnotation);

            updateEvt.getCreatedAnnotations().add(newAnnotation);
        }

        // hide term if obsolete
        if (isObsolete && !isHidden){
            updateEvt.getCreatedAnnotations().add(CvUpdateUtils.hideTerm(term, "obsolete term"));
        }

        // create missing comments
        if (!comments.isEmpty()){
            if (comment == null){
                comment = factory.getCvObjectDao(CvTopic.class).getByIdentifier(CvTopic.COMMENT_MI_REF);

                if (comment == null){
                    comment = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, CvTopic.COMMENT_MI_REF, CvTopic.COMMENT);
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(comment);
                }
            }

            for (String com : comments){
                Annotation newAnnotation = new Annotation(comment, com);
                term.addAnnotation(newAnnotation);

                updateEvt.getCreatedAnnotations().add(newAnnotation);
            }
        }

        sortedOntologyAnnotations.clear();
        sortedCvAnnotations.clear();
    }

    public void updateXrefs(CvUpdateContext updateContext, UpdatedEvent updateEvt){
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        IntactOntologyAccess ontologyAccess = updateContext.getOntologyAccess();
        IntactOntologyTermI ontologyTerm = updateContext.getOntologyTerm();
        CvDagObject term = updateContext.getCvTerm();

        sortedCvXrefs.clear();
        sortedCvXrefs.addAll(term.getXrefs());
        Iterator<CvObjectXref> intactIterator = sortedCvXrefs.iterator();

        sortedOntologyXrefs.clear();
        sortedOntologyXrefs.addAll(ontologyTerm.getDbXrefs());
        Iterator<TermDbXref> ontologyIterator = sortedOntologyXrefs.iterator();

        CvObjectXref currentIntact = null;
        TermDbXref currentOntologyRef = null;
        CvDatabase cvDatabase = null;
        CvXrefQualifier cvQualifier = null;

        if (intactIterator.hasNext() && ontologyIterator.hasNext()){
            currentIntact = intactIterator.next();
            currentOntologyRef = ontologyIterator.next();
            cvDatabase = currentIntact.getCvDatabase();
            cvQualifier = currentIntact.getCvXrefQualifier();

            if (cvDatabase != null && cvQualifier != null){
                do{
                    int dbComparator = cvDatabase.getIdentifier().compareTo(currentOntologyRef.getDatabaseId());

                    // we have a db match
                    if (dbComparator == 0) {

                        int qualifierComparator = cvQualifier.getIdentifier().compareTo(currentOntologyRef.getQualifierId());

                        // we have a qualifier match
                        if (qualifierComparator == 0) {
                            int acComparator = currentIntact.getPrimaryId().compareTo(currentOntologyRef.getAccession());

                            // we have a primary id match
                            if (acComparator == 0) {
                                if (intactIterator.hasNext() && ontologyIterator.hasNext()){
                                    currentIntact = intactIterator.next();
                                    currentOntologyRef = ontologyIterator.next();
                                    cvDatabase = currentIntact.getCvDatabase();
                                    cvQualifier = currentIntact.getCvXrefQualifier();
                                }
                                else {
                                    currentIntact = null;
                                    currentOntologyRef = null;
                                    cvDatabase = null;
                                    cvQualifier = null;
                                }
                            }
                            //intact has no match in ontology
                            else if (acComparator > 0) {
                                if ((!currentIntact.equals(updateContext.getIdentityXref()) && cvQualifier != null && !CvXrefQualifier.SECONDARY_AC_MI_REF.equalsIgnoreCase(cvQualifier.getIdentifier())) || cvQualifier == null){
                                    updateEvt.getDeletedXrefs().add(currentIntact);
                                    term.removeXref(currentIntact);
                                }

                                if (intactIterator.hasNext()){
                                    currentIntact = intactIterator.next();
                                    cvDatabase = currentIntact.getCvDatabase();
                                    cvQualifier = currentIntact.getCvXrefQualifier();
                                }
                                else {
                                    currentIntact = null;
                                    cvDatabase = null;
                                    cvQualifier = null;
                                }
                            }
                            //ontology has no match in intact
                            else {
                                CvDatabase cvDb = cvDatabase;
                                CvXrefQualifier cvQ = cvQualifier;

                                CvObjectXref newXref = new CvObjectXref(IntactContext.getCurrentInstance().getInstitution(), cvDb, currentOntologyRef.getAccession(), cvQ);
                                term.addXref(newXref);

                                updateEvt.getCreatedXrefs().add(newXref);

                                if (ontologyIterator.hasNext()){
                                    currentOntologyRef = ontologyIterator.next();
                                }
                                else {
                                    currentOntologyRef = null;
                                }
                            }
                        }
                        else if (qualifierComparator > 0) {
                            //intact has no match in ontology
                            if ((!currentIntact.equals(updateContext.getIdentityXref()) && cvQualifier != null && !CvXrefQualifier.SECONDARY_AC_MI_REF.equalsIgnoreCase(cvQualifier.getIdentifier())) || cvQualifier == null){
                                updateEvt.getDeletedXrefs().add(currentIntact);
                                term.removeXref(currentIntact);
                            }

                            if (intactIterator.hasNext()){
                                currentIntact = intactIterator.next();
                                cvDatabase = currentIntact.getCvDatabase();
                                cvQualifier = currentIntact.getCvXrefQualifier();
                            }
                            else {
                                currentIntact = null;
                                cvDatabase = null;
                                cvQualifier = null;
                            }
                        }
                        else {
                            //otology has no match in intact
                            CvDatabase cvDb = cvDatabase;
                            CvXrefQualifier cvQ = factory.getCvObjectDao(CvXrefQualifier.class).getByIdentifier(currentOntologyRef.getQualifierId());

                            if (cvQ== null){
                                cvQ = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvXrefQualifier.class, currentOntologyRef.getQualifierId(), currentOntologyRef.getQualifier());
                                IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(cvQ);
                            }

                            CvObjectXref newXref = new CvObjectXref(IntactContext.getCurrentInstance().getInstitution(), cvDb, currentOntologyRef.getAccession(), cvQ);
                            term.addXref(newXref);

                            updateEvt.getCreatedXrefs().add(newXref);

                            if (ontologyIterator.hasNext()){
                                currentOntologyRef = ontologyIterator.next();
                            }
                            else {
                                currentOntologyRef = null;
                            }
                        }
                    }
                    //intact has no match in ontology, we delete it excepted the identity xref
                    else if (dbComparator > 0) {
                        if (!currentIntact.equals(updateContext.getIdentityXref())){
                            updateEvt.getDeletedXrefs().add(currentIntact);
                            term.removeXref(currentIntact);
                        }

                        if (intactIterator.hasNext()){
                            currentIntact = intactIterator.next();
                            cvDatabase = currentIntact.getCvDatabase();
                            cvQualifier = currentIntact.getCvXrefQualifier();
                        }
                        else {
                            currentIntact = null;
                            cvDatabase = null;
                            cvQualifier = null;
                        }
                    }
                    //ontology xref has no match in intact, needs to create it
                    else {
                        CvDatabase cvDb = factory.getCvObjectDao(CvDatabase.class).getByIdentifier(currentOntologyRef.getDatabaseId());

                        if (cvDb == null){
                            cvDb = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvDatabase.class, currentOntologyRef.getDatabaseId(), currentOntologyRef.getDatabase());
                            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(cvDb);
                        }

                        CvXrefQualifier cvQ = factory.getCvObjectDao(CvXrefQualifier.class).getByIdentifier(currentOntologyRef.getQualifierId());

                        if (cvQ== null){
                            cvQ = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvXrefQualifier.class, currentOntologyRef.getQualifierId(), currentOntologyRef.getQualifier());
                            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(cvQ);
                        }

                        CvObjectXref newXref = new CvObjectXref(IntactContext.getCurrentInstance().getInstitution(), cvDb, currentOntologyRef.getAccession(), cvQ);
                        term.addXref(newXref);

                        updateEvt.getCreatedXrefs().add(newXref);

                        if (ontologyIterator.hasNext()){
                            currentOntologyRef = ontologyIterator.next();
                        }
                        else {
                            currentOntologyRef = null;
                        }
                    }
                } while (currentIntact != null && currentOntologyRef != null && cvDatabase != null && cvQualifier != null);
            }
        }

        // need to delete remaining intact xrefs
        if (currentIntact != null || intactIterator.hasNext()){
            if (currentIntact == null ){
                currentIntact = intactIterator.next();
                cvDatabase = currentIntact.getCvDatabase();
                cvQualifier = currentIntact.getCvXrefQualifier();
            }

            do {
                //intact has no match in ontology and it is not a secondary xref or the identity xref
                if ((!currentIntact.equals(updateContext.getIdentityXref()) && cvQualifier != null && !CvXrefQualifier.SECONDARY_AC_MI_REF.equalsIgnoreCase(cvQualifier.getIdentifier())) || cvQualifier == null){
                    updateEvt.getDeletedXrefs().add(currentIntact);

                    term.removeXref(currentIntact);
                }

                if (intactIterator.hasNext()){
                    currentIntact = intactIterator.next();
                    cvDatabase = currentIntact.getCvDatabase();
                    cvQualifier = currentIntact.getCvXrefQualifier();
                }
                else {
                    currentIntact = null;
                    cvDatabase = null;
                    cvQualifier = null;
                }
            }while (currentIntact != null);
        }

        if (currentOntologyRef != null || ontologyIterator.hasNext()){
            if (currentOntologyRef == null ){
                currentOntologyRef = ontologyIterator.next();
            }

            do {
                //ontology has no match in intact
                CvDatabase cvDb = factory.getCvObjectDao(CvDatabase.class).getByIdentifier(currentOntologyRef.getDatabaseId());

                if (cvDb == null){
                    cvDb = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvDatabase.class, currentOntologyRef.getDatabaseId(), currentOntologyRef.getDatabase());
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(cvDb);
                }

                CvXrefQualifier cvQ = factory.getCvObjectDao(CvXrefQualifier.class).getByIdentifier(currentOntologyRef.getQualifierId());

                if (cvQ == null){
                    cvQ = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvXrefQualifier.class, currentOntologyRef.getQualifierId(), currentOntologyRef.getQualifier());
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(cvQ);
                }

                CvObjectXref newXref = new CvObjectXref(IntactContext.getCurrentInstance().getInstitution(), cvDb, currentOntologyRef.getAccession(), cvQ);
                term.addXref(newXref);

                updateEvt.getCreatedXrefs().add(newXref);

                if (ontologyIterator.hasNext()){
                    currentOntologyRef = ontologyIterator.next();
                }
                else {
                    currentOntologyRef = null;
                }
            }
            while (currentOntologyRef != null);
        }

        sortedCvXrefs.clear();
        sortedOntologyXrefs.clear();
    }

    public void updateAliases(CvUpdateContext updateContext, UpdatedEvent updateEvt){
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        IntactOntologyTermI ontologyTerm = updateContext.getOntologyTerm();
        CvDagObject term = updateContext.getCvTerm();

        CvAliasType aliasType = factory.getCvObjectDao(CvAliasType.class).getByShortLabel(ALIAS_TYPE);

        if (aliasType == null){
            aliasType = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvAliasType.class, null, ALIAS_TYPE);
            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(aliasType);
        }

        // the aliases in the ontology to create
        sortedOntologyAliases.clear();
        sortedOntologyAliases.addAll(ontologyTerm.getAliases());
        Iterator<String> ontologyIterator = sortedOntologyAliases.iterator();

        sortedCvAliases.clear();
        sortedCvAliases.addAll(term.getAliases());
        Iterator<CvObjectAlias> intactIterator = sortedCvAliases.iterator();

        CvObjectAlias currentIntact = null;
        String currentOntologyAlias = null;

        if (intactIterator.hasNext() && ontologyIterator.hasNext()){
            currentIntact = intactIterator.next();
            currentOntologyAlias = ontologyIterator.next();

            if (currentIntact.getName() != null){
                do{
                    int nameComparator = currentIntact.getName().compareTo(currentOntologyAlias);

                    // we have a name match
                    if (nameComparator == 0) {
                        if (intactIterator.hasNext() && ontologyIterator.hasNext()){
                            currentIntact = intactIterator.next();
                            currentOntologyAlias = ontologyIterator.next();
                        }
                        else {
                            currentIntact = null;
                            currentOntologyAlias = null;
                        }
                    }
                    //intact has no match in ontology
                    else if (nameComparator > 0) {
                        updateEvt.getDeletedAliases().add(currentIntact);
                        term.removeAlias(currentIntact);

                        if (intactIterator.hasNext()){
                            currentIntact = intactIterator.next();
                        }
                        else {
                            currentIntact = null;
                        }
                    }
                    //ontology has no match in intact
                    else {

                        CvObjectAlias newAlias = new CvObjectAlias(IntactContext.getCurrentInstance().getInstitution(), term, aliasType, currentOntologyAlias);
                        term.addAlias(newAlias);

                        updateEvt.getCreatedAliases().add(newAlias);

                        if (ontologyIterator.hasNext()){
                            currentOntologyAlias = ontologyIterator.next();
                        }
                        else {
                            currentOntologyAlias = null;
                        }
                    }
                } while (currentIntact != null && currentOntologyAlias != null);
            }
        }

        // need to delete remaining intact xrefs
        if (currentIntact != null || intactIterator.hasNext()){
            if (currentIntact == null ){
                currentIntact = intactIterator.next();
            }

            do {
                //intact has no match in ontology
                updateEvt.getDeletedAliases().add(currentIntact);
                term.removeAlias(currentIntact);

                if (intactIterator.hasNext()){
                    currentIntact = intactIterator.next();
                }
                else {
                    currentIntact = null;
                }
            }while (currentIntact != null);
        }

        if (currentOntologyAlias != null || ontologyIterator.hasNext()){
            if (currentOntologyAlias == null ){
                currentOntologyAlias = ontologyIterator.next();
            }

            do {
                //ontology has no match in intact
                CvObjectAlias newAlias = new CvObjectAlias(IntactContext.getCurrentInstance().getInstitution(), term, aliasType, currentOntologyAlias);
                term.addAlias(newAlias);

                updateEvt.getCreatedAliases().add(newAlias);

                if (ontologyIterator.hasNext()){
                    currentOntologyAlias = ontologyIterator.next();
                }
                else {
                    currentOntologyAlias = null;
                }
            }
            while (currentOntologyAlias != null);
        }

        sortedCvAliases.clear();
        sortedOntologyAliases.clear();
    }

    public Map<String, Set<CvDagObject>> getMissingParents() {
        return missingParents;
    }

    public Set<String> getProcessedTerms() {
        return processedTerms;
    }

    public void clear(){
        this.missingParents.clear();
        this.processedTerms.clear();
    }
}
