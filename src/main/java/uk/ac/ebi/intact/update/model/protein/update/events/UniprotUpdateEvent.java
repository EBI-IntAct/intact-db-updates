package uk.ac.ebi.intact.update.model.protein.update.events;

import org.hibernate.annotations.DiscriminatorFormula;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;
import uk.ac.ebi.intact.update.model.protein.update.ProteinEventName;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Event for the basic update of a uniprot proteinAc
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@Entity
@DiscriminatorFormula("objclass")
@DiscriminatorValue("UniprotUpdateEvent")
public class UniprotUpdateEvent extends ProteinEventWithRangeUpdate {

    private String updatedShortLabel;
    private String updatedFullName;
    private String uniprotQuery;

    public UniprotUpdateEvent(){
        super();
        this.updatedShortLabel = null;
        this.updatedFullName = null;
    }

    public UniprotUpdateEvent(ProteinUpdateProcess updateProcess, Protein protein, String shortlabel, String fullname, String uniprotEntry){
        super(updateProcess, ProteinEventName.uniprot_update, protein);
        this.updatedShortLabel = shortlabel;
        this.updatedFullName = fullname;
        this.uniprotQuery = uniprotEntry;
    }

    public UniprotUpdateEvent(ProteinUpdateProcess updateProcess, Protein protein, String uniprotEntry){
        super(updateProcess, ProteinEventName.uniprot_update, protein);
        this.updatedShortLabel = null;
        this.updatedFullName = null;
        this.uniprotQuery = uniprotEntry;
    }

    @Column(name = "shortlabel")
    public String getUpdatedShortLabel() {
        return updatedShortLabel;
    }

    public void setUpdatedShortLabel(String updatedShortLabel) {
        this.updatedShortLabel = updatedShortLabel;
    }

    @Column(name = "fullname")
    public String getUpdatedFullName() {
        return updatedFullName;
    }

    public void setUpdatedFullName(String updatedFullName) {
        this.updatedFullName = updatedFullName;
    }

    @Column(name = "uniprot")
    public String getUniprotQuery() {
        return uniprotQuery;
    }

    public void setUniprotQuery(String uniprot) {
        this.uniprotQuery = uniprot;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final UniprotUpdateEvent event = ( UniprotUpdateEvent ) o;

        if ( updatedShortLabel != null ) {
            if (!updatedShortLabel.equals( event.getUpdatedShortLabel())){
                return false;
            }
        }
        else if (event.getUpdatedShortLabel()!= null){
            return false;
        }

        if ( updatedFullName != null ) {
            if (!updatedFullName.equals( event.getUpdatedFullName())){
                return false;
            }
        }
        else if (event.getUpdatedFullName()!= null){
            return false;
        }

        if ( uniprotQuery != null ) {
            if (!uniprotQuery.equals( event.getUniprotQuery())){
                return false;
            }
        }
        else if (event.getUniprotQuery()!= null){
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

        if ( updatedShortLabel != null ) {
            code = 29 * code + updatedShortLabel.hashCode();
        }

        if ( updatedFullName != null ) {
            code = 29 * code + updatedFullName.hashCode();
        }

        if ( uniprotQuery != null ) {
            code = 29 * code + uniprotQuery.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final UniprotUpdateEvent event = ( UniprotUpdateEvent ) o;

        if ( updatedShortLabel != null ) {
            if (!updatedShortLabel.equals( event.getUpdatedShortLabel())){
                return false;
            }
        }
        else if (event.getUpdatedShortLabel()!= null){
            return false;
        }

        if ( updatedFullName != null ) {
            if (!updatedFullName.equals( event.getUpdatedFullName())){
                return false;
            }
        }
        else if (event.getUpdatedFullName()!= null){
            return false;
        }

        if ( uniprotQuery != null ) {
            if (!uniprotQuery.equals( event.getUniprotQuery())){
                return false;
            }
        }
        else if (event.getUniprotQuery()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Uniprot update event : [ shortlabel = " + updatedShortLabel != null ? updatedShortLabel : "none" + ", fullname = " + updatedFullName != null ? updatedFullName : "none" + ", uniprot query = " + uniprotQuery != null ? uniprotQuery : "none");

        return buffer.toString();
    }

}
