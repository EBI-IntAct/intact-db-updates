package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.InvalidRange;

/**
 * This event is thrwn when an invalid range is found
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>06-Aug-2010</pre>
 */

public class InvalidRangeEvent implements ProteinProcessorEvent {
    /**
     * the data context
     */
    private DataContext dataContext;

    /**
     * the invalid range
     */
    private InvalidRange invalidRange;

    public InvalidRangeEvent(DataContext dataContext, InvalidRange range) {
        this.dataContext = dataContext;
        this.invalidRange = range;
    }
    public DataContext getDataContext() {
        return this.dataContext;
    }

    public InvalidRange getInvalidRange() {
        return invalidRange;
    }
}
