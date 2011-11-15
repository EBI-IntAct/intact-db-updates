package uk.ac.ebi.intact.dbupdate.cv;

import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyAccess;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyTermI;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.cv.errors.CvUpdateError;
import uk.ac.ebi.intact.dbupdate.cv.errors.UpdateError;
import uk.ac.ebi.intact.dbupdate.cv.events.ObsoleteRemappedEvent;
import uk.ac.ebi.intact.dbupdate.cv.events.ObsoleteTermImpossibleToRemapEvent;
import uk.ac.ebi.intact.dbupdate.cv.events.UpdateErrorEvent;
import uk.ac.ebi.intact.dbupdate.cv.utils.CvUpdateUtils;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;

import javax.persistence.Query;
import java.util.*;

/**
 * This class will try to remap an obsolete term
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>10/11/11</pre>
 */

public class ObsoleteCvRemapper {

    private Map<String, String> ontologyIdToDatabase;

    private Map<String, Set<CvDagObject>> remappedCvToUpdate;

    public ObsoleteCvRemapper() {
        ontologyIdToDatabase = new HashMap<String, String>();
        initializeOntologyIDToDatabase();

        remappedCvToUpdate = new HashMap<String, Set<CvDagObject>>();
    }

    private void initializeOntologyIDToDatabase(){
        ontologyIdToDatabase.put("MI", CvDatabase.PSI_MI_MI_REF);
        ontologyIdToDatabase.put("MOD", CvDatabase.PSI_MOD_MI_REF);
    }

    public void remapObsoleteCvTerm(CvUpdateContext updateContext){
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        IntactOntologyAccess ontologyAccess = updateContext.getOntologyAccess();
        IntactOntologyTermI ontologyTerm = updateContext.getOntologyTerm();
        CvDagObject term = updateContext.getCvTerm();
        String identifier = updateContext.getIdentifier();

        // boolean value to know if the term has been remapped or not
        boolean couldRemap = true;
        String newOntologyId = null;

        // the term can be remapped to another term
        if (ontologyTerm.getRemappedTerm() != null){
            String remappedDb = ontologyAccess.getDatabaseIdentifier();

            IntactOntologyTermI remappedTerm = null;

            // the remapped term is not from this ontology, so we need to know which database it is remapped to
            if (!ontologyTerm.getRemappedTerm().startsWith(ontologyAccess.getOntologyID())){

                String[] refInfo = ontologyTerm.getRemappedTerm().split(":");

                newOntologyId = refInfo[0];
                remappedDb = this.ontologyIdToDatabase.get(newOntologyId);

                // the remapped term is not known by this remapper so we cannot remap it
                if (remappedDb == null){
                    couldRemap = false;
                    updateContext.setTermObsolete(true);

                    CvUpdateManager manager = updateContext.getManager();

                    CvUpdateError error = manager.getErrorFactory().createCvUpdateError(UpdateError.ontology_database_no_found, ontologyTerm.getRemappedTerm() + " cannot be remapped to a database and so the obsolete term was not remapped.", ontologyTerm.getRemappedTerm(), term.getAc(), term.getShortLabel());
                    manager.fireOnUpdateError(new UpdateErrorEvent(this, error));
                }
            }
            // the remapped term is from the same ontology as the obsolete term so we can get the ontologyTerm
            else {

                remappedTerm = ontologyAccess.getTermForAccession(ontologyTerm.getTermAccession());

                // the remapped term cannot be found in the ontology so we cannot do the remapping
                if (remappedTerm == null){
                    couldRemap = false;

                    CvUpdateManager manager = updateContext.getManager();

                    CvUpdateError error = manager.getErrorFactory().createCvUpdateError(UpdateError.non_existing_term, ontologyTerm.getRemappedTerm() + " does not exist in the ontology and so the obsolete term was not remapped.", ontologyTerm.getRemappedTerm(), term.getAc(), term.getShortLabel());
                    manager.fireOnUpdateError(new UpdateErrorEvent(this, error));
                }
            }

            // if it is possible to remap the obsolete term
            if (couldRemap){
                CvDagObject termFromDb = factory.getCvObjectDao(CvDagObject.class).getByIdentifier(ontologyTerm.getRemappedTerm());

                // the term does not exist in the db, we just need to update the identifier and identity xref.
                // the remapped term will need to be updated
                if (termFromDb == null){

                    // update identifier
                    term.setIdentifier(ontologyTerm.getRemappedTerm());

                    // update identities
                    CvObjectXref identityXref = updateContext.getIdentityXref();

                    // no identity xrefs, we need to create one if possible
                    if (identityXref == null){
                        // create identity xref
                        CvObjectXref cvXref = CvUpdateUtils.createIdentityXref(term, remappedDb, ontologyTerm.getRemappedTerm());
                        factory.getXrefDao(CvObjectXref.class).persist(cvXref);
                    }
                    // one to several identity refs exist, we need to retrieve the one we need to update
                    else {
                        if (ontologyTerm.getRemappedTerm().startsWith(ontologyAccess.getOntologyID())){
                            identityXref.setPrimaryId(ontologyTerm.getRemappedTerm());
                            factory.getXrefDao(CvObjectXref.class).update(identityXref);
                        }
                        else if (ontologyTerm.getRemappedTerm().contains(":")) {

                            if (remappedDb != null){
                                CvDatabase remappedCvDb = factory.getCvObjectDao(CvDatabase.class).getByPsiMiRef(remappedDb);

                                if (remappedCvDb == null){
                                    remappedCvDb = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvDatabase.class, remappedDb, remappedDb);
                                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(remappedCvDb);
                                }

                                identityXref.setCvDatabase(remappedCvDb);
                                identityXref.setPrimaryId(ontologyTerm.getRemappedTerm());
                                factory.getXrefDao(CvObjectXref.class).update(identityXref);
                            }
                        }
                    }

                    // the term is not obsolete anymore and was successfully remapped to a new ontology term
                    updateContext.setTermObsolete(false);
                    updateContext.setOntologyTerm(ontologyTerm);

                    // remapped to another ontology, needs to be updated later
                    if (ontologyTerm == null && newOntologyId != null){
                        if (remappedCvToUpdate.containsKey(newOntologyId)){
                            remappedCvToUpdate.get(newOntologyId).add(term);
                        }
                        else {
                            Set<CvDagObject> cvs = new HashSet<CvDagObject>();
                            cvs.add(term);
                            remappedCvToUpdate.put(newOntologyId, cvs);
                        }
                    }
                }
                // merge current term with new term
                else {
                    int resultUpdate = 0;
                    CvUpdateManager manager = updateContext.getManager();

                    if (term instanceof CvAliasType && termFromDb instanceof CvAliasType){
                        Query query = factory.getEntityManager().createQuery("update Alias a set a.cvAliasType = :type" +
                                " where a.cvAliasType = :duplicate");
                        query.setParameter("type", termFromDb);
                        query.setParameter("duplicate", term);
                        resultUpdate = query.executeUpdate();

                        // fire event
                        ObsoleteRemappedEvent evt = new ObsoleteRemappedEvent(this, updateContext.getIdentifier(), ontologyTerm.getRemappedTerm(), term.getAc(), termFromDb.getAc(), resultUpdate, "We could update aliasType of " + resultUpdate + " existing aliases.");

                        manager.fireOnRemappedObsolete(evt);
                    }
                    else if (term instanceof CvBiologicalRole && termFromDb instanceof CvBiologicalRole){
                        Query query = factory.getEntityManager().createQuery("update Component c set c.cvBiologicalRole = :role" +
                                " where c.cvBiologicalRole = :duplicate");
                        query.setParameter("role", termFromDb);
                        query.setParameter("duplicate", term);
                        resultUpdate = query.executeUpdate();

                        // fire event
                        ObsoleteRemappedEvent evt = new ObsoleteRemappedEvent(this, updateContext.getIdentifier(), ontologyTerm.getRemappedTerm(), term.getAc(), termFromDb.getAc(), resultUpdate, "We could update biological role of " + resultUpdate + " existing components.");

                        manager.fireOnRemappedObsolete(evt);
                    }
                    else if (term instanceof CvCellType && termFromDb instanceof CvCellType){
                        Query query = factory.getEntityManager().createQuery("update BioSource b set b.cvCellType = :type" +
                                " where b.cvCellType = :duplicate");
                        query.setParameter("type", termFromDb);
                        query.setParameter("duplicate", term);
                        resultUpdate = query.executeUpdate();

                        // fire event
                        ObsoleteRemappedEvent evt = new ObsoleteRemappedEvent(this, updateContext.getIdentifier(), ontologyTerm.getRemappedTerm(), term.getAc(), termFromDb.getAc(), resultUpdate, "We could update cell type of " + resultUpdate + " existing bioSources.");

                        manager.fireOnRemappedObsolete(evt);
                    }
                    else if (term instanceof CvCellCycle && termFromDb instanceof CvCellCycle){
                        Query query = factory.getEntityManager().createQuery("update BioSource b set b.cvCellCycle = :cycle" +
                                " where b.cvCellCycle = :duplicate");
                        query.setParameter("cycle", termFromDb);
                        query.setParameter("duplicate", term);
                        resultUpdate = query.executeUpdate();

                        // fire event
                        ObsoleteRemappedEvent evt = new ObsoleteRemappedEvent(this, updateContext.getIdentifier(), ontologyTerm.getRemappedTerm(), term.getAc(), termFromDb.getAc(), resultUpdate, "We could update cell cycle of " + resultUpdate + " existing bioSources.");

                        manager.fireOnRemappedObsolete(evt);
                    }
                    else if (term instanceof CvTissue && termFromDb instanceof CvTissue){
                        Query query = factory.getEntityManager().createQuery("update BioSource b set b.cvTissue = :tissue" +
                                " where b.cvTissue = :duplicate");
                        query.setParameter("tissue", termFromDb);
                        query.setParameter("duplicate", term);
                        resultUpdate = query.executeUpdate();

                        // fire event
                        ObsoleteRemappedEvent evt = new ObsoleteRemappedEvent(this, updateContext.getIdentifier(), ontologyTerm.getRemappedTerm(), term.getAc(), termFromDb.getAc(), resultUpdate, "We could update tissues of " + resultUpdate + " existing bioSources.");

                        manager.fireOnRemappedObsolete(evt);
                    }
                    else if (term instanceof CvComponentRole && termFromDb instanceof CvComponentRole){
                        Query query = factory.getEntityManager().createQuery("update Component c set c.cvComponentRole = :role" +
                                " where c.cvComponentRole = :duplicate");
                        query.setParameter("role", termFromDb);
                        query.setParameter("duplicate", term);
                        resultUpdate = query.executeUpdate();

                        // fire event
                        ObsoleteRemappedEvent evt = new ObsoleteRemappedEvent(this, updateContext.getIdentifier(), ontologyTerm.getRemappedTerm(), term.getAc(), termFromDb.getAc(), resultUpdate, "We could update component roles of " + resultUpdate + " existing components.");

                        manager.fireOnRemappedObsolete(evt);
                    }
                    else if (term instanceof CvCompartment && termFromDb instanceof CvCompartment){
                        Query query = factory.getEntityManager().createQuery("update BioSource b set b.cvCompartment = :compartment" +
                                " where b.cvCompartment = :duplicate");
                        query.setParameter("compartment", termFromDb);
                        query.setParameter("duplicate", term);
                        resultUpdate = query.executeUpdate();

                        // fire event
                        ObsoleteRemappedEvent evt = new ObsoleteRemappedEvent(this, updateContext.getIdentifier(), ontologyTerm.getRemappedTerm(), term.getAc(), termFromDb.getAc(), resultUpdate, "We could update compartments of " + resultUpdate + " existing bioSources.");

                        manager.fireOnRemappedObsolete(evt);
                    }
                    else if (term instanceof CvConfidenceType && termFromDb instanceof CvConfidenceType){
                        Query query = factory.getEntityManager().createQuery("update ComponentConfidence c set c.cvConfidenceType = :type" +
                                " where c.cvConfidenceType = :duplicate");
                        query.setParameter("type", termFromDb);
                        query.setParameter("duplicate", term);
                        resultUpdate = query.executeUpdate();

                        Query query2 = factory.getEntityManager().createQuery("update Confidence c set c.cvConfidenceType = :type" +
                                " where c.cvConfidenceType = :duplicate");
                        query2.setParameter("type", termFromDb);
                        query2.setParameter("duplicate", term);
                        resultUpdate += query2.executeUpdate();

                        // fire event
                        ObsoleteRemappedEvent evt = new ObsoleteRemappedEvent(this, updateContext.getIdentifier(), ontologyTerm.getRemappedTerm(), term.getAc(), termFromDb.getAc(), resultUpdate, "We could update confidence types of " + resultUpdate + " existing confidences.");

                        manager.fireOnRemappedObsolete(evt);
                    }
                    else if (term instanceof CvDatabase && termFromDb instanceof CvDatabase){
                        Query query = factory.getEntityManager().createQuery("update Xref x set x.cvDatabase = :db" +
                                " where x.cvDatabase = :duplicate");
                        query.setParameter("db", termFromDb);
                        query.setParameter("duplicate", term);
                        resultUpdate = query.executeUpdate();

                        // fire event
                        ObsoleteRemappedEvent evt = new ObsoleteRemappedEvent(this, updateContext.getIdentifier(), ontologyTerm.getRemappedTerm(), term.getAc(), termFromDb.getAc(), resultUpdate, "We could update databases of " + resultUpdate + " existing xrefs.");

                        manager.fireOnRemappedObsolete(evt);
                    }
                    else if (term instanceof CvExperimentalPreparation && termFromDb instanceof CvExperimentalPreparation){
                        Query query = factory.getEntityManager().createQuery("select c from Component c join c.experimentalPreparations exp " +
                                "where exp = :prepa");
                        query.setParameter("prepa", termFromDb);

                        List<Component> components = query.getResultList();

                        for (Component c : components){
                            c.getExperimentalPreparations().remove(term);
                            c.getExperimentalPreparations().add((CvExperimentalPreparation) termFromDb);

                            factory.getComponentDao().update(c);
                        }

                        // fire event
                        ObsoleteRemappedEvent evt = new ObsoleteRemappedEvent(this, updateContext.getIdentifier(), ontologyTerm.getRemappedTerm(), term.getAc(), termFromDb.getAc(), resultUpdate, "We could update experimental preparations of " + resultUpdate + " existing components.");

                        manager.fireOnRemappedObsolete(evt);
                    }
                    else if (term instanceof CvExperimentalRole && termFromDb instanceof CvExperimentalRole){
                        Query query = factory.getEntityManager().createQuery("select c from Component c join c.experimentalRoles r " +
                                "where r = :role");
                        query.setParameter("role", termFromDb);

                        List<Component> components = query.getResultList();

                        for (Component c : components){
                            c.getExperimentalRoles().remove(term);
                            c.getExperimentalRoles().add((CvExperimentalRole) termFromDb);

                            factory.getComponentDao().update(c);
                            resultUpdate++;
                        }

                        // fire event
                        ObsoleteRemappedEvent evt = new ObsoleteRemappedEvent(this, updateContext.getIdentifier(), ontologyTerm.getRemappedTerm(), term.getAc(), termFromDb.getAc(), resultUpdate, "We could update experimental roles of " + resultUpdate + " existing components.");

                        manager.fireOnRemappedObsolete(evt);
                    }
                    else if (term instanceof CvFeatureType && termFromDb instanceof CvFeatureType){
                        Query query = factory.getEntityManager().createQuery("update Feature f set f.cvFeatureType = :type" +
                                " where f.cvFeatureType = :duplicate");
                        query.setParameter("type", termFromDb);
                        query.setParameter("duplicate", term);
                        resultUpdate = query.executeUpdate();

                        // fire event
                        ObsoleteRemappedEvent evt = new ObsoleteRemappedEvent(this, updateContext.getIdentifier(), ontologyTerm.getRemappedTerm(), term.getAc(), termFromDb.getAc(), resultUpdate, "We could update feature types of " + resultUpdate + " existing features.");

                        manager.fireOnRemappedObsolete(evt);
                    }
                    else if (term instanceof CvFeatureIdentification && termFromDb instanceof CvFeatureIdentification){
                        Query query = factory.getEntityManager().createQuery("update Feature f set f.cvFeatureIdentification = :type" +
                                " where f.cvFeatureIdentification = :duplicate");
                        query.setParameter("type", termFromDb);
                        query.setParameter("duplicate", term);
                        resultUpdate = query.executeUpdate();

                        // fire event
                        ObsoleteRemappedEvent evt = new ObsoleteRemappedEvent(this, updateContext.getIdentifier(), ontologyTerm.getRemappedTerm(), term.getAc(), termFromDb.getAc(), resultUpdate, "We could update feature detection method of " + resultUpdate + " existing features.");

                        manager.fireOnRemappedObsolete(evt);
                    }
                    else if (term instanceof CvFuzzyType && termFromDb instanceof CvFuzzyType){
                        Query query = factory.getEntityManager().createQuery("update Range r set r.fromCvFuzzyType = :type" +
                                " where r.fromCvFuzzyType = :duplicate");
                        query.setParameter("type", termFromDb);
                        query.setParameter("duplicate", term);
                        resultUpdate = query.executeUpdate();

                        Query query2 = factory.getEntityManager().createQuery("update Range r set r.toCvFuzzyType = :type" +
                                " where r.toCvFuzzyType = :duplicate");
                        query2.setParameter("type", termFromDb);
                        query2.setParameter("duplicate", term);
                        resultUpdate += query2.executeUpdate();

                        // fire event
                        ObsoleteRemappedEvent evt = new ObsoleteRemappedEvent(this, updateContext.getIdentifier(), ontologyTerm.getRemappedTerm(), term.getAc(), termFromDb.getAc(), resultUpdate, "We could update range status of " + resultUpdate + " existing ranges.");

                        manager.fireOnRemappedObsolete(evt);
                    }
                    else if (term instanceof CvIdentification && termFromDb instanceof CvIdentification){
                        Query query = factory.getEntityManager().createQuery("update Experiment e set e.cvIdentification = :type" +
                                " where e.cvIdentification = :duplicate");
                        query.setParameter("type", termFromDb);
                        query.setParameter("duplicate", term);
                        resultUpdate = query.executeUpdate();

                        Query query2 = factory.getEntityManager().createQuery("select c from Component c join c.participantDetectionMethods p " +
                                "where p = :method");
                        query2.setParameter("method", termFromDb);

                        List<Component> components = query2.getResultList();

                        for (Component c : components){
                            c.getParticipantDetectionMethods().remove(term);
                            c.getParticipantDetectionMethods().add((CvIdentification) termFromDb);

                            factory.getComponentDao().update(c);
                            resultUpdate++;
                        }

                        // fire event
                        ObsoleteRemappedEvent evt = new ObsoleteRemappedEvent(this, updateContext.getIdentifier(), ontologyTerm.getRemappedTerm(), term.getAc(), termFromDb.getAc(), resultUpdate, "We could update participant detection methods of " + resultUpdate + " existing components/experiments.");

                        manager.fireOnRemappedObsolete(evt);
                    }
                    else if (term instanceof CvInteraction && termFromDb instanceof CvInteraction){
                        Query query = factory.getEntityManager().createQuery("update Experiment e set e.cvInteraction = :type" +
                                " where e.cvInteraction = :duplicate");
                        query.setParameter("type", termFromDb);
                        query.setParameter("duplicate", term);
                        resultUpdate = query.executeUpdate();

                        Query query2 = factory.getEntityManager().createQuery("update MineInteraction m set m.detectionMethod = :type" +
                                " where m.detectionMethod = :duplicate");
                        query2.setParameter("type", termFromDb);
                        query2.setParameter("duplicate", term);
                        resultUpdate += query2.executeUpdate();

                        // fire event
                        ObsoleteRemappedEvent evt = new ObsoleteRemappedEvent(this, updateContext.getIdentifier(), ontologyTerm.getRemappedTerm(), term.getAc(), termFromDb.getAc(), resultUpdate, "We could update interaction detection methods of " + resultUpdate + " existing MineInteractions/experiments.");

                        manager.fireOnRemappedObsolete(evt);
                    }
                    else if (term instanceof CvInteractionType && termFromDb instanceof CvInteractionType){
                        Query query = factory.getEntityManager().createQuery("update InteractionImpl i set i.cvInteractionType = :type" +
                                " where i.cvInteractionType = :duplicate");
                        query.setParameter("type", termFromDb);
                        query.setParameter("duplicate", term);
                        resultUpdate = query.executeUpdate();

                        // fire event
                        ObsoleteRemappedEvent evt = new ObsoleteRemappedEvent(this, updateContext.getIdentifier(), ontologyTerm.getRemappedTerm(), term.getAc(), termFromDb.getAc(), resultUpdate, "We could update interaction types of " + resultUpdate + " existing interactions.");

                        manager.fireOnRemappedObsolete(evt);
                    }
                    else if (term instanceof CvInteractorType && termFromDb instanceof CvInteractorType){
                        Query query = factory.getEntityManager().createQuery("update InteractorImpl i set i.cvInteractorType = :type" +
                                " where i.cvInteractorType = :duplicate");
                        query.setParameter("type", termFromDb);
                        query.setParameter("duplicate", term);
                        resultUpdate = query.executeUpdate();

                        // fire event
                        ObsoleteRemappedEvent evt = new ObsoleteRemappedEvent(this, updateContext.getIdentifier(), ontologyTerm.getRemappedTerm(), term.getAc(), termFromDb.getAc(), resultUpdate, "We could update interactor types of " + resultUpdate + " existing interactors.");

                        manager.fireOnRemappedObsolete(evt);
                    }
                    else if (term instanceof CvLifecycleEvent && termFromDb instanceof CvLifecycleEvent){
                        Query query = factory.getEntityManager().createQuery("update LifeCycleEvent l set l.event = :event" +
                                " where l.event = :duplicate");
                        query.setParameter("event", termFromDb);
                        query.setParameter("duplicate", term);
                        resultUpdate = query.executeUpdate();

                        // fire event
                        ObsoleteRemappedEvent evt = new ObsoleteRemappedEvent(this, updateContext.getIdentifier(), ontologyTerm.getRemappedTerm(), term.getAc(), termFromDb.getAc(), resultUpdate, "We could update life cycle event types of " + resultUpdate + " existing life cycle events.");

                        manager.fireOnRemappedObsolete(evt);
                    }
                    else if (term instanceof CvParameterType && termFromDb instanceof CvParameterType){
                        Query query = factory.getEntityManager().createQuery("update ComponentParameter c set c.cvParameterType = :type" +
                                " where c.cvParameterType = :duplicate");
                        query.setParameter("type", termFromDb);
                        query.setParameter("duplicate", term);
                        resultUpdate = query.executeUpdate();

                        Query query2 = factory.getEntityManager().createQuery("update InteractionParameter i set i.cvParameterType = :type" +
                                " where i.cvParameterType = :duplicate");
                        query2.setParameter("type", termFromDb);
                        query2.setParameter("duplicate", term);
                        resultUpdate += query2.executeUpdate();

                        // fire event
                        ObsoleteRemappedEvent evt = new ObsoleteRemappedEvent(this, updateContext.getIdentifier(), ontologyTerm.getRemappedTerm(), term.getAc(), termFromDb.getAc(), resultUpdate, "We could update parameter types of " + resultUpdate + " existing parameters.");

                        manager.fireOnRemappedObsolete(evt);
                    }
                    else if (term instanceof CvParameterUnit && termFromDb instanceof CvParameterUnit){
                        Query query = factory.getEntityManager().createQuery("update ComponentParameter c set c.cvParameterUnit = :unit" +
                                " where c.cvParameterUnit = :duplicate");
                        query.setParameter("unit", termFromDb);
                        query.setParameter("duplicate", term);
                        resultUpdate = query.executeUpdate();

                        Query query2 = factory.getEntityManager().createQuery("update InteractionParameter i set i.cvParameterUnit = :unit" +
                                " where i.cvParameterUnit = :duplicate");
                        query2.setParameter("unit", termFromDb);
                        query2.setParameter("duplicate", term);
                        resultUpdate += query2.executeUpdate();

                        // fire event
                        ObsoleteRemappedEvent evt = new ObsoleteRemappedEvent(this, updateContext.getIdentifier(), ontologyTerm.getRemappedTerm(), term.getAc(), termFromDb.getAc(), resultUpdate, "We could update parameter units of " + resultUpdate + " existing parameters.");

                        manager.fireOnRemappedObsolete(evt);
                    }
                    else if (term instanceof CvPublicationStatus && termFromDb instanceof CvPublicationStatus){
                        Query query = factory.getEntityManager().createQuery("update Publication p set p.status = :status" +
                                " where p.status = :duplicate");
                        query.setParameter("status", termFromDb);
                        query.setParameter("duplicate", term);
                        resultUpdate = query.executeUpdate();

                        // fire event
                        ObsoleteRemappedEvent evt = new ObsoleteRemappedEvent(this, updateContext.getIdentifier(), ontologyTerm.getRemappedTerm(), term.getAc(), termFromDb.getAc(), resultUpdate, "We could update status of " + resultUpdate + " existing publications.");

                        manager.fireOnRemappedObsolete(evt);
                    }
                    else if (term instanceof CvTopic && termFromDb instanceof CvTopic){
                        Query query = factory.getEntityManager().createQuery("update Annotation a set a.cvTopic = :topic" +
                                " where a.cvTopic = :duplicate");
                        query.setParameter("topic", termFromDb);
                        query.setParameter("duplicate", term);
                        resultUpdate = query.executeUpdate();

                        // fire event
                        ObsoleteRemappedEvent evt = new ObsoleteRemappedEvent(this, updateContext.getIdentifier(), ontologyTerm.getRemappedTerm(), term.getAc(), termFromDb.getAc(), resultUpdate, "We could update topicsof " + resultUpdate + " existing annotations.");

                        manager.fireOnRemappedObsolete(evt);
                    }
                    else if (term instanceof CvXrefQualifier && termFromDb instanceof CvXrefQualifier){
                        Query query = factory.getEntityManager().createQuery("update Xref x set x.cvXrefQualifier = :qualifier" +
                                " where x.cvXrefQualifier = :duplicate");
                        query.setParameter("qualifier", termFromDb);
                        query.setParameter("duplicate", term);
                        resultUpdate = query.executeUpdate();

                        // fire event
                        ObsoleteRemappedEvent evt = new ObsoleteRemappedEvent(this, updateContext.getIdentifier(), ontologyTerm.getRemappedTerm(), term.getAc(), termFromDb.getAc(), resultUpdate, "We could update qualifiers of " + resultUpdate + " existing xrefs.");

                        manager.fireOnRemappedObsolete(evt);
                    }
                    else {
                        // do something
                        couldRemap = false;

                        // fire event
                        CvUpdateError error = manager.getErrorFactory().createCvUpdateError(UpdateError.cv_impossible_merge, "We cannot merge cvs of type " + term.getClass().getCanonicalName(), updateContext.getIdentifier(), term.getAc(), term.getShortLabel());
                        UpdateErrorEvent evt = new UpdateErrorEvent(this, error);

                        manager.fireOnUpdateError(evt);
                    }
                }
            }
        }
        // no obvious term to remap to, log this term as impossible to remap
        else {
            couldRemap = false;

            // fire an event
            CvUpdateManager manager = updateContext.getManager();
            ObsoleteTermImpossibleToRemapEvent evt = new ObsoleteTermImpossibleToRemapEvent(this, identifier, term.getAc(), term.getShortLabel(), "There was no obvious remapped term in the ontology for this obsolete term. Need to be remapped manually");
            evt.getPossibleTerms().addAll(ontologyTerm.getPossibleTermsToRemapTo());

            manager.fireOnObsoleteImpossibleToRemap(evt);
        }
    }

    private void createIdentityXrefIfPossible(CvUpdateContext updateContext, IntactOntologyTermI ontologyTerm, CvDagObject term, String remappedDb) {
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        // the remapping update was successful
        if (remappedDb != null){
            // create identity xref
            CvObjectXref cvXref = CvUpdateUtils.createIdentityXref(term, remappedDb, ontologyTerm.getRemappedTerm());
            factory.getXrefDao(CvObjectXref.class).persist(cvXref);

            updateContext.setIdentityXref(cvXref);
        }
        // impossible to create an identity xref but the remapping did happen (identifier was updated)
        else {

            updateContext.setIdentityXref(null);

            CvUpdateManager manager = updateContext.getManager();

            CvUpdateError error = manager.getErrorFactory().createCvUpdateError(UpdateError.ontology_database_no_found, ontologyTerm.getRemappedTerm() + " cannot be remapped to a database and so no identity xref could be created.", ontologyTerm.getRemappedTerm(), term.getAc(), term.getShortLabel());
            manager.fireOnUpdateError(new UpdateErrorEvent(this, error));
        }
    }

    public Map<String, String> getOntologyIdToDatabase() {
        return ontologyIdToDatabase;
    }

    public Map<String, Set<CvDagObject>> getRemappedCvToUpdate() {
        return remappedCvToUpdate;
    }

    public void clear(){
        this.remappedCvToUpdate.clear();
    }
}
