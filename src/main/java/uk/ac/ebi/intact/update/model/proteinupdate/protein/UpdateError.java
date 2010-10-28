package uk.ac.ebi.intact.update.model.proteinupdate.protein;

import uk.ac.ebi.intact.update.model.HibernatePersistentImpl;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@Entity
@Table(name = "ia_update_error")
public class UpdateError extends HibernatePersistentImpl{

    private String type;
    private String message;

    public UpdateError(){
        super();
        this.type = null;
        this.message = null;
    }

    public UpdateError(String type, String message){
        super();
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
