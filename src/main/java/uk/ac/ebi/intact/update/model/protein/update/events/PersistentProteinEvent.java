package uk.ac.ebi.intact.update.model.protein.update.events;

import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.update.model.UpdateEvent;
import uk.ac.ebi.intact.update.model.UpdateProcess;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;

import javax.persistence.*;

/**
 * The super class which represents an event of a proteinAc update
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="objclass", discriminatorType=DiscriminatorType.STRING)
@DiscriminatorValue("PersistentProteinEvent")
@Table(name = "ia_protein_event")
public class PersistentProteinEvent extends UpdateEvent {

    ProteinEventName proteinEventName;

    String uniprotAc;

    String message;

    String proteinAc;

    public PersistentProteinEvent(){
        super();
        this.proteinEventName = null;
        this.uniprotAc = null;
        this.message = null;
    }

    public PersistentProteinEvent(ProteinUpdateProcess process, ProteinEventName name, Protein protein){
        super(process, name.toString());
        this.proteinEventName = name;
        this.proteinAc = protein != null ? protein.getAc() : null;

        InteractorXref uniprotXref = ProteinUtils.getUniprotXref(protein);
        if (uniprotXref != null){
            this.uniprotAc = uniprotXref.getPrimaryId();
        }
        else {
            this.uniprotAc = null;
        }
        this.message = null;
    }

    public PersistentProteinEvent(ProteinUpdateProcess process, ProteinEventName name, Protein protein, String uniprotAc){
        super(process, name.toString());
        this.proteinEventName = name;
        this.proteinAc = protein != null ? protein.getAc() : null;

        this.uniprotAc = uniprotAc;
        this.message = null;
    }

    public PersistentProteinEvent(ProteinUpdateProcess process, ProteinEventName name, String proteinAc){
        super(process, name.toString());
        this.proteinEventName = name;
        this.proteinAc = proteinAc;

        this.uniprotAc = null;
        this.message = null;
    }

    public PersistentProteinEvent(ProteinUpdateProcess process, ProteinEventName name, String proteinAc, String uniprotAc){
        super(process, name.toString());
        this.proteinEventName = name;
        this.proteinAc = proteinAc;

        this.uniprotAc = uniprotAc;
        this.message = null;
    }

    @Transient
    public ProteinEventName getProteinEventName() {

        if (proteinEventName == null && getName() != null){
            try{
                this.proteinEventName = ProteinEventName.valueOf(getName().toLowerCase());
            }
            catch (Exception e) {
                this.proteinEventName = ProteinEventName.none;
            }
        }

        return proteinEventName;
    }

    public void setProteinEventName(ProteinEventName proteinEventName) {
        this.proteinEventName = proteinEventName;
        setName(proteinEventName.toString());
    }

    @Column(name="protein_ac", nullable = false)
    public String getProteinAc() {
        return proteinAc;
    }

    public void setProteinAc(String proteinAc) {
        this.proteinAc = proteinAc;
    }

    @Override
    @ManyToOne( targetEntity = ProteinUpdateProcess.class )
    @JoinColumn( name = "parent_ac" )
    public UpdateProcess<PersistentProteinEvent> getParent(){
        return super.getParent();
    }

    @Column(name = "message")
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Column(name = "uniprot_ac")
    public String getUniprotAc() {
        return uniprotAc;
    }

    public void setUniprotAc(String uniprotAc) {
        this.uniprotAc = uniprotAc;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final PersistentProteinEvent event = (PersistentProteinEvent) o;

        if ( proteinEventName != null ) {
            if (!proteinEventName.equals( event.getProteinEventName())){
                return false;
            }
        }
        else if (event.getProteinEventName()!= null){
            return false;
        }

        if ( message != null ) {
            if (!message.equals( event.getMessage())){
                return false;
            }
        }
        else if (event.getMessage()!= null){
            return false;
        }

        if ( uniprotAc != null ) {
            if (!uniprotAc.equals( event.getUniprotAc())){
                return false;
            }
        }
        else if (event.getUniprotAc()!= null){
            return false;
        }

        if ( proteinAc != null ) {
            if (!proteinAc.equals( event.getProteinAc())){
                return false;
            }
        }
        else if (event.getProteinAc()!= null){
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

        if ( proteinEventName != null ) {
            code = 29 * code + proteinEventName.hashCode();
        }

        if ( message != null ) {
            code = 29 * code + message.hashCode();
        }

        if ( uniprotAc != null ) {
            code = 29 * code + uniprotAc.hashCode();
        }

        if ( proteinAc != null ) {
            code = 29 * code + proteinAc.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final PersistentProteinEvent event = (PersistentProteinEvent) o;

        if ( proteinEventName != null ) {
            if (!proteinEventName.equals( event.getProteinEventName())){
                return false;
            }
        }
        else if (event.getProteinEventName()!= null){
            return false;
        }

        if ( message != null ) {
            if (!message.equals( event.getMessage())){
                return false;
            }
        }
        else if (event.getMessage()!= null){
            return false;
        }

        if ( uniprotAc != null ) {
            if (!uniprotAc.equals( event.getUniprotAc())){
                return false;
            }
        }
        else if (event.getUniprotAc()!= null){
            return false;
        }

        if ( proteinAc != null ) {
            if (!proteinAc.equals( event.getProteinAc())){
                return false;
            }
        }
        else if (event.getProteinAc()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Protein : " + proteinAc != null ? proteinAc : "none");
        buffer.append("Uniprot ac : " + uniprotAc != null ? uniprotAc : "none");
        buffer.append("Message : " + message != null ? message : "none");
        buffer.append(" \n");

        return buffer.toString();
    }
}
