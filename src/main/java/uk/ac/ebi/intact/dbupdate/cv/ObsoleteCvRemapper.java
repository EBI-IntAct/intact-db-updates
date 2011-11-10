package uk.ac.ebi.intact.dbupdate.cv;

import psidev.psi.tools.ontology_manager.interfaces.OntologyAccessTemplate;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyTermI;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.XrefUtils;

import javax.persistence.Query;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class will try to remap an obsolete term
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>10/11/11</pre>
 */

public class ObsoleteCvRemapper {

    private Map<String, String> ontologyIdToDatabase;

    public ObsoleteCvRemapper() throws IOException {
        ontologyIdToDatabase = new HashMap<String, String>();
    }

    private void initializeOntologyIDToDatabase(){
        ontologyIdToDatabase.put("MI", CvDatabase.PSI_MI_MI_REF);
        ontologyIdToDatabase.put("MOD", CvDatabase.PSI_MOD_MI_REF);
    }

    public boolean remapObsoleteCvTerm(CvDagObject term, IntactOntologyTermI ontologyTerm,
                                    OntologyAccessTemplate<IntactOntologyTermI> ontologyAccess, String ontologyID, DaoFactory factory){
        String database = ontologyIdToDatabase.get(ontologyID);
        boolean couldRemap = false;

        if (database == null){
            throw new IllegalArgumentException("The cv object " + term.getShortLabel() + " cannot be remapped because ontologyId is not recognised " + ontologyID);
        }

        if (ontologyTerm.getRemappedTerm() != null){
            CvDagObject termFromDb = factory.getCvObjectDao(CvDagObject.class).getByIdentifier(ontologyTerm.getRemappedTerm());

            if (termFromDb == null){
                couldRemap = true;
                term.setIdentifier(ontologyTerm.getRemappedTerm());

                Collection<CvObjectXref> identities = XrefUtils.getIdentityXrefs(term);

                if (identities.isEmpty()){
                    CvXrefQualifier identity = factory.getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.IDENTITY_MI_REF);
                    String remappedDb = null;

                    if (ontologyTerm.getRemappedTerm().startsWith(ontologyID)){
                        remappedDb = this.ontologyIdToDatabase.get(ontologyID);
                    }
                    else if (ontologyTerm.getRemappedTerm().contains(":")) {
                        String[] refInfo = ontologyTerm.getRemappedTerm().split(":");

                        String newOntologyId = refInfo[0];
                        remappedDb = this.ontologyIdToDatabase.get(newOntologyId);
                    }

                    if (remappedDb != null){
                        CvDatabase remappedCvDb = factory.getCvObjectDao(CvDatabase.class).getByPsiMiRef(remappedDb);

                        // create identity xref
                        CvObjectXref cvXref = XrefUtils.createIdentityXref(term, ontologyTerm.getRemappedTerm(), identity, remappedCvDb);
                        factory.getXrefDao(CvObjectXref.class).persist(cvXref);

                        term.addXref(cvXref);
                    }
                }
                else {
                    for (CvObjectXref ref : identities){
                        if (ontologyTerm.getTermAccession().equalsIgnoreCase(ref.getPrimaryId())){

                            if (ontologyTerm.getRemappedTerm().startsWith(ontologyID)){
                                ref.setPrimaryId(ontologyTerm.getRemappedTerm());
                                factory.getXrefDao(CvObjectXref.class).update(ref);
                            }
                            else if (ontologyTerm.getRemappedTerm().contains(":")) {
                                String[] refInfo = ontologyTerm.getRemappedTerm().split(":");

                                String newOntologyId = refInfo[0];
                                String remappedDb = this.ontologyIdToDatabase.get(newOntologyId);

                                if (remappedDb != null){
                                    CvDatabase remappedCvDb = factory.getCvObjectDao(CvDatabase.class).getByPsiMiRef(remappedDb);

                                    ref.setCvDatabase(remappedCvDb);
                                    ref.setPrimaryId(ontologyTerm.getRemappedTerm());
                                    factory.getXrefDao(CvObjectXref.class).update(ref);
                                }
                            }
                        }
                    }
                }
            }
            // merge current term with new term
            else {
                int resultUpdate = 0;
                boolean canDelete = true;

                if (term instanceof CvAliasType && termFromDb instanceof CvAliasType){
                    Query query = factory.getEntityManager().createQuery("update Alias a set a.cvAliasType = :type" +
                            " where a.cvAliasType = :duplicate");
                    query.setParameter("type", termFromDb);
                    query.setParameter("duplicate", term);
                    resultUpdate = query.executeUpdate();
                }
                else if (term instanceof CvBiologicalRole && termFromDb instanceof CvBiologicalRole){
                    Query query = factory.getEntityManager().createQuery("update Component c set c.cvBiologicalRole = :role" +
                            " where c.cvBiologicalRole = :duplicate");
                    query.setParameter("role", termFromDb);
                    query.setParameter("duplicate", term);
                    resultUpdate = query.executeUpdate();
                }
                else if (term instanceof CvCellType && termFromDb instanceof CvCellType){
                    Query query = factory.getEntityManager().createQuery("update BioSource b set b.cvCellType = :type" +
                            " where b.cvCellType = :duplicate");
                    query.setParameter("type", termFromDb);
                    query.setParameter("duplicate", term);
                    resultUpdate = query.executeUpdate();
                }
                else if (term instanceof CvCellCycle && termFromDb instanceof CvCellCycle){
                    Query query = factory.getEntityManager().createQuery("update BioSource b set b.cvCellCycle = :cycle" +
                            " where b.cvCellCycle = :duplicate");
                    query.setParameter("cycle", termFromDb);
                    query.setParameter("duplicate", term);
                    resultUpdate = query.executeUpdate();
                }
                else if (term instanceof CvTissue && termFromDb instanceof CvTissue){
                    Query query = factory.getEntityManager().createQuery("update BioSource b set b.cvTissue = :tissue" +
                            " where b.cvTissue = :duplicate");
                    query.setParameter("tissue", termFromDb);
                    query.setParameter("duplicate", term);
                    resultUpdate = query.executeUpdate();
                }
                else if (term instanceof CvComponentRole && termFromDb instanceof CvComponentRole){
                    Query query = factory.getEntityManager().createQuery("update Component c set c.cvComponentRole = :role" +
                            " where c.cvComponentRole = :duplicate");
                    query.setParameter("role", termFromDb);
                    query.setParameter("duplicate", term);
                    resultUpdate = query.executeUpdate();
                }
                else if (term instanceof CvCompartment && termFromDb instanceof CvCompartment){
                    Query query = factory.getEntityManager().createQuery("update BioSource b set b.cvCompartment = :compartment" +
                            " where b.cvCompartment = :duplicate");
                    query.setParameter("compartment", termFromDb);
                    query.setParameter("duplicate", term);
                    resultUpdate = query.executeUpdate();
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
                }
                else if (term instanceof CvDatabase && termFromDb instanceof CvDatabase){
                    Query query = factory.getEntityManager().createQuery("update Xref x set x.cvDatabase = :db" +
                            " where x.cvDatabase = :duplicate");
                    query.setParameter("db", termFromDb);
                    query.setParameter("duplicate", term);
                    resultUpdate = query.executeUpdate();
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
                    }
                }
                else if (term instanceof CvFeatureType && termFromDb instanceof CvFeatureType){
                    Query query = factory.getEntityManager().createQuery("update Feature f set f.cvFeatureType = :type" +
                            " where f.cvFeatureType = :duplicate");
                    query.setParameter("type", termFromDb);
                    query.setParameter("duplicate", term);
                    resultUpdate = query.executeUpdate();
                }
                else if (term instanceof CvFeatureIdentification && termFromDb instanceof CvFeatureIdentification){
                    Query query = factory.getEntityManager().createQuery("update Feature f set f.cvFeatureIdentification = :type" +
                            " where f.cvFeatureIdentification = :duplicate");
                    query.setParameter("type", termFromDb);
                    query.setParameter("duplicate", term);
                    resultUpdate = query.executeUpdate();
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
                    }
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
                }
                else if (term instanceof CvInteractionType && termFromDb instanceof CvInteractionType){
                    Query query = factory.getEntityManager().createQuery("update InteractionImpl i set i.cvInteractionType = :type" +
                            " where i.cvInteractionType = :duplicate");
                    query.setParameter("type", termFromDb);
                    query.setParameter("duplicate", term);
                    resultUpdate = query.executeUpdate();
                }
                else if (term instanceof CvInteractorType && termFromDb instanceof CvInteractorType){
                    Query query = factory.getEntityManager().createQuery("update InteractorImpl i set i.cvInteractorType = :type" +
                            " where i.cvInteractorType = :duplicate");
                    query.setParameter("type", termFromDb);
                    query.setParameter("duplicate", term);
                    resultUpdate = query.executeUpdate();
                }
                else if (term instanceof CvLifecycleEvent && termFromDb instanceof CvLifecycleEvent){
                    Query query = factory.getEntityManager().createQuery("update LifeCycleEvent l set l.event = :event" +
                            " where l.event = :duplicate");
                    query.setParameter("event", termFromDb);
                    query.setParameter("duplicate", term);
                    resultUpdate = query.executeUpdate();
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
                }
                else if (term instanceof CvPublicationStatus && termFromDb instanceof CvPublicationStatus){
                    Query query = factory.getEntityManager().createQuery("update Publication p set p.status = :status" +
                            " where p.status = :duplicate");
                    query.setParameter("status", termFromDb);
                    query.setParameter("duplicate", term);
                    resultUpdate = query.executeUpdate();
                }
                else if (term instanceof CvTopic && termFromDb instanceof CvTopic){
                    Query query = factory.getEntityManager().createQuery("update Annotation a set a.cvTopic = :topic" +
                            " where a.cvTopic = :duplicate");
                    query.setParameter("topic", termFromDb);
                    query.setParameter("duplicate", term);
                    resultUpdate = query.executeUpdate();
                }
                else if (term instanceof CvXrefQualifier && termFromDb instanceof CvXrefQualifier){
                    Query query = factory.getEntityManager().createQuery("update Xref x set x.cvXrefQualifier = :qualifier" +
                            " where x.cvXrefQualifier = :duplicate");
                    query.setParameter("qualifier", termFromDb);
                    query.setParameter("duplicate", term);
                    resultUpdate = query.executeUpdate();
                }
                else {
                    // do something
                    canDelete = false;
                }

                if (canDelete){
                    couldRemap = true;
                    factory.getCvObjectDao(CvDagObject.class).delete(term);
                }
            }
        }

        return couldRemap;
    }

    public Map<String, String> getOntologyIdToDatabase() {
        return ontologyIdToDatabase;
    }
}
