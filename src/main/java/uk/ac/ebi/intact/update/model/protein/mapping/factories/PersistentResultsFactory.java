package uk.ac.ebi.intact.update.model.protein.mapping.factories;

import uk.ac.ebi.intact.protein.mapping.factories.ResultsFactory;
import uk.ac.ebi.intact.protein.mapping.results.BlastResults;
import uk.ac.ebi.intact.protein.mapping.results.IdentificationResults;
import uk.ac.ebi.intact.protein.mapping.results.PICRCrossReferences;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PersistentBlastResults;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PersistentIdentificationResults;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PersistentPICRCrossReferences;

/**
 * Factory returning results which can be persisted with the current persistence unit
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>27/06/11</pre>
 */

public class PersistentResultsFactory implements ResultsFactory{
    @Override
    public BlastResults getBlastResults() {
        return new PersistentBlastResults();
    }

    @Override
    public IdentificationResults getIdentificationResults() {
        return new PersistentIdentificationResults();
    }

    @Override
    public PICRCrossReferences getPICRCrossReferences() {
        return new PersistentPICRCrossReferences();
    }
}
