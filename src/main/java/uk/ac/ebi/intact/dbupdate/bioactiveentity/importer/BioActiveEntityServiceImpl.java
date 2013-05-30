package uk.ac.ebi.intact.dbupdate.bioactiveentity.importer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.chebi.webapps.chebiWS.client.ChebiWebServiceClient;
import uk.ac.ebi.chebi.webapps.chebiWS.model.ChebiWebServiceFault_Exception;
import uk.ac.ebi.chebi.webapps.chebiWS.model.DataItem;
import uk.ac.ebi.chebi.webapps.chebiWS.model.Entity;
import uk.ac.ebi.intact.dbupdate.bioactiveentity.utils.BioActiveEntityUtils;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.AnnotatedObjectUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 22/05/2013
 * Time: 17:14
 * To change this template use File | Settings | File Templates.
 */
public class BioActiveEntityServiceImpl implements BioActiveEntityService {

    /**
     * Sets up a logger for that class.
     */
    public static final Log log = LogFactory.getLog(BioActiveEntityServiceImpl.class);

    private ChebiWebServiceClient chebiClient;
    private final Institution owner = BioActiveEntityUtils.getInstitution();
    private final CvDatabase chebi = BioActiveEntityUtils.getChEBIDatabase();

    private static final int MAX_SIZE_CHEBI_IDS = 50;

    public BioActiveEntityServiceImpl() {
        this.chebiClient = new ChebiWebServiceClient();
    }

    public BioActiveEntityServiceImpl(ChebiWebServiceClient chebiClient) {
        if (chebiClient != null) {
            this.chebiClient = chebiClient;
        } else {
            log.warn("No Chebi Web Service is given, a default Service will be used");
            this.chebiClient = new ChebiWebServiceClient();
        }
    }

    @Override
    public SmallMolecule getBioEntityByChebiId(String chebiId) throws BioActiveEntityServiceException {

        SmallMolecule bioActiveEntity = null;
        Entity entity = null;

        if (chebiId == null || chebiId.isEmpty()) {
            throw new BioActiveEntityServiceException("The ChEBI Id can not be null or empty");
        }

        try {
            entity = chebiClient.getCompleteEntity(chebiId);

            if (entity == null) {
                throw new BioActiveEntityServiceException("Unknown ChEBI Web Service Fault with the ID: "
                        + chebiId + " the molecule can not be retrieve");
            }

            bioActiveEntity = processEntity(entity);


        } catch (ChebiWebServiceFault_Exception e) {
            throw new BioActiveEntityServiceException("Unknown ChEBI Web Service Fault with the ID: "
                    + chebiId + ". Error message: " + e.getFaultInfo().getMessage(),e);
        }
        finally {
            entity = null;
        }

        return bioActiveEntity;
    }


    @Override
    public List<SmallMolecule> getBioEntityByChebiIdList(List<String> chebiIds) throws BioActiveEntityServiceException {

        List<SmallMolecule> bioActiveEntities = null;
        List<Entity> entities = null;

        if (chebiIds == null || chebiIds.isEmpty()) {
            throw new BioActiveEntityServiceException("The ChEBI Id can not be null or empty");
        }

        List<List<String>> parts = BioActiveEntityUtils.splitter(chebiIds, MAX_SIZE_CHEBI_IDS);
        bioActiveEntities = new ArrayList<SmallMolecule>();

        for (List<String> part : parts) {
            try {
                //If we have the same Id in the list, we will have only one Entity
                entities = chebiClient.getCompleteEntityByList(part);

                if (entities == null || entities.isEmpty()) {
                    throw new BioActiveEntityServiceException("Unknown ChEBI Web Service Fault with the IDs: "
                            + chebiIds + " the molecules can not be retrieve");
                }

                for (Entity entity : entities) {
                    SmallMolecule smallMolecule = processEntity(entity);
                    if(!bioActiveEntities.contains(smallMolecule)){
                        bioActiveEntities.add(smallMolecule);
                    }
                }

            } catch (ChebiWebServiceFault_Exception e) {
                throw new BioActiveEntityServiceException("Unknown ChEBI Web Service Fault with the IDs: " + chebiIds
                        + ". Error message: "
                        + e.getFaultInfo().getMessage(), e);
            }
            entities.clear();
        }

        return bioActiveEntities;
    }

    private SmallMolecule processEntity(Entity entity) {

        SmallMolecule bioActiveEntity = null;

        log.debug("Importing " + entity.getChebiId() + " with name: " + entity.getChebiAsciiName());

        //Basic BioActive entity
        bioActiveEntity = new SmallMoleculeImpl(
                AnnotatedObjectUtils.prepareShortLabel(entity.getChebiAsciiName()),
                owner,
                BioActiveEntityUtils.getSmallMoleculeType()
        );

        //Full Name
        bioActiveEntity.setFullName(entity.getChebiAsciiName());

        //Xrefs
        Collection<InteractorXref> xrefs = createXRefs(entity);
        if (xrefs != null && !xrefs.isEmpty()) {
            bioActiveEntity.setXrefs(xrefs);
        }

        //Aliases
        Collection<InteractorAlias> aliases = createAliases(entity, bioActiveEntity);
        if (aliases != null && !aliases.isEmpty()) {
            bioActiveEntity.setAliases(aliases);
        }

        //Annotations
        Collection<Annotation> annotations = createAnnotations(entity);
        if (annotations != null && !annotations.isEmpty()) {
            bioActiveEntity.setAnnotations(annotations);
        }

        return bioActiveEntity;
    }

    private Collection<InteractorXref> createXRefs(Entity entity) {

        Collection<InteractorXref> interactorXrefs = new ArrayList<InteractorXref>();

        CvXrefQualifier identityQual = BioActiveEntityUtils.getPrimaryIDQualifier();
        CvXrefQualifier secondaryIdQual = BioActiveEntityUtils.getSecondaryIDQualifier();

        //Primary ID (Identity)
        //If the entity exits the primary id too
        interactorXrefs.add(new InteractorXref(owner, chebi, entity.getChebiId(), null, null, identityQual));

        //Secondary IDs (Secondary identifiers)
        if (entity.getSecondaryChEBIIds() != null && !entity.getSecondaryChEBIIds().isEmpty()) {
            log.debug("Found " + entity.getSecondaryChEBIIds().size() + " secondary IDs");
            for (String secondaryId : entity.getSecondaryChEBIIds()) {
                interactorXrefs.add(new InteractorXref(owner, chebi, secondaryId, null, null, secondaryIdQual));
            }
        }

        return interactorXrefs;
    }

    private Collection<InteractorAlias> createAliases(Entity entity, SmallMolecule bioActiveEntity) {

        Collection<InteractorAlias> aliases = new ArrayList<InteractorAlias>();

        CvAliasType synonymAliasType = BioActiveEntityUtils.getSynonymAliasType();
        CvAliasType iupacNameAliasType = BioActiveEntityUtils.getIupacAliasType();


        //Chebi Synonyms
        if (entity.getSynonyms() != null && !entity.getSynonyms().isEmpty()) {
            log.debug("Found " + entity.getSynonyms().size() + " synonyms");
            for (DataItem synonym : entity.getSynonyms()) {
                aliases.add(new InteractorAlias(owner, bioActiveEntity, synonymAliasType, synonym.getData()));
            }
        }

        //IUPAC Names
        if (entity.getIupacNames() != null && !entity.getIupacNames().isEmpty()) {
            log.debug("Found " + entity.getIupacNames().size() + " IUPAC names");
            for (DataItem iupacName : entity.getIupacNames()) {
                aliases.add(new InteractorAlias(owner, bioActiveEntity, iupacNameAliasType, iupacName.getData()));
            }
        }

        return aliases;
    }

    private Collection<Annotation> createAnnotations(Entity entity) {

        Collection<Annotation> annotations = new ArrayList<Annotation>();

        CvTopic inchiType = BioActiveEntityUtils.getInchiType();
        CvTopic inchiKeyType = BioActiveEntityUtils.getInchiKeyType();
        CvTopic smilesType = BioActiveEntityUtils.getSmilesType();

        //InChI
        if (entity.getInchi() != null && !entity.getInchi().isEmpty()) {
            annotations.add(new Annotation(inchiType, entity.getInchi()));
        }

        //Standard InChKey
        if (entity.getInchiKey() != null && !entity.getInchiKey().isEmpty()) {
            annotations.add(new Annotation(inchiKeyType, entity.getInchiKey()));
        }

        //SMILES
        if (entity.getSmiles() != null && !entity.getSmiles().isEmpty()) {
            annotations.add(new Annotation(smilesType, entity.getSmiles()));
        }

        return annotations;
    }

}
