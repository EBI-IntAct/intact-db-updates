package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;

import java.util.EventObject;

/**
 * Event fired when a protein is updated
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class ProteinEvent extends EventObject implements ProteinProcessorEvent, MessageContainer {

    /**
     * the protein updated
     */
    private Protein protein;

    /**
     * the data context
     */
    private DataContext dataContext;

    /**
     * the message
     */
    private String message;

    /**
     * the uniprot protein
     */
    private UniprotProtein uniprotProtein;

    /**
     * The uniprot identity of the protein
     */
    private String uniprotIdentity;

    /**
     * A protein update event
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public ProteinEvent(Object source, DataContext dataContext, Protein protein) {
        super(source);
        this.protein = protein;
        this.dataContext = dataContext;
        this.message = null;
        this.uniprotProtein = null;
        this.uniprotIdentity = null;
    }

    public ProteinEvent(Object source, DataContext dataContext, Protein protein, UniprotProtein uniprotProtein) {
        super(source);
        this.protein = protein;
        this.dataContext = dataContext;
        this.uniprotProtein = uniprotProtein;
        this.message = null;
        this.uniprotIdentity = null;
    }

    public ProteinEvent(Object source, DataContext dataContext, Protein protein, String message) {
        this(source, dataContext, protein);
        this.message = message;
        this.uniprotProtein = null;
        this.uniprotIdentity = null;
    }

    public ProteinEvent(Object source, DataContext dataContext, Protein protein, UniprotProtein uniprotProtein, String message) {
        super(source);
        this.protein = protein;
        this.dataContext = dataContext;
        this.uniprotProtein = uniprotProtein;
        this.message = message;
        this.uniprotIdentity = null;
    }

    public DataContext getDataContext() {
        return dataContext;
    }

    public Protein getProtein() {
        return protein;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UniprotProtein getUniprotProtein() {
        return uniprotProtein;
    }

    public void setUniprotProtein(UniprotProtein uniprotProtein) {
        this.uniprotProtein = uniprotProtein;
    }

    public String getUniprotIdentity() {
        return uniprotIdentity;
    }

    public void setUniprotIdentity(String uniprotIdentity) {
        this.uniprotIdentity = uniprotIdentity;
    }
}