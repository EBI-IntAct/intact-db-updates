package uk.ac.ebi.intact.dbupdate.cv;

import uk.ac.ebi.intact.bridges.ontology_manager.TermDbXref;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyAccess;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyTermI;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.cv.events.UpdatedEvent;
import uk.ac.ebi.intact.dbupdate.cv.utils.CvXrefComparator;
import uk.ac.ebi.intact.dbupdate.cv.utils.OntologyXrefComparator;
import uk.ac.ebi.intact.model.CvDagObject;
import uk.ac.ebi.intact.model.CvDatabase;
import uk.ac.ebi.intact.model.CvObjectXref;
import uk.ac.ebi.intact.model.CvXrefQualifier;
import uk.ac.ebi.intact.model.util.CvObjectUtils;

import java.util.Iterator;
import java.util.TreeSet;

/**
 * updater of cv xrefs
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>13/12/11</pre>
 */

public class CvXrefUpdater {
    private TreeSet<CvObjectXref> sortedCvXrefs;
    private TreeSet<TermDbXref> sortedOntologyXrefs;
    private CvObjectXref currentIntact;
    private TermDbXref currentOntologyRef;
    private CvDatabase cvDatabase;
    private CvXrefQualifier cvQualifier;

    public CvXrefUpdater(){
        sortedCvXrefs = new TreeSet<CvObjectXref>(new CvXrefComparator());
        sortedOntologyXrefs = new TreeSet<TermDbXref>(new OntologyXrefComparator());
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

        currentIntact = null;
        currentOntologyRef = null;
        cvDatabase = null;
        cvQualifier = null;

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
                            else if (acComparator < 0) {
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
                        else if (qualifierComparator < 0) {
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
                    else if (dbComparator < 0) {
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

        clear();
    }

    public void clear(){
        sortedCvXrefs.clear();
        sortedOntologyXrefs.clear();
        currentIntact = null;
        currentOntologyRef = null;
        cvDatabase = null;
        cvQualifier = null;
    }
}
