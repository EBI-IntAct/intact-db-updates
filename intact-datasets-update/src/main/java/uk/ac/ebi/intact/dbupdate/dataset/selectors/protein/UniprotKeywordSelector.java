package uk.ac.ebi.intact.dbupdate.dataset.selectors.protein;

import org.springframework.transaction.TransactionStatus;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.dbupdate.dataset.DatasetException;
import uk.ac.ebi.intact.model.CvDatabase;
import uk.ac.ebi.intact.model.CvXrefQualifier;
import uk.ac.ebi.kraken.interfaces.uniprot.Keyword;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.client.Client;
import uk.ac.ebi.uniprot.dataservice.client.QueryResult;
import uk.ac.ebi.uniprot.dataservice.client.exception.ServiceException;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtComponent;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtQueryBuilder;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtService;

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

    private UniProtService uniprotService;

    public UniprotKeywordSelector(){
        super();
        this.uniprotService = Client.getServiceFactoryInstance().getUniProtQueryService();
    }

    public UniprotKeywordSelector(String report){
        super(report);
        this.uniprotService = Client.getServiceFactoryInstance().getUniProtQueryService();
    }

    private boolean isProteinAssociatedWithKeywordInUniprot(String uniprotId){
        String fixedUniprotAc = uniprotId;
        uniprotService.start();
        if (uniprotId.contains("-")){
            fixedUniprotAc = uniprotId.substring(0, uniprotId.indexOf("-"));
        }
        try{
            //Retrieve list of keyword objects from a UniProt entry by its accession number
            QueryResult<UniProtEntry> entry = uniprotService.getEntries(UniProtQueryBuilder.accession(fixedUniprotAc).or(UniProtQueryBuilder.secondaryAccession(fixedUniprotAc)));
            // Cast the object to UniProt Keywords
            for (Keyword key : entry.getFirstResult().getKeywords()){
                if (key.getValue().equalsIgnoreCase(keyword)){
                    uniprotService.stop();
                    return true;
                }
            }
        } catch (ServiceException e) {
            uniprotService.stop();
            log.error("Uniprot " + uniprotId + " has not been found ", e);
        } catch (NullPointerException e){
            uniprotService.stop();
            log.error("Uniprot " + uniprotId + " has not been found ", e);
        }
        uniprotService.stop();
        return false;
    }


    /**
     *
     * @return a list of String [] which contain the intact ac and uniprot ac of each uniprot protein represented in IntAct
     */
    private List<Object[]> getProteinsFromUniprotInIntact(){
        DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();
        TransactionStatus status = dataContext.beginTransaction();

        String finalQuery = "select p.ac, x.primaryId from ProteinImpl p join p.xrefs as x where x.cvDatabase.identifier = :uniprot and x.cvXrefQualifier.identifier = :identity";

        // create the query
        final Query query = IntactContext.getCurrentInstance().getDaoFactory().getEntityManager().createQuery(finalQuery);

        // set the query parameters
        query.setParameter("uniprot", CvDatabase.UNIPROT_MI_REF);
        query.setParameter("identity", CvXrefQualifier.IDENTITY_MI_REF);

        // get the intact proteins matching the cross reference
        List<Object[]> listOfAcs = query.getResultList();
        dataContext.commitTransaction(status);

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
    public Set<String> collectSelectionOfProteinAccessionsInIntact() throws DatasetException {
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
