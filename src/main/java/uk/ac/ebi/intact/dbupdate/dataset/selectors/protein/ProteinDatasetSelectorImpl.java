package uk.ac.ebi.intact.dbupdate.dataset.selectors.protein;

import uk.ac.ebi.intact.dbupdate.dataset.selectors.DatasetSelectorImpl;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

/**
 * The base implementation of ProteinDatasetSelector. It can add an organism selection on the query to select protein
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>16-Jun-2010</pre>
 */

public abstract class ProteinDatasetSelectorImpl extends DatasetSelectorImpl implements ProteinDatasetSelector{


    /**
     * Create a new ProteinDatasetSelectorImpl with no dataset value and no intact context. These two variables must be initialised using the set methods
     */
    public ProteinDatasetSelectorImpl(){
        super();
    }

    public ProteinDatasetSelectorImpl(String report){
        super(report);
    }

    /**
     *
     * @param interactorQuery : the query to select interactors with specific aliases
     * @return a new query as a String adding the organism restrictions to the previous query
     */
    protected String addOrganismSelection(String interactorQuery){
        StringBuffer query = new StringBuffer(1640);

        // We don't have any organism restrictions so we don't change the previous query
        if (this.listOfPossibleTaxId.isEmpty()){
            return interactorQuery;
        }

        // We want all the interactors with an organism which is contained in the list of organism restrictions
        query.append("select i.ac from InteractorImpl i join i.bioSource as b where (b.taxId in (" );

        for (String t : listOfPossibleTaxId){
            query.append("'"+t+"', ");
        }

        query.delete(query.lastIndexOf(","), query.length());
        query.append(")) and i.ac in ("+interactorQuery+")");

        return query.toString();
    }

    /**
     * Write the protein accessions selected in a file if isFileWriterEnabled
     * @param proteinAccessions
     * @throws java.io.IOException
     */
    protected void writeProteinReport(Set<String> proteinAccessions) throws IOException {

        if (isFileWriterEnabled()){
            // create the file where to write the report
            Writer writer = new FileWriter(report);

            writer.write("Collect "+proteinAccessions.size()+" proteins related to the dataset '"+this.datasetValue+"' \n");

            for (String ac : proteinAccessions){
                writer.write(ac+"\n");
            }

            writer.flush();

            writer.close();
        }
    }
}
