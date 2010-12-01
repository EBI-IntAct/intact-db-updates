package uk.ac.ebi.intact.update.model.proteinupdate;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.Date;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>24-Nov-2010</pre>
 */
@Entity
@DiscriminatorValue("DeletedProteinEvent")
public class DeletedProteinEvent extends ProteinEvent{

    String message;

    public DeletedProteinEvent(){
        super();
        this.message = null;

    }

    public DeletedProteinEvent(Date created, String message, int index){
        super(EventName.delete_protein, created, index);
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
