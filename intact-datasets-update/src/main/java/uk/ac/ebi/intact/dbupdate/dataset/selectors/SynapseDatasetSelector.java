package uk.ac.ebi.intact.dbupdate.dataset.selectors;

import uk.ac.ebi.intact.dbupdate.dataset.DatasetException;
import uk.ac.ebi.intact.dbupdate.dataset.selectors.protein.InteractorAliasSelector;

/**
 * Dataset writer for synapse
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>23/01/12</pre>
 */

public class SynapseDatasetSelector extends InteractorAliasSelector {

    private static final String SYNAPSE_CONFIG = "/dataset/synapse.csv";

    public SynapseDatasetSelector() throws DatasetException {
        super();

        readDatasetFromResources(SYNAPSE_CONFIG);
    }

    public SynapseDatasetSelector(String report) throws DatasetException {
        super(report);

        readDatasetFromResources(SYNAPSE_CONFIG);
    }
}
