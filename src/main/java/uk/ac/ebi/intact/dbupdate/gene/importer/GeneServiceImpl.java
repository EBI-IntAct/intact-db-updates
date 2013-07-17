package uk.ac.ebi.intact.dbupdate.gene.importer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.dbupdate.gene.utils.GeneUtils;
import uk.ac.ebi.intact.dbupdate.gene.utils.ParameterNameValue;
import uk.ac.ebi.intact.dbupdate.gene.utils.UniProtRestQuery;
import uk.ac.ebi.intact.dbupdate.gene.utils.UniProtResult;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.AnnotatedObjectUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 11/07/2013
 * Time: 14:25
 * To change this template use File | Settings | File Templates.
 */
/*
 * This class retrieve only metadata for a ensembl identifier in uniprot.
 * This is because of the shortlabel should be generated from the entry name in uniprot
 */
public class GeneServiceImpl implements GeneService {

    /**
     * Sets up a logger for that class.
     */
    public static final Log log = LogFactory.getLog(GeneServiceImpl.class);

    private final Institution owner = GeneUtils.getInstitution();
    private final CvDatabase ensembl = GeneUtils.getEnsemblDatabase();


    public GeneServiceImpl() {
    }

    @Override
    public List<Interactor> getGeneByEnsemblIdInSwissprot(String ensemblId) throws GeneServiceException {
        return getGeneByEnsemblId(ensemblId, " + reviewed:yes");
    }

    @Override
    public List<Interactor> getGeneByEnsemblIdInUniprot(String ensemblId) throws GeneServiceException {
        return getGeneByEnsemblId(ensemblId, "");
    }

    private List<Interactor> getGeneByEnsemblId(String ensemblId, String queryFilter) throws GeneServiceException{
        List<Interactor> genes = null;

        if (ensemblId == null || ensemblId.isEmpty()) {
            throw new GeneServiceException("The Ensembl Id can not be null or empty");
        }

        ParameterNameValue[] parameters = null;

        try {

            parameters = new ParameterNameValue[]
                    {
                            new ParameterNameValue("query", ensemblId + queryFilter),
                            new ParameterNameValue("columns",
                                    "entry name,genes,organism,organism-id,id,reviewed,protein names"),
                            new ParameterNameValue("reviewed", "yes"), //By default only the swissprot entries (curators decision)
                            new ParameterNameValue("format", "tab")
                    };

            List<UniProtResult> list = UniProtRestQuery.queryUniProt("uniprot", parameters);
            genes = new ArrayList<Interactor>(list.size());

            for(UniProtResult entity:list){
                genes.add(processEntity(entity,ensemblId));
            }

        } catch (UnsupportedEncodingException e) {
            throw new GeneServiceException("The parameters " + Arrays.toString(parameters) + " for the uniprot query can not be encoded: ", e);
        }

        return genes;
    }

    private Interactor processEntity(UniProtResult entity, String ensemblId) {

        Interactor gene = null;

        log.debug("Importing " + ensemblId + " with name: " + entity.getEntryName() + "_GENE");

        //Basic BioActive entity
        gene = new InteractorImpl(AnnotatedObjectUtils.prepareShortLabel(GeneUtils.generateShortLabel(entity.getEntryName())),
                owner,
                GeneUtils.getGeneType()
        );

        //Full Name
        gene.setFullName(entity.getRecommendedName());

        //BioSource
        gene.setBioSource(new BioSource(entity.getOrganism(), entity.getOrganismId()));

        //Xrefs
        Collection<InteractorXref> xrefs = createXRefs(entity, ensemblId);
        if (xrefs != null && !xrefs.isEmpty()) {
            gene.setXrefs(xrefs);
        }

        //Aliases
        Collection<InteractorAlias> aliases = createAliases(entity, gene);
        if (aliases != null && !aliases.isEmpty()) {
            gene.setAliases(aliases);
        }

        return gene;

    }

    private Collection<InteractorXref> createXRefs(UniProtResult entity, String ensemblId) {

        Collection<InteractorXref> interactorXrefs = new ArrayList<InteractorXref>();

        CvXrefQualifier identityQual = GeneUtils.getPrimaryIDQualifier();

        //Primary ID (Identity)
        //If the entity exits the primary id too
        interactorXrefs.add(new InteractorXref(owner, ensembl, ensemblId, null, null, identityQual));

        //We do not have any secondary IDs (Secondary identifiers)

        return interactorXrefs;
    }

    private Collection<InteractorAlias> createAliases(UniProtResult entity, Interactor gene) {

        Collection<InteractorAlias> aliases = new ArrayList<InteractorAlias>();

        CvAliasType synonymAliasType = GeneUtils.getSynonymAliasType();


        //Gene Synonyms
        if (entity.getSynonyms() != null && !entity.getSynonyms().isEmpty()) {
            log.debug("Found " + entity.getSynonyms().size() + " synonyms");
            for (String synonym : entity.getSynonyms()) {
                aliases.add(new InteractorAlias(owner, gene, synonymAliasType, synonym));
            }
        }

        return aliases;
    }
}
