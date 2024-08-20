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
    private String proteinAc;

    public UpdateErrorEvent(Object source,
                            DataContext dataContext,
                            ProteinUpdateError error,
                            Protein protein,
                            String uniprotAc,
                            String proteinAc) {
        super(source);
        this.error = error;
        this.dataContext = dataContext;
        this.protein = protein;
        this.uniprotAc = uniprotAc;
        this.proteinAc = proteinAc;
    }

    public UpdateErrorEvent(Object source, DataContext dataContext, ProteinUpdateError error, Protein protein, String uniprotAc) {
        this(source, dataContext, error, protein, uniprotAc, null);
    }

    public UpdateErrorEvent(Object source, DataContext dataContext, ProteinUpdateError error, Protein protein) {
        this(source, dataContext, error, protein, null, null);
    }

    public UpdateErrorEvent(Object source, DataContext dataContext, ProteinUpdateError error, String uniprotAc) {
        this(source, dataContext, error, null, uniprotAc, null);
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

    public String getProteinAc() {
        return proteinAc;
    }

    public void setProteinAc(String proteinAc) {
        this.proteinAc = proteinAc;
    }
}