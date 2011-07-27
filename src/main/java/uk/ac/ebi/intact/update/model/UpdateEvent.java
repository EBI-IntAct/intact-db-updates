package uk.ac.ebi.intact.update.model;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.io.Serializable;

/**
 * Abstract class for each updateEvent
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>18/07/11</pre>
 */
@MappedSuperclass
public abstract class UpdateEvent extends HibernateUpdatePersistentImpl implements Serializable {

    String name;

    UpdateProcess parent;

    public UpdateEvent(){
        super();
        this.name = null;
    }

    public UpdateEvent(UpdateProcess process, String name){
        super();
        this.name = name;
        this.parent = process;
    }

    @Transient
    public UpdateProcess getParent() {
        return this.parent;
    }

    public void setParent(UpdateProcess updateProcess) {
        this.parent = updateProcess;
    }

    @Column(name = "name", nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final UpdateEvent event = (UpdateEvent) o;

        if ( name != null ) {
            if (!name.equals( event.getName())){
                return false;
            }
        }
        else if (event.getName()!= null){
            return false;
        }

        return true;
    }

    /**
     * This class overwrites equals. To ensure proper functioning of HashTable,
     * hashCode must be overwritten, too.
     *
     * @return hash code of the object.
     */
    @Override
    public int hashCode() {

        int code = 29;

        code = 29 * code + super.hashCode();

        if ( name != null ) {
            code = 29 * code + name.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final UpdateEvent event = (UpdateEvent) o;

        if ( name != null ) {
            if (!name.equals( event.getName())){
                return false;
            }
        }
        else if (event.getName()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Event : " + name != null ? name.toString() : "none");
        buffer.append(" \n");

        return buffer.toString();
    }
}
