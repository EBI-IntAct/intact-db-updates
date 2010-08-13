package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.InvalidRange;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>06-Aug-2010</pre>
 */

public class InvalidRangeEvent implements ProteinProcessorEvent {
    private DataContext dataContext;

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
