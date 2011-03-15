package uk.ac.ebi.intact.update.model.protein.update.events;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.update.UpdateProcess;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * Super class for events having messages
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>14/03/11</pre>
 */
@MappedSuperclass
public class ProteinEventWithMessage extends ProteinEvent {

    String message;

    public ProteinEventWithMessage(){
        super();
        this.message = null;

    }

    public ProteinEventWithMessage(UpdateProcess updateProcess, EventName name, Protein protein, String message, int index){
        super(updateProcess, name, protein, index);
        this.message = message;
    }

    @Column(name = "message", nullable = false)
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
