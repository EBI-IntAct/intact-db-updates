package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinProcessor;
import uk.ac.ebi.intact.protein.mapping.model.contexts.UpdateContext;
import uk.ac.ebi.intact.update.model.proteinmapping.results.IdentificationResults;

import java.util.EventObject;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>05-Jan-2011</pre>
 */

public class ProteinRemappingEvent extends EventObject implements ProteinProcessorEvent, MessageContainer{

    private UpdateContext context;
    private IdentificationResults result;
    private String message;
    private DataContext dataContext;

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public ProteinRemappingEvent(ProteinProcessor source, DataContext context, UpdateContext updateContext, IdentificationResults results, String message) {
        super(source);
        this.dataContext = context;
        this.context = updateContext;
        this.result = results;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public DataContext getDataContext() {
        return this.dataContext;
    }

    public UpdateContext getContext() {
        return context;
    }

    public IdentificationResults getResult() {
        return result;
    }
}
