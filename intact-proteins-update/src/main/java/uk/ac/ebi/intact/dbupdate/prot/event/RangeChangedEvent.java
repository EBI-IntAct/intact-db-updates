package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.UpdatedRange;

/**
 * Event fired when a range is updated
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class RangeChangedEvent implements ProteinProcessorEvent {

    /**
     * the data context
     */
    private DataContext dataContext;

    /**
     * The range updated
     */
    private UpdatedRange updatedRange;

    public RangeChangedEvent(DataContext dataContext, UpdatedRange updatedRange) {
        this.dataContext = dataContext;
        this.updatedRange = updatedRange;
    }

    public DataContext getDataContext() {
        return dataContext;
    }

    public UpdatedRange getUpdatedRange() {
        return updatedRange;
    }
}