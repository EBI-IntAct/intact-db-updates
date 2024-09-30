package uk.ac.ebi.intact.dbupdate.cv.updater;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.bridges.ontologymanager.MIOntologyTermI;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.cv.CvUpdateContext;
import uk.ac.ebi.intact.dbupdate.cv.events.UpdatedEvent;
import uk.ac.ebi.intact.dbupdate.cv.utils.CvAnnotationComparator;
import uk.ac.ebi.intact.dbupdate.cv.utils.CvUpdateUtils;
import uk.ac.ebi.intact.dbupdate.cv.utils.OntologyAnnotationComparator;
import uk.ac.ebi.intact.model.Annotation;
import uk.ac.ebi.intact.model.CvDagObject;
import uk.ac.ebi.intact.model.CvTopic;
import uk.ac.ebi.intact.model.util.CvObjectUtils;

import java.util.*;

/**
 * Updater of cv annotations
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>13/12/11</pre>
 */

//TODO Deal with comments and URL
public class CvAnnotationUpdaterImpl implements CvAnnotationUpdater {

    private final TreeSet<psidev.psi.mi.jami.model.Annotation> sortedOntologyAnnotations;
    private final TreeSet<Annotation> sortedCvAnnotations;

    // boolean value to know if url is in the annotations
    private boolean hasFoundURL = false;

    // boolean value to know if search url is in the annotations
    private boolean hasFoundSearchURL = false;

    // boolean value to know if validation regex is in the annotations
    private boolean hasFoundValidationRegexp = false;
    // boolean value to know if definition is in the annotations
    private boolean hasFoundDefinition = false;
    // boolean value to know if obsolete is in the annotations
    private boolean hasFoundObsolete = false;
    // boolean value to know if hidden is in the annotations
    private boolean isHidden = false;

    private Annotation currentIntact;
    private psidev.psi.mi.jami.model.Annotation currentOntologyRef;
    private CvTopic cvTopic = null;

    // the comments to create
    private final Set<String> comments;

    // the CvTopic for comment
    private CvTopic comment = null;

    public CvAnnotationUpdaterImpl() {
        sortedCvAnnotations = new TreeSet<>(new CvAnnotationComparator());
        sortedOntologyAnnotations = new TreeSet<>(new OntologyAnnotationComparator());

        comments = new HashSet<>();
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void updateAnnotations(CvUpdateContext updateContext, UpdatedEvent updateEvt) {
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        MIOntologyTermI ontologyTerm = updateContext.getOntologyTerm();
        CvDagObject term = updateContext.getCvTerm();
        boolean isObsolete = updateContext.isTermObsolete();
        clear();

        sortedCvAnnotations.addAll(term.getAnnotations());
        Iterator<Annotation> intactIterator = sortedCvAnnotations.iterator();

        sortedOntologyAnnotations.addAll(ontologyTerm.getDelegate().getAnnotations());
        Iterator<psidev.psi.mi.jami.model.Annotation> ontologyIterator = sortedOntologyAnnotations.iterator();

        // All the comments to create from the ontology.
        // We extract only the value part for the comments to maintain the logic that was in the updater before
        // migrating CvUpdate to jami-ontology-manager.
        Collection<psidev.psi.mi.jami.model.Annotation> commentAnnotations = AnnotationUtils.collectAllAnnotationsHavingTopic(
                ontologyTerm.getDelegate().getAnnotations(),CvTopic.COMMENT_MI_REF, CvTopic.COMMENT);

        for (psidev.psi.mi.jami.model.Annotation commentAnnotation : commentAnnotations) {
            comments.add(commentAnnotation.getValue());
        }

        if (intactIterator.hasNext() && ontologyIterator.hasNext()) {
            currentIntact = intactIterator.next();
            currentOntologyRef = ontologyIterator.next();
            cvTopic = currentIntact.getCvTopic();

            if (cvTopic != null) {
                do {
                    int topicComparator = cvTopic.getIdentifier().compareTo(currentOntologyRef.getTopic().getMIIdentifier());

                    // We have a db match, the annotation from Intact and from the ontology have the same topic
                    if (topicComparator == 0) {
                        processAnnotationsWithMatchingTopic(factory, term, ontologyTerm, isObsolete, intactIterator, ontologyIterator, updateEvt);
                    }
                    // Annotation from Intact has no match in ontology, we check if we need to delete it or update it
                    else if (topicComparator < 0) {
                        processAnnotationWithNoMatchInOntology(term, ontologyTerm, isObsolete, updateEvt, intactIterator);
                    }
                    // Annotation from ontology has no match in intact, needs to create it
                    else {
                        processMissingAnnotation(factory, term, updateEvt, ontologyIterator);
                    }
                } while (currentIntact != null && currentOntologyRef != null && cvTopic != null);
            }
        }

        // need to delete specific remaining intact annotations, keeps the others
        if (currentIntact != null || intactIterator.hasNext()) {
            if (currentIntact == null) {
                currentIntact = intactIterator.next();
                cvTopic = currentIntact.getCvTopic();
            }

            do {
                // Annotation from Intact has no match in ontology, we check if we need to delete it or update it
                processAnnotationWithNoMatchInOntology(term, ontologyTerm, isObsolete, updateEvt, intactIterator);
            } while (currentIntact != null && cvTopic != null);
        }

        // need to add annotations found in the ontology but missing in intact
        if (currentOntologyRef != null || ontologyIterator.hasNext()) {
            if (currentOntologyRef == null) {
                currentOntologyRef = ontologyIterator.next();
            }

            do {
                // Annotation from ontology has no match in intact, needs to create it
                processMissingAnnotation(factory, term, updateEvt, ontologyIterator);
            } while (currentOntologyRef != null);
        }

        // Create expected annotations that are missing, such as description or url
        // All these annotation should had been created or updated already, but we double-check here just in case
        createMissingAnnotations(factory, term, ontologyTerm, updateEvt, isObsolete);

        clear();
    }

    public void clear() {
        sortedOntologyAnnotations.clear();
        sortedCvAnnotations.clear();
        hasFoundURL = false;
        hasFoundSearchURL = false;
        hasFoundValidationRegexp = false;
        hasFoundDefinition = false;
        hasFoundObsolete = false;
        isHidden = false;

        currentIntact = null;
        currentOntologyRef = null;
        cvTopic = null;

        comments.clear();

        comment = null;
    }

    private void processAnnotationsWithMatchingTopic(
            DaoFactory factory,
            CvDagObject term,
            MIOntologyTermI ontologyTerm,
            boolean isObsolete,
            Iterator<Annotation> intactIterator,
            Iterator<psidev.psi.mi.jami.model.Annotation> ontologyIterator,
            UpdatedEvent updateEvt) {

        int acComparator;
        if (currentOntologyRef.getValue() == null && currentIntact.getAnnotationText() == null) {
            acComparator = 0;
        }
        else if (currentOntologyRef.getValue() != null && currentIntact.getAnnotationText() == null) {
            acComparator = -1;
        }
        else {
            acComparator = currentIntact.getAnnotationText().compareTo(currentOntologyRef.getValue());
        }

        // We have an exact match, the annotation from Intact and from the ontology contain the same text
        if (acComparator == 0) {
            processAnnotationsWithExactMatch(term, updateEvt, intactIterator, ontologyIterator);
        }
        // Annotation from Intact has no match in ontology, we need to delete it
        else if (acComparator < 0) {
            processAnnotationWithNoMatchInOntology(term, ontologyTerm, isObsolete, updateEvt, intactIterator);
        }
        // Annotation from ontology has no match in Intact, we need to add it
        else {
            processMissingAnnotation(factory, term, updateEvt, ontologyIterator);
        }
    }

    private void processAnnotationsWithExactMatch(
            CvDagObject term,
            UpdatedEvent updateEvt,
            Iterator<Annotation> intactIterator,
            Iterator<psidev.psi.mi.jami.model.Annotation> ontologyIterator) {

        // we have a definition annotation. Only one is allowed
        if (CvTopic.DEFINITION.equalsIgnoreCase(cvTopic.getShortLabel())) {
            if (!hasFoundDefinition) {
                hasFoundDefinition = true;
            }
            else {
                // One definition annotation has been found already, we delete the extra ones
                deleteAnnotation(term, updateEvt);
            }
        }
        // we have a url annotation. Only one is allowed
        else if (CvTopic.URL.equalsIgnoreCase(cvTopic.getShortLabel())) {
            if (!hasFoundURL) {
                hasFoundURL = true;
            }
            else {
                // One url annotation has been found already, we delete the extra ones
                deleteAnnotation(term, updateEvt);
            }
        }
        // we have a search url annotation. Only one is allowed
        else if (CvTopic.SEARCH_URL.equalsIgnoreCase(cvTopic.getShortLabel())) {
            if (!hasFoundSearchURL) {
                hasFoundSearchURL = true;
            }
            else {
                // One search url annotation has been found already, we delete the extra ones
                deleteAnnotation(term, updateEvt);
            }
        }
        // we have a validation regex annotation. Only one is allowed
        else if (CvTopic.XREF_VALIDATION_REGEXP.equalsIgnoreCase(cvTopic.getShortLabel())) {
            if (!hasFoundValidationRegexp) {
                hasFoundValidationRegexp = true;
            }
            else {
                // One validation regex annotation has been found already, we delete the extra ones
                deleteAnnotation(term, updateEvt);
            }
        }
        // we have an obsolete annotation. Only one is allowed
        else if (CvTopic.OBSOLETE.equalsIgnoreCase(cvTopic.getShortLabel())) {
            if (!hasFoundObsolete) {
                hasFoundObsolete = true;
            }
            else {
                // One obsolete annotation has been found already, we delete the extra ones
                deleteAnnotation(term, updateEvt);
            }
        }

        if (intactIterator.hasNext() && ontologyIterator.hasNext()) {
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

    private void processAnnotationWithNoMatchInOntology(
            CvDagObject term,
            MIOntologyTermI ontologyTerm,
            boolean isObsolete,
            UpdatedEvent updateEvt,
            Iterator<Annotation> intactIterator) {

        // we have a definition. Only one is allowed
        if (CvTopic.DEFINITION.equalsIgnoreCase(cvTopic.getShortLabel())) {
            if (!hasFoundDefinition) {
                hasFoundDefinition = true;

                // we update existing definition annotation with new text from ontology
                if (!ontologyTerm.getDelegate().getDefinition().equalsIgnoreCase(currentIntact.getAnnotationText())) {
                    updateAnnotation(updateEvt, ontologyTerm.getDelegate().getDefinition());
                }
            }
            else {
                // One definition annotation has been found already, we delete the extra ones
                deleteAnnotation(term, updateEvt);
            }
        }
        // we have an obsolete annotation. Only one is allowed
        else if (CvTopic.OBSOLETE_MI_REF.equalsIgnoreCase(cvTopic.getIdentifier())) {
            if (!hasFoundObsolete && isObsolete) {
                hasFoundObsolete = true;

                // we update existing obsolete annotation with new text from ontology
                if (ontologyTerm.getObsoleteMessage() ==  null && currentIntact.getAnnotationText() != null) {
                    updateAnnotation(updateEvt, null);
                }
                else if (ontologyTerm.getObsoleteMessage() !=  null && !ontologyTerm.getObsoleteMessage().equalsIgnoreCase(currentIntact.getAnnotationText())){
                    updateAnnotation(updateEvt, ontologyTerm.getObsoleteMessage());
                }
            }
            else{
                // One obsolete annotation has been found already, we delete the extra ones
                deleteAnnotation(term, updateEvt);
            }
        }
        // the term is hidden. We do nothing for now
        else if (CvTopic.HIDDEN.equalsIgnoreCase(cvTopic.getShortLabel())) {
            isHidden = true;
        }
        // we have a comment. Checks that it exists in the ontology
        else if (CvTopic.COMMENT_MI_REF.equalsIgnoreCase(cvTopic.getIdentifier())) {
            comment = cvTopic;

            // comment exist in the ontology
            if (comments.contains(currentIntact.getAnnotationText())) {
                comments.remove(currentIntact.getAnnotationText());
            }
            // comment does not exist but can be updated with a newer comment from the ontology
            else if (!comments.contains(currentIntact.getAnnotationText()) && !comments.isEmpty()) {
                updateAnnotation(updateEvt, comments.iterator().next());

                comments.remove(currentIntact.getAnnotationText());
            }
            // delete the comment annotation, as it does not exist in the ontology
            else {
                deleteAnnotation(term, updateEvt);
            }
        }
        // we have a url. Only one url is allowed
        else if (CvTopic.URL_MI_REF.equalsIgnoreCase(cvTopic.getIdentifier())) {
            if (!hasFoundURL) {
                // we update existing url annotation with new text from ontology
                hasFoundURL = updateAnnotationIfFoundInOntology(ontologyTerm, updateEvt, CvTopic.URL_MI_REF, CvTopic.URL);
            }
            else {
                // One url annotation has been found already, we delete the extra ones
                deleteAnnotation(term, updateEvt);
            }
        }
        // we have a search url. Only one search url is allowed
        else if (CvTopic.SEARCH_URL_MI_REF.equalsIgnoreCase(cvTopic.getIdentifier())) {
            if (!hasFoundSearchURL) {
                // we update existing search url annotation with new text from ontology
                hasFoundSearchURL = updateAnnotationIfFoundInOntology(
                        ontologyTerm, updateEvt, CvTopic.SEARCH_URL_MI_REF, CvTopic.SEARCH_URL);
            }
            else {
                // One search url annotation has been found already, we delete the extra ones
                deleteAnnotation(term, updateEvt);
            }
        }
        // we have a validation regex. Only one validation regex is allowed
        else if (CvTopic.XREF_VALIDATION_REGEXP_MI_REF.equalsIgnoreCase(cvTopic.getIdentifier())) {
            if (!hasFoundValidationRegexp) {
                // we update existing validation regex annotation with new text from ontology
                hasFoundValidationRegexp = updateAnnotationIfFoundInOntology(
                        ontologyTerm, updateEvt, CvTopic.XREF_VALIDATION_REGEXP_MI_REF, CvTopic.XREF_VALIDATION_REGEXP);
            }
            else {
                // One validation regex annotation has been found already, we delete the extra ones
                deleteAnnotation(term, updateEvt);
            }
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

    private void processMissingAnnotation(
            DaoFactory factory,
            CvDagObject term,
            UpdatedEvent updateEvt,
            Iterator<psidev.psi.mi.jami.model.Annotation> ontologyIterator) {

        CvTopic cvTop = factory.getCvObjectDao(CvTopic.class).getByIdentifier(currentOntologyRef.getTopic().getMIIdentifier());

        if (cvTop == null) {
            cvTop = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, currentOntologyRef.getTopic().getMIIdentifier(), currentOntologyRef.getTopic().getShortName());
            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(cvTop);
        }

        createNewAnnotationFromOntology(cvTop, term, updateEvt, ontologyIterator);
    }

    private void createNewAnnotationFromOntology(
            CvTopic cvTop,
            CvDagObject term,
            UpdatedEvent updateEvt,
            Iterator<psidev.psi.mi.jami.model.Annotation> ontologyIterator) {

        // we have a definition. Only one is allowed
        if (CvTopic.DEFINITION.equalsIgnoreCase(cvTop.getShortLabel())) {
            if (!hasFoundDefinition) {
                hasFoundDefinition = true;
                createNewAnnotation(cvTop, term, updateEvt, currentOntologyRef.getValue());
            }
        }
        // we have a definition. Only one is allowed
        else if (CvTopic.URL.equalsIgnoreCase(cvTop.getShortLabel())) {
            if (!hasFoundURL) {
                hasFoundURL = true;
                createNewAnnotation(cvTop, term, updateEvt, currentOntologyRef.getValue());
            }
        }
        // we have a definition. Only one is allowed
        else if (CvTopic.SEARCH_URL.equalsIgnoreCase(cvTop.getShortLabel())) {
            if (!hasFoundSearchURL) {
                hasFoundSearchURL = true;
                createNewAnnotation(cvTop, term, updateEvt, currentOntologyRef.getValue());
            }
        }
        // we have a definition. Only one is allowed
        else if (CvTopic.XREF_VALIDATION_REGEXP.equalsIgnoreCase(cvTop.getShortLabel())) {
            if (!hasFoundValidationRegexp) {
                hasFoundValidationRegexp = true;
                createNewAnnotation(cvTop, term, updateEvt, currentOntologyRef.getValue());
            }
        }
        // we have a definition. Only one is allowed
        else if (CvTopic.OBSOLETE.equalsIgnoreCase(cvTop.getShortLabel())) {
            if (!hasFoundObsolete) {
                hasFoundObsolete = true;
                createNewAnnotation(cvTop, term, updateEvt, currentOntologyRef.getValue());
            }
        }
        // we have a comment, we remove it from the list of comments to create and add it to Intact
        else if (CvTopic.COMMENT_MI_REF.equalsIgnoreCase(cvTop.getIdentifier())) {
            comment = cvTop;
            comments.remove(currentOntologyRef.getValue());
            createNewAnnotation(cvTop, term, updateEvt, currentOntologyRef.getValue());
        }
        // we have another type of annotation, we simply create it in Intact
        else {
            createNewAnnotation(cvTop, term, updateEvt, currentOntologyRef.getValue());
        }

        if (ontologyIterator.hasNext()) {
            currentOntologyRef = ontologyIterator.next();
        } else {
            currentOntologyRef = null;
        }
    }

    private void createMissingAnnotations(
            DaoFactory factory,
            CvDagObject term,
            MIOntologyTermI ontologyTerm,
            UpdatedEvent updateEvt,
            boolean isObsolete) {

        // create missing definition
        if (!hasFoundDefinition && ontologyTerm.getDelegate().getDefinition() != null) {
            createNewAnnotationWithTopic(factory, term, updateEvt, ontologyTerm.getDelegate().getDefinition(), null, CvTopic.DEFINITION);
        }

        // create missing url
        psidev.psi.mi.jami.model.Annotation urlAnnotation = AnnotationUtils.collectFirstAnnotationWithTopic(
                ontologyTerm.getDelegate().getAnnotations(),CvTopic.URL_MI_REF, CvTopic.URL);

        if (!hasFoundURL && urlAnnotation != null) {
            createNewAnnotationWithTopic(factory, term, updateEvt, urlAnnotation.getValue(), CvTopic.URL_MI_REF, CvTopic.URL);
        }

        // create missing search url
        psidev.psi.mi.jami.model.Annotation searchUrlAnnotation = AnnotationUtils.collectFirstAnnotationWithTopic(
                ontologyTerm.getDelegate().getAnnotations(),CvTopic.SEARCH_URL_MI_REF, CvTopic.SEARCH_URL);

        if (!hasFoundSearchURL && searchUrlAnnotation != null) {
            createNewAnnotationWithTopic(factory, term, updateEvt, searchUrlAnnotation.getValue(), CvTopic.SEARCH_URL_MI_REF, CvTopic.SEARCH_URL);
        }

        // create missing validation regex
        psidev.psi.mi.jami.model.Annotation validationRegexAnnotation = AnnotationUtils.collectFirstAnnotationWithTopic(
                ontologyTerm.getDelegate().getAnnotations(),CvTopic.XREF_VALIDATION_REGEXP_MI_REF, CvTopic.XREF_VALIDATION_REGEXP);

        if (!hasFoundValidationRegexp && validationRegexAnnotation != null) {
            createNewAnnotationWithTopic(factory, term, updateEvt, validationRegexAnnotation.getValue(), CvTopic.XREF_VALIDATION_REGEXP_MI_REF, CvTopic.XREF_VALIDATION_REGEXP);
        }

        // create missing obsolete
        if (!hasFoundObsolete && isObsolete) {
            createNewAnnotationWithTopic(factory, term, updateEvt, ontologyTerm.getObsoleteMessage(), CvTopic.OBSOLETE_MI_REF, CvTopic.OBSOLETE);
        }

        // hide term if obsolete
        if (isObsolete && !isHidden) {
            Annotation hidden = CvUpdateUtils.hideTerm(term, CvTopic.OBSOLETE_OLD);

            if (updateEvt != null) {
                updateEvt.getCreatedAnnotations().add(hidden);
            }
        }

        // create missing comments that have not been created or updated yet
        if (!comments.isEmpty()) {
            if (comment == null) {
                comment = factory.getCvObjectDao(CvTopic.class).getByIdentifier(CvTopic.COMMENT_MI_REF);

                if (comment == null) {
                    comment = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, CvTopic.COMMENT_MI_REF, CvTopic.COMMENT);
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(comment);
                }
            }

            for (String com : comments) {
                createNewAnnotation(comment, term, updateEvt, com);
            }
        }
    }

    private void createNewAnnotationWithTopic(
            DaoFactory factory,
            CvDagObject term,
            UpdatedEvent updateEvt,
            String annotationText,
            String topicId,
            String topicName) {

        CvTopic topicFromDb;
        if (topicId != null) {
            topicFromDb = factory.getCvObjectDao(CvTopic.class).getByIdentifier(topicId);
        } else {
            topicFromDb = factory.getCvObjectDao(CvTopic.class).getByShortLabel(topicName);
        }

        if (topicFromDb == null) {
            topicFromDb = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, topicId, topicName);
            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(topicFromDb);
        }

        createNewAnnotation(topicFromDb, term, updateEvt, annotationText);
    }

    private boolean updateAnnotationIfFoundInOntology(
            MIOntologyTermI ontologyTerm,
            UpdatedEvent updateEvt,
            String topicId,
            String topicName) {

        psidev.psi.mi.jami.model.Annotation annotation = AnnotationUtils.collectFirstAnnotationWithTopic(
                ontologyTerm.getDelegate().getAnnotations(), topicId, topicName);

        if (annotation != null) {
            updateAnnotation(updateEvt, annotation.getValue());
            return true;
        }
        return false;
    }

    private void createNewAnnotation(
            CvTopic cvTop,
            CvDagObject term,
            UpdatedEvent updateEvt,
            String annotationText) {

        Annotation newAnnot = new Annotation(cvTop, annotationText);
        term.addAnnotation(newAnnot);

        if (updateEvt != null) {
            updateEvt.getCreatedAnnotations().add(newAnnot);
        }
    }

    private void updateAnnotation(UpdatedEvent updateEvt, String annotationText) {
        currentIntact.setAnnotationText(annotationText);

        if (updateEvt != null) {
            updateEvt.getUpdatedAnnotations().add(currentIntact);
        }
    }

    private void deleteAnnotation(CvDagObject term, UpdatedEvent updateEvt) {
        if (updateEvt != null) {
            updateEvt.getDeletedAnnotations().add(currentIntact);
        }
        term.removeAnnotation(currentIntact);
    }
}
