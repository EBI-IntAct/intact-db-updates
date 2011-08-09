package uk.ac.ebi.intact.update.model.protein.errors;

import uk.ac.ebi.intact.dbupdate.prot.errors.UpdateError;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Errors for proteins impossible to delete
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>08/08/11</pre>
 */
@Entity
@DiscriminatorValue("impossible_delete")
public class ImpossibleToDelete extends DefaultPersistentUpdateError{

    /**
     * the intact protein shortlabel
     */
    private String proteinLabel;

    public ImpossibleToDelete(){
        super(null, UpdateError.protein_impossible_to_delete, null);
        this.proteinLabel = null;
    }

    public ImpossibleToDelete(ProteinUpdateProcess process, String errorMessage, String proteinLabel) {
        super(process, UpdateError.protein_impossible_to_delete, errorMessage);
        this.proteinLabel = proteinLabel;
    }

    @Column(name = "protein_label")
    public String getProteinLabel() {
        return proteinLabel;
    }

    public void setProteinLabel(String proteinLabel) {
        this.proteinLabel = proteinLabel;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final ImpossibleToDelete event = (ImpossibleToDelete) o;

        if ( proteinLabel != null ) {
            if (!proteinLabel.equals( event.getProteinLabel())){
                return false;
            }
        }
        else if (event.getProteinLabel()!= null){
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

        if ( proteinLabel != null ) {
            code = 29 * code + proteinLabel.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final ImpossibleToDelete event = (ImpossibleToDelete) o;

        if ( proteinLabel != null ) {
            if (!proteinLabel.equals( event.getProteinLabel())){
                return false;
            }
        }
        else if (event.getProteinLabel()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {

        return reason;
    }
}
