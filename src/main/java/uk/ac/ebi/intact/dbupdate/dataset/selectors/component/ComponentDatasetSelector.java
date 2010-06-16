package uk.ac.ebi.intact.dbupdate.dataset.selectors.component;

import uk.ac.ebi.intact.dbupdate.dataset.DatasetException;
import uk.ac.ebi.intact.dbupdate.dataset.selectors.DatasetSelector;

import java.util.Set;

/**
 * This interface is the interface to implement for all the DatasetSelectors which aim at selcting components
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>15-Jun-2010</pre>
 */

public interface ComponentDatasetSelector extends DatasetSelector {

    /**
     *
     * @return The component IntAct accessions which are respecting the conditions imposed by the DatasetSelector
     * @throws uk.ac.ebi.intact.dbupdate.dataset.DatasetException : exception if the intact context is not set or if there is no component to retrieve
     * or if the dataset value is not set.
     */
    public Set<String> getSelectionOfComponentAccessionsInIntact() throws DatasetException;
}
