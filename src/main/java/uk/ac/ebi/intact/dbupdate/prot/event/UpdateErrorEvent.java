package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.model.Protein;

import java.util.EventObject;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03-Nov-2010</pre>
 */

public class UpdateErrorEvent extends EventObject implements ProteinProcessorEvent, MessageContainer{

    UpdateError error;

    /**
     * the data context
     */
    private DataContext dataContext;

    /**
     * the message
     */
    private String message;

    public UpdateErrorEvent(Object source, DataContext dataContext, String message, UpdateError error) {
        super(source);
        this.error = error;
        this.dataContext = dataContext;
        this.message = message;
    }

    public UpdateError getError() {
        return error;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public DataContext getDataContext() {
        return this.dataContext;
    }
}
