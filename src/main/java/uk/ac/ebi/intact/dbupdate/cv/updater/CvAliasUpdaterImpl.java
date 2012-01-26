package uk.ac.ebi.intact.dbupdate.cv.updater;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyTermI;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.cv.CvUpdateContext;
import uk.ac.ebi.intact.dbupdate.cv.events.UpdatedEvent;
import uk.ac.ebi.intact.dbupdate.cv.utils.CvAliasComparator;
import uk.ac.ebi.intact.model.CvAliasType;
import uk.ac.ebi.intact.model.CvDagObject;
import uk.ac.ebi.intact.model.CvObjectAlias;
import uk.ac.ebi.intact.model.util.CvObjectUtils;

import java.util.Iterator;
import java.util.TreeSet;

/**
 * Updater of CvAliases
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>13/12/11</pre>
 */

public class CvAliasUpdaterImpl implements CvAliasUpdater{
    public final static String ALIAS_TYPE= "synonym";
    public final static String ALIAS_TYPE_MI = "MI:1041";

    private TreeSet<CvObjectAlias> sortedCvAliases;
    private TreeSet<String> sortedOntologyAliases;
    private CvObjectAlias currentIntact;
    private String currentOntologyAlias;

    public CvAliasUpdaterImpl() {

        sortedCvAliases = new TreeSet<CvObjectAlias>(new CvAliasComparator());
        sortedOntologyAliases = new TreeSet<String>();
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void updateAliases(CvUpdateContext updateContext, UpdatedEvent updateEvt){
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        IntactOntologyTermI ontologyTerm = updateContext.getOntologyTerm();
        CvDagObject term = updateContext.getCvTerm();

        CvAliasType aliasType = factory.getCvObjectDao(CvAliasType.class).getByIdentifier(ALIAS_TYPE_MI);

        if (aliasType == null){
            aliasType = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvAliasType.class, ALIAS_TYPE_MI, ALIAS_TYPE);
            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(aliasType);
        }

        currentIntact = null;
        currentOntologyAlias = null;

        // the aliases in the ontology to create
        sortedOntologyAliases.clear();
        sortedOntologyAliases.addAll(ontologyTerm.getAliases());
        Iterator<String> ontologyIterator = sortedOntologyAliases.iterator();

        sortedCvAliases.clear();
        sortedCvAliases.addAll(term.getAliases());
        Iterator<CvObjectAlias> intactIterator = sortedCvAliases.iterator();

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
                    else if (nameComparator < 0) {
                        if (updateEvt != null){
                            updateEvt.getDeletedAliases().add(currentIntact);
                        }
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

                        if (updateEvt != null){
                            updateEvt.getCreatedAliases().add(newAlias);
                        }

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
                if (updateEvt != null){
                    updateEvt.getDeletedAliases().add(currentIntact);
                }
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

                if (updateEvt != null){
                    updateEvt.getCreatedAliases().add(newAlias);
                }

                if (ontologyIterator.hasNext()){
                    currentOntologyAlias = ontologyIterator.next();
                }
                else {
                    currentOntologyAlias = null;
                }
            }
            while (currentOntologyAlias != null);
        }

        clear();
    }

    public void clear(){
        sortedCvAliases.clear();
        sortedOntologyAliases.clear();
        currentIntact = null;
        currentOntologyAlias = null;
    }
}
