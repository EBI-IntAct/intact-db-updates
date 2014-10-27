package uk.ac.ebi.intact.dbupdate.cv.updater;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyAccess;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.*;
import uk.ac.ebi.intact.dbupdate.cv.CvUpdateContext;
import uk.ac.ebi.intact.dbupdate.cv.CvUpdateManager;
import uk.ac.ebi.intact.dbupdate.cv.events.UpdatedEvent;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.XrefUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.regex.Matcher;

/**
 * Updater for cv parents of basic IntAct terms that are not part of any ontology but have an objclass that attach them to one of existing MI parents
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>13/12/11</pre>
 */

public class CvIntactParentUpdaterImpl implements CvParentUpdater{
    private Map<String, Set<CvDagObject>> missingParents;

    private CvDagObject currentIntactParent;
    private String currentIdentity;

    private Map<Class<? extends CvDagObject>, String> classMap;

    public CvIntactParentUpdaterImpl(){
        missingParents = new HashMap<String, Set<CvDagObject>>();

        classMap = new HashMap<Class<? extends CvDagObject>, String>();
    }

    @PostConstruct
    public void initializeClassMap(){
        classMap.put( CvInteraction.class, "MI:0001" );
        classMap.put( CvInteractionType.class, "MI:0190" );
        classMap.put( CvIdentification.class, "MI:0002" );
        classMap.put( CvFeatureIdentification.class, "MI:0003" );
        classMap.put( CvFeatureType.class, "MI:0116" );
        classMap.put( CvInteractorType.class, "MI:0313" );
        classMap.put( CvExperimentalPreparation.class, "MI:0346" );
        classMap.put( CvFuzzyType.class, "MI:0333" );
        classMap.put( CvXrefQualifier.class, "MI:0353" );
        classMap.put( CvDatabase.class, "MI:0444" );
        classMap.put( CvExperimentalRole.class, "MI:0495" );
        classMap.put( CvBiologicalRole.class, "MI:0500" );
        classMap.put( CvAliasType.class, "MI:0300" );
        classMap.put( CvTopic.class, "MI:0590" );
        classMap.put( CvParameterType.class, "MI:0640" );
        classMap.put( CvParameterUnit.class, "MI:0647" );
        classMap.put( CvConfidenceType.class, "MI:1064" );
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void updateParents(CvUpdateContext updateContext, UpdatedEvent updateEvt){

        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        IntactOntologyAccess ontologyAccess = updateContext.getOntologyAccess();
        CvDagObject term = updateContext.getCvTerm();

        this.currentIntactParent = null;
        this.currentIdentity = null;

        String parentId = classMap.get(term.getClass());
        boolean hasParentId = (parentId == null);
        if (hasParentId){
            return;
        }

        Iterator<CvDagObject> intactIterator = collectAllParents(term).iterator();

        // root terms to exclude
        Collection<String> rootTermsToExclude = Collections.EMPTY_LIST;

        if (updateContext.getManager() != null){
            rootTermsToExclude = updateContext.getManager().getRootTermsToExclude();
        }

        if (intactIterator.hasNext()){
            currentIntactParent = intactIterator.next();

            initializeIdentityValue(ontologyAccess);

            if (currentIdentity != null){
                do{
                    if (!hasParentId){
                        hasParentId = parentId.equals(currentIdentity);
                    }

                    if (intactIterator.hasNext()){
                        currentIntactParent = intactIterator.next();
                    }
                    else{
                        currentIntactParent = null;
                        currentIdentity = null;
                    }

                }while (currentIntactParent != null && !hasParentId && currentIdentity != null);
            }
        }

        if (!hasParentId){
            if (!rootTermsToExclude.contains(parentId)){
                CvDagObject parentFromDb = factory.getCvObjectDao(term.getClass()).getByIdentifier(parentId);

                if (parentFromDb == null){
                    if (this.missingParents.containsKey(parentId)){
                        this.missingParents.get(parentId).add(term);
                    }
                    else {
                        Set<CvDagObject> objects = new HashSet<CvDagObject>();
                        objects.add(term);

                        this.missingParents.put(parentId, objects);
                    }
                }
                else {
                    term.addParent(parentFromDb);
                    updateEvt.getCreatedParents().add(parentFromDb);
                }
            }
        }

        this.currentIntactParent = null;
        this.currentIdentity = null;

        doUpdate(updateContext, updateEvt);
    }

    private void doUpdate(CvUpdateContext updateContext, UpdatedEvent updateEvt) {
        if (updateEvt.isTermUpdated()){

            DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

            // update parents
            CvObjectDao<CvDagObject> cvDao = factory.getCvObjectDao(CvDagObject.class);

            for (CvDagObject created : updateEvt.getCreatedParents()){
                cvDao.update(created);
            }

            // update term
            factory.getCvObjectDao(CvDagObject.class).update(updateContext.getCvTerm());

            // fire event
            CvUpdateManager manager = updateContext.getManager();
            manager.fireOnUpdateCase(updateEvt);
        }
    }

    public Map<String, Set<CvDagObject>> getMissingParents() {
        return missingParents;
    }

    private void initializeIdentityValue(IntactOntologyAccess ontologyAccess){
        CvObjectXref currentIdentityXref = XrefUtils.getIdentityXref(currentIntactParent, ontologyAccess.getDatabaseIdentifier());
        // this parent cannot be updated because is not from the same ontology
        if (currentIdentityXref == null && ontologyAccess.getDatabaseRegexp() != null){
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

    private Set<CvDagObject> collectAllParents(CvDagObject parent){
        Set<CvDagObject> parents = new HashSet<CvDagObject>(parent.getParents());

        for (CvDagObject p : parent.getParents()){
            parents.addAll(p.getParents());
        }

        return parents;
    }

    public void clear(){
        this.missingParents.clear();
        this.currentIntactParent = null;
        this.currentIdentity = null;
    }
}
