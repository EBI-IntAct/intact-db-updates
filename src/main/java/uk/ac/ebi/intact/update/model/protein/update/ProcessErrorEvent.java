package uk.ac.ebi.intact.update.model.protein.update;

import javax.persistence.*;
import java.util.Date;

/**
 * TODO comment this
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

    public ProcessErrorEvent(){
        super();
        this.type = null;
        this.message = null;
    }

    public ProcessErrorEvent(Date created, int index, String type, String message){
        super(EventName.update_error, created, index);
        this.type = type;
        this.message = message;
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
}
