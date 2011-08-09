package uk.ac.ebi.intact.update.model.protein;

import uk.ac.ebi.intact.model.Xref;
import uk.ac.ebi.intact.update.model.HibernateUpdatePersistentImpl;
import uk.ac.ebi.intact.update.model.UpdateEventImpl;
import uk.ac.ebi.intact.update.model.UpdateStatus;
import uk.ac.ebi.intact.update.model.protein.events.PersistentProteinEvent;

import javax.persistence.*;

/**
 * Cross reference of a protein
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@Entity
@Table(name = "ia_prot_updated_xref")
public class UpdatedCrossReference extends HibernateUpdatePersistentImpl {

    /**
     * The database
     */
    private String database;

    /**
     * The identifier
     */
    private String identifier;

    /**
     * The qualifier
     */
    private String qualifier;

    private UpdateEventImpl parent;

    private UpdateStatus status;

    public UpdatedCrossReference(){
        super();
        this.database = null;
        this.identifier = null;
        this.qualifier = null;
        this.status = UpdateStatus.none;

        this.parent = null;
    }

    public UpdatedCrossReference(Xref ref, UpdateStatus status){

        super();
        if (ref != null){

            this.database = ref.getCvDatabase() != null ? ref.getCvDatabase().getAc() : null;

            this.identifier = ref.getPrimaryId();

            this.qualifier = ref.getCvXrefQualifier() != null ? ref.getCvXrefQualifier().getAc() : null;
        }
        else {
            this.database = null;
            this.identifier = null;
            this.qualifier = null;
        }

        this.status = status != null ? status : UpdateStatus.none;
        this.parent = null;
    }

    @Column(name="database_ac", nullable = false)
    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    @Column(name = "identifier", nullable = false)
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Column(name = "qualifier_ac", nullable = true)
    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
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
    @JoinColumn( name = "event_id", nullable = false)
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

        final UpdatedCrossReference updated = ( UpdatedCrossReference ) o;

        if ( database != null ) {
            if (!database.equals( updated.getDatabase())){
                return false;
            }
        }
        else if (updated.getDatabase()!= null){
            return false;
        }

        if ( identifier != null ) {
            if (!identifier.equals( updated.getIdentifier())){
                return false;
            }
        }
        else if (updated.getIdentifier()!= null){
            return false;
        }

        if ( qualifier != null ) {
            if (!qualifier.equals( updated.getQualifier())){
                return false;
            }
        }
        else if (updated.getQualifier()!= null){
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

        if ( database != null ) {
            code = 29 * code + database.hashCode();
        }

        if ( identifier != null ) {
            code = 29 * code + identifier.hashCode();
        }

        if ( qualifier != null ) {
            code = 29 * code + qualifier.hashCode();
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

        final UpdatedCrossReference updated = ( UpdatedCrossReference ) o;

        if ( database != null ) {
            if (!database.equals( updated.getDatabase())){
                return false;
            }
        }
        else if (updated.getDatabase()!= null){
            return false;
        }

        if ( identifier != null ) {
            if (!identifier.equals( updated.getIdentifier())){
                return false;
            }
        }
        else if (updated.getIdentifier()!= null){
            return false;
        }

        if ( qualifier != null ) {
            if (!qualifier.equals( updated.getQualifier())){
                return false;
            }
        }
        else if (updated.getQualifier()!= null){
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

        buffer.append("XRef : [ database = " + database != null ? database : "none" + ", identifier = " + identifier != null ? identifier : "none" + ", qualifier = " + qualifier != null ? qualifier : "none" + ", status = " + status != null ? status.toString() : "none");

        return buffer.toString();
    }
}
