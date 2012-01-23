package uk.ac.ebi.intact.dbupdate.dataset.selectors;

import uk.ac.ebi.intact.dbupdate.dataset.DatasetException;
import uk.ac.ebi.intact.dbupdate.dataset.selectors.protein.InteractorXRefSelector;

/**
 * selector for updating ndpk dataset
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>23/01/12</pre>
 */

public class NdpkDatasetSelector extends InteractorXRefSelector{

    private static final String NDPK_CONFIG = "/dataset/ndpk.csv";

    public NdpkDatasetSelector() throws DatasetException {
        super();

        readDatasetFromResources(NDPK_CONFIG);
    }

    public NdpkDatasetSelector(String report) throws DatasetException {
        super(report);

        readDatasetFromResources(NDPK_CONFIG);
    }
}
