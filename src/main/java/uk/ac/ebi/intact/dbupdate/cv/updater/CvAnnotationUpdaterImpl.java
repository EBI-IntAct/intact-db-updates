package uk.ac.ebi.intact.dbupdate.cv.updater;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.bridges.ontology_manager.TermAnnotation;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyTermI;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.AnnotationDao;
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * Updater of cv annotations
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>13/12/11</pre>
 */

public class CvAnnotationUpdaterImpl implements CvAnnotationUpdater {
    private TreeSet<TermAnnotation> sortedOntologyAnnotations;
    private TreeSet<Annotation> sortedCvAnnotations;

    // boolean value to know if url is in the annotations
    private boolean hasFoundURL = false;
    // boolean value to know if definition is in the annotations
    private boolean hasFoundDefinition = false;
    // boolean value to know if obsolete is in the annotations
    private boolean hasFoundObsolete = false;
    // boolean value to know if hidden is in the annotations
    private boolean isHidden = false;

    private Annotation currentIntact;
    private TermAnnotation currentOntologyRef;
    private CvTopic cvTopic = null;

    // the comments to create
    private Set<String> comments;

    // the CvTopic for comment
    private CvTopic comment = null;

    public CvAnnotationUpdaterImpl(){
        sortedCvAnnotations = new TreeSet<Annotation>(new CvAnnotationComparator());
        sortedOntologyAnnotations = new TreeSet<TermAnnotation>(new OntologyAnnotationComparator());

        comments = new HashSet<String>();
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void updateAnnotations(CvUpdateContext updateContext, UpdatedEvent updateEvt){
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        IntactOntologyTermI ontologyTerm = updateContext.getOntologyTerm();
        CvDagObject term = updateContext.getCvTerm();
        boolean isObsolete = updateContext.isTermObsolete();
        clear();

        sortedCvAnnotations.addAll(term.getAnnotations());
        Iterator<Annotation> intactIterator = sortedCvAnnotations.iterator();

        sortedOntologyAnnotations.addAll(ontologyTerm.getAnnotations());
        Iterator<TermAnnotation> ontologyIterator = sortedOntologyAnnotations.iterator();

        AnnotationDao annotationDao = factory.getAnnotationDao();

        // the comments to create
        comments.addAll(ontologyTerm.getComments());

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
                        else if (acComparator < 0) {
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
                    else if (topicComparator < 0) {
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

        clear();
    }

    public void clear(){
        sortedOntologyAnnotations.clear();
        sortedCvAnnotations.clear();
        hasFoundURL = false;
        // boolean value to know if definition is in the annotations
        hasFoundDefinition = false;
        // boolean value to know if obsolete is in the annotations
        hasFoundObsolete = false;
        // boolean value to know if hidden is in the annotations
        isHidden = false;

        currentIntact = null;
        currentOntologyRef = null;
        cvTopic = null;

        // the comments to create
        comments.clear();

        // the CvTopic for comment
        comment = null;
    }
}
