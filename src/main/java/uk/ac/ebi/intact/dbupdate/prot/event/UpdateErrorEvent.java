package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.dbupdate.prot.errors.ProteinUpdateError;
import uk.ac.ebi.intact.model.Protein;

import java.util.EventObject;

/**
 * Event containing an update error
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03-Nov-2010</pre>
 */

public class UpdateErrorEvent extends EventObject implements ProteinProcessorEvent{

    ProteinUpdateError error;

    /**
     * the data context
     */
    private DataContext dataContext;

    private Protein protein;

    private String uniprotAc;

    public UpdateErrorEvent(Object source, DataContext dataContext, ProteinUpdateError error) {
        super(source);
        this.error = error;
        this.dataContext = dataContext;
        this.protein = null;
        this.uniprotAc = null;
    }

    public UpdateErrorEvent(Object source, DataContext dataContext, ProteinUpdateError error, Protein protein) {
        super(source);
        this.error = error;
        this.dataContext = dataContext;
        this.protein = protein;
        this.uniprotAc = null;
    }

    public UpdateErrorEvent(Object source, DataContext dataContext, ProteinUpdateError error, String uniprotAc) {
        super(source);
        this.error = error;
        this.dataContext = dataContext;
        this.protein = null;
        this.uniprotAc = uniprotAc;
    }

    public UpdateErrorEvent(Object source, DataContext dataContext, ProteinUpdateError error, Protein protein, String uniprotAc) {
        super(source);
        this.error = error;
        this.dataContext = dataContext;
        this.protein = protein;
        this.uniprotAc = uniprotAc;
    }

    public ProteinUpdateError getError() {
        return error;
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
