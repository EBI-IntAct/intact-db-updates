package uk.ac.ebi.intact.update.model.protein.mapping.factories;

import uk.ac.ebi.intact.protein.mapping.actions.ActionName;
import uk.ac.ebi.intact.protein.mapping.factories.ReportsFactory;
import uk.ac.ebi.intact.protein.mapping.model.actionReport.*;
import uk.ac.ebi.intact.protein.mapping.model.actionReport.impl.DefaultIntactCrc64Report;
import uk.ac.ebi.intact.protein.mapping.model.actionReport.impl.DefaultIntactReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentBlastReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentMappingReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentPICRReport;

/**
 * This reports factory return reports which can be persisted with the current persistence unit
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>27/06/11</pre>
 */

public class PersistentReportsFactory implements ReportsFactory {
    @Override
    public BlastReport getBlastReport(ActionName name) {
        return new PersistentBlastReport(name);
    }

    @Override
    public IntactCrc64Report getIntactCrc64Report(ActionName name) {
        return new DefaultIntactCrc64Report(name);
    }

    @Override
    public IntactReport getIntactReport(ActionName name) {
        return new DefaultIntactReport(name);
    }

    @Override
    public MappingReport getMappingReport(ActionName name) {
        return new PersistentMappingReport(name);
    }

    @Override
    public PICRReport getPICRReport(ActionName name) {
        return new PersistentPICRReport(name);
    }
}
