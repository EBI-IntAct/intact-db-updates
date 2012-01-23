package uk.ac.ebi.intact.dbupdate.dataset.selectors.protein;

import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.dbupdate.dataset.DatasetException;
import uk.ac.ebi.intact.model.CvDatabase;
import uk.ac.ebi.intact.model.CvXrefQualifier;
import uk.ac.ebi.kraken.interfaces.uniprot.Keyword;
import uk.ac.ebi.kraken.uuw.services.remoting.EntryRetrievalService;
import uk.ac.ebi.kraken.uuw.services.remoting.RemoteDataAccessException;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtJAPI;

import javax.persistence.Query;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This selector returns the list of intact acs for the proteins in IntAct which are associated with a specific keyword in uniprot
 * (such as apoptosis, etc.)
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>23/01/12</pre>
 */

public class UniprotKeywordSelector extends ProteinDatasetSelectorImpl{
    private String keyword;

    private static final String keywordField = "ognl:keywords";

    private EntryRetrievalService entryRetrievalService;

    public UniprotKeywordSelector(){
        this.entryRetrievalService = UniProtJAPI.factory.getEntryRetrievalService();
    }

    private boolean isProteinAssociatedWithKeywordInUniprot(String uniprotId){

        try{
            //Retrieve list of keyword objects from a UniProt entry by its accession number
            Object attribute = UniProtJAPI.factory.getEntryRetrievalService().getUniProtAttribute(uniprotId ,  "ognl:keywords");

            // Cast the object to UniProt Keywords
            List<Keyword> keywords  = (List<Keyword>)attribute;

            for (Keyword key : keywords){
                if (key.getValue().equalsIgnoreCase(keyword)){
                    return true;
                }
            }
        }
        catch (RemoteDataAccessException e){
            log.error("Uniprot " + uniprotId + " has not been found ", e);
        }

        return false;
    }

    /**
     *
     * @return a list of String [] which contain the intact ac and uniprot ac of each uniprot protein represented in IntAct
     */
    private List<Object[]> getProteinsFromUniprotInIntact(){
        String finalQuery = "select p.ac, x.primaryId from ProteinImpl p join p.xrefs as x where x.cvDatabase.identifier = :uniprot and x.cvXrefQualifier.identifier = :identity";

        // create the query
        final Query query = IntactContext.getCurrentInstance().getDaoFactory().getEntityManager().createQuery(finalQuery);

        // set the query parameters
        query.setParameter("uniprot", CvDatabase.UNIPROT_MI_REF);
        query.setParameter("identity", CvXrefQualifier.IDENTITY_MI_REF);

        // get the intact proteins matching the cross reference
        List<Object[]> listOfAcs = query.getResultList();

        return listOfAcs;
    }

    @Override
    protected void readSpecificContent(String[] columns) throws DatasetException {
        int columnLength = columns.length;

        if (columnLength > 0){
            this.keyword = columns[columnLength - 1];
        }
        else {
            keyword = null;
        }
    }

    @Override
    public Set<String> getSelectionOfProteinAccessionsInIntact() throws DatasetException {
        Set<String> proteinAccessions = new HashSet<String>();

        List<Object[]> listOfExistingUniprotInIntact = getProteinsFromUniprotInIntact();

        for (Object[] o : listOfExistingUniprotInIntact){
            String intactAc = (String) o[0];
            String uniprotAc = (String) o[1];

            if (isProteinAssociatedWithKeywordInUniprot(uniprotAc)){
                proteinAccessions.add(intactAc);
            }
        }

        // write protein report if file enabled
        try {
            writeProteinReport(proteinAccessions);
        } catch (IOException e) {
            throw new DatasetException("We can't write the results of the protein selection.", e);
        }

        return proteinAccessions;
    }

    public String getKeyword() {
        return keyword;
    }
}
