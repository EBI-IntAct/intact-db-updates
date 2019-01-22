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
    private TreeSet<psidev.psi.mi.jami.model.Annotation> sortedOntologyAnnotations;
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
    private psidev.psi.mi.jami.model.Annotation currentOntologyRef;
    private CvTopic cvTopic = null;

    // the comments to create
    private Set<String> comments;

    // the CvTopic for comment
    private CvTopic comment = null;

    public CvAnnotationUpdaterImpl(){
        sortedCvAnnotations = new TreeSet<>(new CvAnnotationComparator());
        sortedOntologyAnnotations = new TreeSet<>(new OntologyAnnotationComparator());

        comments = new HashSet<String>();
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void updateAnnotations(CvUpdateContext updateContext, UpdatedEvent updateEvt){
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        MIOntologyTermI ontologyTerm = updateContext.getOntologyTerm();
        CvDagObject term = updateContext.getCvTerm();
        boolean isObsolete = updateContext.isTermObsolete();
        clear();

        sortedCvAnnotations.addAll(term.getAnnotations());
        Iterator<Annotation> intactIterator = sortedCvAnnotations.iterator();

        sortedOntologyAnnotations.addAll(ontologyTerm.getDelegate().getAnnotations());
        Iterator<psidev.psi.mi.jami.model.Annotation> ontologyIterator = sortedOntologyAnnotations.iterator();

        // the comments to create.
        // We extract only the value part for the comments to maintain the logic that was in the updater before
        // migrating CvUpdate to jami-ontology-manager.
        Collection<psidev.psi.mi.jami.model.Annotation> commentAnnotations = AnnotationUtils.collectAllAnnotationsHavingTopic(
                ontologyTerm.getDelegate().getAnnotations(),CvTopic.COMMENT_MI_REF, CvTopic.COMMENT);

        for (psidev.psi.mi.jami.model.Annotation commentAnnotation : commentAnnotations) {
            comments.add(commentAnnotation.getValue());
        }

        if (intactIterator.hasNext() && ontologyIterator.hasNext()){
            currentIntact = intactIterator.next();
            currentOntologyRef = ontologyIterator.next();
            cvTopic = currentIntact.getCvTopic();

            if (cvTopic != null){
                do{
                    int topicComparator = cvTopic.getIdentifier().compareTo(currentOntologyRef.getTopic().getMIIdentifier());

                    // we have a db match
                    if (topicComparator == 0) {
                        int acComparator;
                        if (currentOntologyRef.getValue() == null && currentIntact.getAnnotationText() == null){
                            acComparator = 0;
                        }
                        else if (currentOntologyRef.getValue() != null && currentIntact.getAnnotationText() == null){
                            acComparator = -1;
                        }
                        else {
                            acComparator = currentIntact.getAnnotationText().compareTo(currentOntologyRef.getValue());
                        }

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
                            if (updateEvt != null){
                                updateEvt.getDeletedAnnotations().add(currentIntact);

                            }
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

                            Annotation newAnnot = new Annotation(cvTop, currentOntologyRef.getValue());
                            term.addAnnotation(newAnnot);

                            if (updateEvt != null){
                                updateEvt.getCreatedAnnotations().add(newAnnot);
                            }

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
                                if (!ontologyTerm.getDelegate().getDefinition().equalsIgnoreCase(currentIntact.getAnnotationText())){
                                    currentIntact.setAnnotationText(ontologyTerm.getDelegate().getDefinition());

                                    if (updateEvt != null){
                                        updateEvt.getUpdatedAnnotations().add(currentIntact); 
                                    }
                                }
                            }
                            else{
                                if (updateEvt != null){
                                    updateEvt.getDeletedAnnotations().add(currentIntact);
                                }
                                term.removeAnnotation(currentIntact);
                            }
                        }
                        // we have an obsolete annotation. Only one is allowed
                        else if (CvTopic.OBSOLETE_MI_REF.equalsIgnoreCase(cvTopic.getIdentifier())){
                            if (!hasFoundObsolete && isObsolete){
                                hasFoundObsolete = true;

                                if (ontologyTerm.getObsoleteMessage() ==  null && currentIntact.getAnnotationText() != null){
                                    currentIntact.setAnnotationText(null);

                                    if (updateEvt != null){
                                        updateEvt.getUpdatedAnnotations().add(currentIntact);
 
                                    }
                                }
                                else if (ontologyTerm.getObsoleteMessage() !=  null && !ontologyTerm.getObsoleteMessage().equalsIgnoreCase(currentIntact.getAnnotationText())){
                                    currentIntact.setAnnotationText(ontologyTerm.getDelegate().getDefinition());

                                    if (updateEvt != null){
                                        updateEvt.getUpdatedAnnotations().add(currentIntact);
                                    }
                                }
                            }
                            else{
                                if (updateEvt != null){
                                    updateEvt.getDeletedAnnotations().add(currentIntact);
                                }
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

                                if (updateEvt != null){
                                    updateEvt.getUpdatedAnnotations().add(currentIntact);
                                }

                                comments.remove(currentIntact.getAnnotationText());
                            }
                            // delete the comment
                            else {
                                if (updateEvt != null){
                                    updateEvt.getDeletedAnnotations().add(currentIntact);
                                }
                                term.removeAnnotation(currentIntact);
                            }
                        }
                        // we have a url. Only one url is allowed
                        else if (CvTopic.URL_MI_REF.equalsIgnoreCase(cvTopic.getIdentifier())){
                            if (!hasFoundURL){
                                hasFoundURL = true;

                                if (!ontologyTerm.getDelegate().getDefinition().equalsIgnoreCase(currentIntact.getAnnotationText())){
                                    currentIntact.setAnnotationText(ontologyTerm.getDelegate().getDefinition());

                                    if (updateEvt != null){
                                        updateEvt.getUpdatedAnnotations().add(currentIntact);

                                    }
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
                        CvTopic cvTop = factory.getCvObjectDao(CvTopic.class).getByIdentifier(currentOntologyRef.getTopic().getMIIdentifier());

                        if (cvTop == null){
                            cvTop = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, currentOntologyRef.getTopic().getMIIdentifier(), currentOntologyRef.getTopic().getShortName());
                            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(cvTop);
                        }

                        Annotation newAnnot = new Annotation(cvTop, currentOntologyRef.getValue());
                        term.addAnnotation(newAnnot);

                        if (updateEvt != null){
                            updateEvt.getCreatedAnnotations().add(newAnnot);
                        }

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
                        if ((ontologyTerm.getDelegate().getDefinition() != null && currentIntact.getAnnotationText() != null && !ontologyTerm.getDelegate().getDefinition().equalsIgnoreCase(currentIntact.getAnnotationText()))
                                || (ontologyTerm.getDelegate().getDefinition() == null && currentIntact.getAnnotationText() != null) ||
                                (ontologyTerm.getDelegate().getDefinition() != null && currentIntact.getAnnotationText() == null)){
                            currentIntact.setAnnotationText(ontologyTerm.getDelegate().getDefinition());

                            if (updateEvt != null){
                                updateEvt.getUpdatedAnnotations().add(currentIntact);
                            }
                        }
                    }
                    else{
                        if (updateEvt != null){
                            updateEvt.getDeletedAnnotations().add(currentIntact);
                        }
                        term.removeAnnotation(currentIntact);
                    }
                }
                // we have an obsolete annotation. Only one is allowed
                else if (CvTopic.OBSOLETE_MI_REF.equalsIgnoreCase(cvTopic.getIdentifier())){
                    if (!hasFoundObsolete && isObsolete){
                        hasFoundObsolete = true;

                        if (ontologyTerm.getObsoleteMessage() ==  null && currentIntact.getAnnotationText() != null){
                            currentIntact.setAnnotationText(null);

                            if (updateEvt != null){
                                updateEvt.getUpdatedAnnotations().add(currentIntact); 
                            }
                        }
                        else if (ontologyTerm.getObsoleteMessage() !=  null && !ontologyTerm.getObsoleteMessage().equalsIgnoreCase(currentIntact.getAnnotationText())){
                            currentIntact.setAnnotationText(ontologyTerm.getDelegate().getDefinition());

                            if (updateEvt != null){
                                updateEvt.getUpdatedAnnotations().add(currentIntact);
                            }
                        }
                    }
                    else{
                        if (updateEvt != null){
                            updateEvt.getDeletedAnnotations().add(currentIntact);
                        }
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
                        if (updateEvt != null){
                            updateEvt.getUpdatedAnnotations().add(currentIntact);
                        }

                        comments.remove(currentIntact.getAnnotationText());
                    }
                    // delete the comment
                    else {
                        if (updateEvt != null){
                            updateEvt.getDeletedAnnotations().add(currentIntact);
                        }
                        term.removeAnnotation(currentIntact);
                    }
                }
                // we have a url. Only one url is allowed
                else if (CvTopic.URL_MI_REF.equalsIgnoreCase(cvTopic.getIdentifier())){
                    if (!hasFoundURL){
                        hasFoundURL = true;

                        if (!ontologyTerm.getDelegate().getDefinition().equalsIgnoreCase(currentIntact.getAnnotationText())){
                            currentIntact.setAnnotationText(ontologyTerm.getDelegate().getDefinition());

                            if (updateEvt != null){
                                updateEvt.getUpdatedAnnotations().add(currentIntact);
                            }
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
                CvTopic cvTop = factory.getCvObjectDao(CvTopic.class).getByIdentifier(currentOntologyRef.getTopic().getMIIdentifier());

                if (cvTop == null){
                    cvTop = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, currentOntologyRef.getTopic().getMIIdentifier(), currentOntologyRef.getTopic().getShortName());
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(cvTop);
                }

                Annotation newAnnot = new Annotation(cvTop, currentOntologyRef.getValue());
                term.addAnnotation(newAnnot);

                if (updateEvt != null){
                    updateEvt.getCreatedAnnotations().add(newAnnot);

                }

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
        if (!hasFoundDefinition && ontologyTerm.getDelegate().getDefinition() != null){
            CvTopic topicFromDb = factory.getCvObjectDao(CvTopic.class).getByShortLabel(CvTopic.DEFINITION);

            if (topicFromDb == null){
                topicFromDb = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, null, CvTopic.DEFINITION);
                IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(topicFromDb);
            }

            Annotation newAnnotation = new Annotation(topicFromDb, ontologyTerm.getDelegate().getDefinition());
            term.addAnnotation(newAnnotation);

            if (updateEvt != null){
                updateEvt.getCreatedAnnotations().add(newAnnotation);
            }
        }

//        // create missing url
        psidev.psi.mi.jami.model.Annotation urlAnnotation = AnnotationUtils.collectFirstAnnotationWithTopic(
                ontologyTerm.getDelegate().getAnnotations(),CvTopic.URL_MI_REF, CvTopic.URL);

        if (!hasFoundURL && urlAnnotation != null){
            CvTopic topicFromDb = factory.getCvObjectDao(CvTopic.class).getByIdentifier(CvTopic.URL_MI_REF);

            if (topicFromDb == null){
                topicFromDb = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, CvTopic.URL_MI_REF, CvTopic.URL);
                IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(topicFromDb);
            }

            Annotation newAnnotation = new Annotation(topicFromDb, urlAnnotation.getValue());
            term.addAnnotation(newAnnotation);

            if (updateEvt != null){
                updateEvt.getCreatedAnnotations().add(newAnnotation);

            }
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

            if (updateEvt != null){
                updateEvt.getCreatedAnnotations().add(newAnnotation);
            }
        }

        // hide term if obsolete
        if (isObsolete && !isHidden){
            Annotation hidden = CvUpdateUtils.hideTerm(term, "obsolete term");

            if (updateEvt != null){
                updateEvt.getCreatedAnnotations().add(hidden);
            }
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

                if (updateEvt != null){
                    updateEvt.getCreatedAnnotations().add(newAnnotation);
                }
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
//        comments.clear();

        // the CvTopic for comment
        comment = null;
    }
}
