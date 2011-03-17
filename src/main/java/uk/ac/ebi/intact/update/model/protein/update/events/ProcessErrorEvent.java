package uk.ac.ebi.intact.update.model.protein.update.events;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.update.UpdateProcess;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Event for errors during proteinAc update process
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@Entity
@DiscriminatorValue("ProcessErrorEvent")
public class ProcessErrorEvent extends ProteinEvent{

    private String type;
    private String message;

    private String uniprotAc;

    public ProcessErrorEvent(){
        super();
        this.type = null;
        this.message = null;
        this.uniprotAc = null;
    }

    public ProcessErrorEvent(UpdateProcess updateProcess, Protein protein, int index, String type, String message, String uniprotAc ){
        super(updateProcess, EventName.update_error, protein, index);
        this.type = type;
        this.message = message;
        this.uniprotAc = uniprotAc;
    }

    @Column(name = "message")
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Column(name = "error_type", nullable = false)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Column(name = "uniprot_ac", nullable = true)
    public String getUniprotAc() {
        return uniprotAc;
    }

    public void setUniprotAc(String uniprotAc) {
        this.uniprotAc = uniprotAc;
    }
}
