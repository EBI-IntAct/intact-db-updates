package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.OutOfBoundRange;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>06-Aug-2010</pre>
 */

public class RangeOutOfBoundEvent implements ProteinProcessorEvent {
    private DataContext dataContext;

    private OutOfBoundRange outOfBoundRange;

    public RangeOutOfBoundEvent(DataContext dataContext, OutOfBoundRange range) {
        this.dataContext = dataContext;
        this.outOfBoundRange = range;
    }
    public DataContext getDataContext() {
        return this.dataContext;
    }

        public OutOfBoundRange getOutOfBoundRange() {
        return outOfBoundRange;
    }
}
