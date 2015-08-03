package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinProcessor;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.protein.mapping.model.actionReport.MappingReport;
import uk.ac.ebi.intact.protein.mapping.model.contexts.UpdateContext;
import uk.ac.ebi.intact.protein.mapping.results.IdentificationResults;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>05-Jan-2011</pre>
 */

public class ProteinRemappingEvent extends ProteinEvent{

    private UpdateContext context;
    private IdentificationResults<MappingReport> result;
    private String message;

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public ProteinRemappingEvent(ProteinProcessor source, DataContext context, Protein protein, UpdateContext updateContext, IdentificationResults results, String message) {
        super(source, context, protein);
        this.context = updateContext;
        this.result = results;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    public UpdateContext getContext() {
        return context;
    }

    public IdentificationResults getResult() {
        return result;
    }
}
