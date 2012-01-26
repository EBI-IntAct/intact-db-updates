package uk.ac.ebi.intact.dbupdate.cv;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyAccess;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyTermI;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.CvObjectDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.cv.errors.CvUpdateError;
import uk.ac.ebi.intact.dbupdate.cv.errors.UpdateError;
import uk.ac.ebi.intact.dbupdate.cv.events.UpdateErrorEvent;
import uk.ac.ebi.intact.model.CvDagObject;

import java.io.IOException;
import java.util.*;

/**
 * This class allows to run global cv update for MI and MOD ontology using the cvUpdateManager
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>25/01/12</pre>
 */

public class GlobalCvUpdateRunner {
    private static final Log log = LogFactory.getLog(GlobalCvUpdateRunner.class);

    private CvUpdateManager cvUpdateManager;

    private Set<String> processedIntactAcs = new HashSet<String>();

    public GlobalCvUpdateRunner(){
    }

    public CvUpdateManager getCvUpdateManager() {
        return cvUpdateManager;
    }

    /**
     * Update all MI and MOD terms.
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public void updateAll(){
        cvUpdateManager.clear();
        clearProcessedIntactAcs();

        log.info("Update and create all terms for PSI-MI ontology");
        updateAndCreateAllTerms("MI");

        log.info("Update all existing terms for PSI-MOD ontology");
        updateAllTerms("MOD");
    }

    /**
     * Update and create all the terms of a specific ontology
     * - update existing terms
     * - create missing parents
     * - create terms from the ontology which are not yet in IntAct
     * - update terms from other ontology if some obsolete terms have been remapped to them
     * - check for duplicates
     * @param ontologyId
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public void updateAndCreateAllTerms(String ontologyId) {
        try {
            cvUpdateManager.registerBasicListeners();

            cvUpdateManager.clear();

            // get the ontologyAcces for this ontology id
            IntactOntologyAccess ontologyAccess = cvUpdateManager.getIntactOntologyManager().getOntologyAccess(ontologyId);

            if (ontologyAccess == null){
                throw new IllegalArgumentException("Cannot update terms of ontology " + ontologyId + ". The ontologies possible to update are in the configuration file (/resources/ontologies.xml)");
            }

            // update existing terms and remap obsolete terms if possible
            log.info("Update existing terms...");
            updateExistingTerms(ontologyId);

            // update all remapped terms to other ontologies
            log.info("Update obsolete terms remapped to an other ontology...");
            updateTermsRemappedToOtherOntologies();

            // create missing parents reported while updating existing cvs
            log.info("Create missing parents...");
            createMissingParents();

            // create missing terms in ontology which have not already been processed
            log.info("Create missing terms...");
            createMissingTerms(ontologyId);

            // check if duplicated terms exist
            log.info("Check duplicated terms...");
            cvUpdateManager.checkDuplicatedCvTerms(ontologyAccess);

        } catch (IOException e) {
            log.fatal("Impossible to run the update because we cannot create the reports directory", e);
        }
    }

    /**
     * This method will update all the terms of a given ontology :
     * - update existing terms
     * - create missing parents
     * - update terms from other ontology if some obsolete terms have been remapped to them
     * - check for duplicates
     * @param ontologyId
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public void updateAllTerms(String ontologyId) {
        try {
            cvUpdateManager.registerBasicListeners();

            cvUpdateManager.clear();

            IntactOntologyAccess ontologyAccess = cvUpdateManager.getIntactOntologyManager().getOntologyAccess(ontologyId);

            if (ontologyAccess == null){
                throw new IllegalArgumentException("Cannot update terms of ontology " + ontologyId + ". The ontologies possible to update are in the configuration file (/resources/ontologies.xml)");
            }

            // update existing terms
            log.info("Update existing terms...");
            updateExistingTerms(ontologyId);

            // update all remapped terms to other ontologies
            log.info("Update obsolete terms remapped to an other ontology...");
            updateTermsRemappedToOtherOntologies();

            // create missing parents
            log.info("Create missing parents...");
            createMissingParents();

            // check if duplicated terms exist
            log.info("Check duplicated terms...");
            cvUpdateManager.checkDuplicatedCvTerms(ontologyAccess);

        } catch (IOException e) {
            log.fatal("Impossible to run the update because we cannot create the reports directory", e);
        }
    }

    /**
     * Updates all existing cvs of a given ontology.
     */
    private void updateExistingTerms(String ontologyId){

        List<String> cvObjectAcs = cvUpdateManager.getValidCvObjects(ontologyId);

        for (String validCv : cvObjectAcs){

            if (!processedIntactAcs.contains(validCv)){
                try {
                    cvUpdateManager.updateCv(validCv, ontologyId);

                    processedIntactAcs.add(validCv);
                } catch (Exception e) {
                    log.error("Impossible to update the cv " + validCv, e);
                    CvUpdateError error = cvUpdateManager.getErrorFactory().createCvUpdateError(UpdateError.fatal, "Impossible to update this cv", null, validCv, null);

                    UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
                    cvUpdateManager.fireOnUpdateError(evt);
                }
            }
        }
    }

    /**
     * Updates all the terms which were obsolete and have been remapped to another ontology
     */
    private void updateTermsRemappedToOtherOntologies() {

        Map<String, Set<CvDagObject>> remappedTermsToUpdate = cvUpdateManager.getRemappedObsoleteTermsToUpdate();

        if (!remappedTermsToUpdate.isEmpty()){
            for (Map.Entry<String, Set<CvDagObject>> entry : remappedTermsToUpdate.entrySet()){

                for (CvDagObject cvObject : entry.getValue()){

                    log.info("Update remapped term " + cvObject.getAc() + ", label = " + cvObject.getShortLabel() + ", identifier = " + cvObject.getIdentifier());
                    try {
                        cvUpdateManager.updateCv(cvObject.getAc(), entry.getKey());

                    } catch (Exception e) {
                        CvUpdateError error = cvUpdateManager.getErrorFactory().createCvUpdateError(UpdateError.fatal, "Impossible to update the cv ", cvObject.getIdentifier(), cvObject.getAc(), cvObject.getShortLabel());

                        UpdateErrorEvent evt = new UpdateErrorEvent(this, error);
                        cvUpdateManager.fireOnUpdateError(evt);
                    }

                    processedIntactAcs.add(cvObject.getAc()) ;
                }
            }
        }
    }

    /**
     * Create missing parents and update reported children
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private void createMissingParents() {
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        CvObjectDao<CvDagObject> cvDao = factory.getCvObjectDao(CvDagObject.class);

        Map<String, Set<CvDagObject>> missingParentsToCreate = cvUpdateManager.getMissingParentsToCreate();

        if (!missingParentsToCreate.isEmpty()){
            // import missing parents and update children
            for (Map.Entry<String, Set<CvDagObject>> missing : missingParentsToCreate.entrySet()){

                cvUpdateManager.createMissingParentsFor(cvDao, missing.getKey(), missing.getValue());
            }
        }
    }

    /**
     * Create all the terms of a given ontology which do not exist in IntAct
     * @param ontologyId
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private void createMissingTerms(String ontologyId) {

        IntactOntologyAccess ontologyAccess = cvUpdateManager.getIntactOntologyAccessFor(ontologyId);

        if (ontologyAccess != null){

            Collection<IntactOntologyTermI> rootTerms = ontologyAccess.getRootTerms();

            // import root terms if not obsolete and has children
            for (IntactOntologyTermI root : rootTerms){
                Collection<IntactOntologyTermI> children = ontologyAccess.getDirectChildren(root);

                if (!children.isEmpty()){
                    cvUpdateManager.importNonObsoleteRootAndChildren(ontologyAccess, children);
                }
            }

            CvObjectDao<CvDagObject> cvDao = IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvDagObject.class);

            Map<String, Set<CvDagObject>> missingTermsFromOtherOntology = cvUpdateManager.getTermsFromOtherOntologiesToCreate();

            // update missing parents from other ontologies
            for (Map.Entry<String, Set<CvDagObject>> entry : missingTermsFromOtherOntology.entrySet()){
                cvUpdateManager.createMissingParentsFor(cvDao, entry.getKey(), entry.getValue());
            }
        }
        else {
            throw new IllegalArgumentException("Cannot update terms of ontology " + ontologyId + ". The ontologies possible to update are in the configuration file (/resources/ontologies.xml)");
        }
    }

    public void clearProcessedIntactAcs(){
        this.processedIntactAcs.clear();
    }

    public void setCvUpdateManager(CvUpdateManager cvUpdateManager) {
        this.cvUpdateManager = cvUpdateManager;
    }
}
