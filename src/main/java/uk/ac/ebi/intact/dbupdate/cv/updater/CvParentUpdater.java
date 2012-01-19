package uk.ac.ebi.intact.dbupdate.cv.updater;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyAccess;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyTermI;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.cv.CvUpdateContext;
import uk.ac.ebi.intact.dbupdate.cv.events.UpdatedEvent;
import uk.ac.ebi.intact.dbupdate.cv.utils.CvParentComparator;
import uk.ac.ebi.intact.dbupdate.cv.utils.OntologyParentComparator;
import uk.ac.ebi.intact.model.CvDagObject;
import uk.ac.ebi.intact.model.CvObjectXref;
import uk.ac.ebi.intact.model.util.XrefUtils;

import java.util.*;
import java.util.regex.Matcher;

/**
 * Updater for cv parents
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>13/12/11</pre>
 */

public class CvParentUpdater {
    private Map<String, Set<CvDagObject>> missingParents;

    private TreeSet<CvDagObject> sortedCvParents;
    private TreeSet<IntactOntologyTermI> sortedOntologyParents;

    private CvDagObject currentIntactParent;
    private IntactOntologyTermI currentOntologyParent;
    private String currentIdentity;

    public CvParentUpdater(){
        missingParents = new HashMap<String, Set<CvDagObject>>();

        sortedCvParents = new TreeSet<CvDagObject>(new CvParentComparator());
        sortedOntologyParents = new TreeSet<IntactOntologyTermI>(new OntologyParentComparator());
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void updateParents(CvUpdateContext updateContext, UpdatedEvent updateEvt){

        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        IntactOntologyAccess ontologyAccess = updateContext.getOntologyAccess();
        IntactOntologyTermI ontologyTerm = updateContext.getOntologyTerm();
        CvDagObject term = updateContext.getCvTerm();

        sortedCvParents.clear();
        sortedOntologyParents.clear();
        this.currentIntactParent = null;
        this.currentOntologyParent = null;
        this.currentIdentity = null;

        CvParentComparator cvParentComparator = (CvParentComparator) sortedCvParents.comparator();
        cvParentComparator.setDbIdentifier(ontologyAccess.getDatabaseIdentifier());
        cvParentComparator.setDbPattern(ontologyAccess.getDatabaseRegexp());

        sortedCvParents.addAll(term.getParents());
        Iterator<CvDagObject> intactIterator = sortedCvParents.iterator();

        sortedOntologyParents.addAll(ontologyAccess.getDirectParents(ontologyTerm));
        Iterator<IntactOntologyTermI> ontologyIterator = sortedOntologyParents.iterator();

        // root terms to exclude
        Collection<IntactOntologyTermI> rootTerms = ontologyAccess.getRootTerms();

        if (intactIterator.hasNext() && ontologyIterator.hasNext()){
            currentIntactParent = intactIterator.next();
            currentOntologyParent = ontologyIterator.next();

            initializeIdentityValue(ontologyAccess);

            if (currentIdentity != null && currentOntologyParent.getTermAccession() != null){
                do{
                    int idComparator = currentIdentity.compareTo(currentOntologyParent.getTermAccession());

                    // we have an id match
                    if (idComparator == 0) {

                        if (intactIterator.hasNext() && ontologyIterator.hasNext()){
                            currentIntactParent = intactIterator.next();
                            currentOntologyParent = ontologyIterator.next();
                            initializeIdentityValue(ontologyAccess);
                        }
                        else {
                            currentIntactParent = null;
                            currentOntologyParent = null;
                            currentIdentity = null;
                        }
                    }
                    // intact has a parent which is not in the ontology and which should be deleted because from same ontology
                    else if (idComparator < 0) {
                        IntactOntologyTermI parentNotInOntology = ontologyAccess.getTermForAccession(currentIdentity);

                        // the term does not exist in the ontology, we can delete it
                        if (parentNotInOntology == null){
                            updateEvt.getDeletedParents().add(currentIntactParent);
                            term.removeParent(currentIntactParent);
                        }
                        // the term parent exists and is not obsolete so we can remove it from the parents. We keep obsolete terms hierarchy
                        else if (parentNotInOntology != null && !ontologyAccess.isObsolete(parentNotInOntology)){
                            updateEvt.getDeletedParents().add(currentIntactParent);
                            term.removeParent(currentIntactParent);
                        }

                        if (intactIterator.hasNext()){
                            currentIntactParent = intactIterator.next();
                            initializeIdentityValue(ontologyAccess);
                        }
                        else {
                            currentIntactParent = null;
                            currentIdentity = null;
                        }
                    }
                    // the parent exists in the ontology but is not in IntAct so we need to create it
                    else {

                        // if the parent is not a root term, we can add the parent to the parent s to create or update existing parent in db to add this child
                        if (!rootTerms.contains(currentOntologyParent)){
                            CvDagObject parentFromDb = factory.getCvObjectDao(CvDagObject.class).getByIdentifier(currentOntologyParent.getTermAccession());

                            if (parentFromDb == null){
                                if (this.missingParents.containsKey(currentOntologyParent.getTermAccession())){
                                    this.missingParents.get(currentOntologyParent.getTermAccession()).add(term);
                                }
                                else {
                                    Set<CvDagObject> objects = new HashSet<CvDagObject>();
                                    objects.add(term);

                                    this.missingParents.put(currentOntologyParent.getTermAccession(), objects);
                                }
                            }
                            else {
                                term.addParent(parentFromDb);
                                updateEvt.getCreatedParents().add(parentFromDb);
                            }
                        }

                        if (ontologyIterator.hasNext()){
                            currentOntologyParent = ontologyIterator.next();
                        }
                        else {
                            currentOntologyParent = null;
                        }
                    }

                }while (currentIntactParent != null && currentOntologyParent != null && currentIdentity != null);
            }
        }

        // need to delete remaining intact parents from same ontology
        if (currentIntactParent != null || intactIterator.hasNext()){
            if (currentIntactParent == null ){
                currentIntactParent = intactIterator.next();
                initializeIdentityValue(ontologyAccess);
            }

            if (currentIdentity != null){
                do {
                    //intact has no match in ontology and should be deleted
                    IntactOntologyTermI parentNotInOntology = ontologyAccess.getTermForAccession(currentIdentity);

                    // the term does not exist in the ontology, we can delete it
                    if (parentNotInOntology == null){
                        updateEvt.getDeletedParents().add(currentIntactParent);
                        term.removeParent(currentIntactParent);
                    }
                    // the term parent exists and is not obsolete so we can remove it from the parents. We keep obsolete terms hierarchy
                    else if (parentNotInOntology != null && !ontologyAccess.isObsolete(parentNotInOntology)){
                        updateEvt.getDeletedParents().add(currentIntactParent);
                        term.removeParent(currentIntactParent);
                    }

                    if (intactIterator.hasNext()){
                        currentIntactParent = intactIterator.next();
                        initializeIdentityValue(ontologyAccess);
                    }
                    else {
                        currentIntactParent = null;
                        currentIdentity = null;
                    }
                }while (currentIntactParent != null && currentIdentity != null);
            }
        }

        if (currentOntologyParent != null || ontologyIterator.hasNext()){
            if (currentOntologyParent == null ){
                currentOntologyParent = ontologyIterator.next();
            }

            do {
                // if the parent is not a root term, we can add the parent to the parent s to create or update existing parent in db to add this child
                if (!rootTerms.contains(currentOntologyParent)){
                    CvDagObject parentFromDb = factory.getCvObjectDao(CvDagObject.class).getByIdentifier(currentOntologyParent.getTermAccession());

                    if (parentFromDb == null){
                        if (this.missingParents.containsKey(currentOntologyParent.getTermAccession())){
                            this.missingParents.get(currentOntologyParent.getTermAccession()).add(term);
                        }
                        else {
                            Set<CvDagObject> objects = new HashSet<CvDagObject>();
                            objects.add(term);

                            this.missingParents.put(currentOntologyParent.getTermAccession(), objects);
                        }
                    }
                    else {
                        term.addParent(parentFromDb);
                        updateEvt.getCreatedParents().add(parentFromDb);
                    }
                }

                if (ontologyIterator.hasNext()){
                    currentOntologyParent = ontologyIterator.next();
                }
                else {
                    currentOntologyParent = null;
                }
            }
            while (currentOntologyParent != null);
        }

        sortedCvParents.clear();
        sortedOntologyParents.clear();
        this.currentIntactParent = null;
        this.currentOntologyParent = null;
        this.currentIdentity = null;
    }

    public Map<String, Set<CvDagObject>> getMissingParents() {
        return missingParents;
    }

    private void initializeIdentityValue(IntactOntologyAccess ontologyAccess){
        CvObjectXref currentIdentityXref = XrefUtils.getIdentityXref(currentIntactParent, ontologyAccess.getDatabaseIdentifier());
        // this parent cannot be updated because is not from the same ontology
        if (currentIdentityXref == null){
            Matcher matcher = ontologyAccess.getDatabaseRegexp().matcher(currentIntactParent.getIdentifier());

            if (matcher.find() && matcher.group().equalsIgnoreCase(currentIntactParent.getIdentifier())){
                currentIdentity = currentIntactParent.getIdentifier();
            }
            else {
                currentIdentity = null;
            }
        }
        else {
            currentIdentity = currentIdentityXref.getPrimaryId();
        }
    }

    public void clear(){
        this.missingParents.clear();
        this.sortedCvParents.clear();
        this.sortedOntologyParents.clear();
        this.currentIntactParent = null;
        this.currentOntologyParent = null;
        this.currentIdentity = null;
    }
}
