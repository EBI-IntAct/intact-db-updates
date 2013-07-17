package uk.ac.ebi.intact.dbupdate.gene.importer;

import uk.ac.ebi.intact.model.Interactor;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 11/07/2013
 * Time: 14:27
 * To change this template use File | Settings | File Templates.
 */
public interface GeneService {

    public List<Interactor> getGeneByEnsemblIdInSwissprot(String ensemblId) throws GeneServiceException;

    public List<Interactor> getGeneByEnsemblIdInUniprot(String ensemblId) throws GeneServiceException;


}
