package uk.ac.ebi.intact.dbupdate.dataset.selectors;

import uk.ac.ebi.intact.dbupdate.dataset.DatasetException;
import uk.ac.ebi.intact.dbupdate.dataset.selectors.protein.UniprotKeywordSelector;

/**
 * Selector for updating apoptosis dataset
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>23/01/12</pre>
 */

public class ApoptosisDatasetSelector extends UniprotKeywordSelector{

    private static final String APOPTOSIS_CONFIG = "/dataset/apoptosis.csv";

    public ApoptosisDatasetSelector() throws DatasetException {
        super();

        readDatasetFromResources(APOPTOSIS_CONFIG);
    }

    public ApoptosisDatasetSelector(String report) throws DatasetException {
        super(report);

        readDatasetFromResources(APOPTOSIS_CONFIG);
    }
}
