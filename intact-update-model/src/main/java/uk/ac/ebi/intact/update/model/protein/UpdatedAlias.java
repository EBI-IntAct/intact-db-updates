package uk.ac.ebi.intact.update.model.protein;

import uk.ac.ebi.intact.model.Alias;
import uk.ac.ebi.intact.update.model.HibernateUpdatePersistentImpl;
import uk.ac.ebi.intact.update.model.UpdateEventImpl;
import uk.ac.ebi.intact.update.model.UpdateStatus;
import uk.ac.ebi.intact.update.model.protein.events.PersistentProteinEvent;

import javax.persistence.*;

/**
 * ProteinUpdateAlias of a protein
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@Entity
@Table(name = "ia_prot_updated_alias")
public class UpdatedAlias extends HibernateUpdatePersistentImpl {

    /**
     * The alias type
     */
    private String type;

    /**
     * The alias name
     */
    private String name;

    /**
     * The updateProcess
     */
    private UpdateEventImpl parent;

    /**
     * The update status
     */
    private UpdateStatus status;

    public UpdatedAlias(){
        super();
        this.name = null;
        this.type = null;
        this.status = UpdateStatus.none;
        this.parent = null;
    }

    public UpdatedAlias(String type, String name, UpdateStatus status){
        super();
        this.name = name;
        this.type = type;
        this.status = status != null ? status : UpdateStatus.none;
        this.parent = null;
    }

    public UpdatedAlias(Alias alias, UpdateStatus status){
        super();
        if (alias != null){
            type = alias.getCvAliasType() != null ? alias.getCvAliasType().getAc() : null;

            this.name = alias.getName();
        }
        else {
            this.type = null;
            this.name = null;
        }
        this.status = status != null ? status : UpdateStatus.none;
        this.parent = null;
    }

    @Column(name="type_ac", nullable = false)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Column(name = "name", nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    public UpdateStatus getStatus() {
        return status;
    }

    public void setStatus(UpdateStatus status) {
        this.status = status;
    }

    @ManyToOne( targetEntity = PersistentProteinEvent.class )
    @JoinColumn( name = "event_id" )
    public UpdateEventImpl getParent() {
        return parent;
    }

    public void setParent(UpdateEventImpl parent) {
        this.parent = parent;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final UpdatedAlias updated = ( UpdatedAlias ) o;

        if ( type != null ) {
            if (!type.equals( updated.getType())){
                return false;
            }
        }
        else if (updated.getType()!= null){
            return false;
        }

        if ( name != null ) {
            if (!name.equals( updated.getName())){
                return false;
            }
        }
        else if (updated.getName()!= null){
            return false;
        }

        if ( status != null ) {
            if (!status.equals( updated.getStatus())){
                return false;
            }
        }
        else if (updated.getStatus()!= null){
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

        if ( type != null ) {
            code = 29 * code + type.hashCode();
        }

        if ( name != null ) {
            code = 29 * code + name.hashCode();
        }

        if ( status != null ) {
            code = 29 * code + status.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final UpdatedAlias updated = ( UpdatedAlias ) o;

        if ( type != null ) {
            if (!type.equals( updated.getType())){
                return false;
            }
        }
        else if (updated.getType()!= null){
            return false;
        }

        if ( name != null ) {
            if (!name.equals( updated.getName())){
                return false;
            }
        }
        else if (updated.getName()!= null){
            return false;
        }

        if ( status != null ) {
            if (!status.equals( updated.getStatus())){
                return false;
            }
        }
        else if (updated.getStatus()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Alias : [ type = " + type != null ? type : "none" + ", name = " + name != null ? name : "none" + ", status = " + status != null ? status.toString() : "none");

        return buffer.toString();
    }
}
