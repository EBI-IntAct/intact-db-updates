package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.dbupdate.prot.UpdateError;
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

    private Protein protein;

    private String uniprotAc;

    public UpdateErrorEvent(Object source, DataContext dataContext, String message, UpdateError error) {
        super(source);
        this.error = error;
        this.dataContext = dataContext;
        this.message = message;
        this.protein = null;
        this.uniprotAc = null;
    }

    public UpdateErrorEvent(Object source, DataContext dataContext, String message, UpdateError error, Protein protein) {
        super(source);
        this.error = error;
        this.dataContext = dataContext;
        this.message = message;
        this.protein = protein;
        this.uniprotAc = null;
    }

    public UpdateErrorEvent(Object source, DataContext dataContext, String message, UpdateError error, String uniprotAc) {
        super(source);
        this.error = error;
        this.dataContext = dataContext;
        this.message = message;
        this.protein = null;
        this.uniprotAc = uniprotAc;
    }

    public UpdateErrorEvent(Object source, DataContext dataContext, String message, UpdateError error, Protein protein, String uniprotAc) {
        super(source);
        this.error = error;
        this.dataContext = dataContext;
        this.message = message;
        this.protein = protein;
        this.uniprotAc = uniprotAc;
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

    public Protein getProtein() {
        return protein;
    }

    public void setProtein(Protein protein) {
        this.protein = protein;
    }

    public String getUniprotAc() {
        return uniprotAc;
    }

    public void setUniprotAc(String uniprotAc) {
        this.uniprotAc = uniprotAc;
    }
}
