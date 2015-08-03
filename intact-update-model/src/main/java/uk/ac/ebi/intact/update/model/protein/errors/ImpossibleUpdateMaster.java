package uk.ac.ebi.intact.update.model.protein.errors;

import uk.ac.ebi.intact.dbupdate.prot.errors.UniprotUpdateError;
import uk.ac.ebi.intact.dbupdate.prot.errors.UpdateError;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Errors for master proteins impossible to update
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>08/08/11</pre>
 */
@Entity
@DiscriminatorValue("impossible_update_master")
public class ImpossibleUpdateMaster extends DefaultPersistentUpdateError implements UniprotUpdateError {

    /**
     * the uniprot ac
     */
    private String uniprotAc;

    public ImpossibleUpdateMaster(){
        super(null, UpdateError.impossible_update_master, null);
        this.uniprotAc = null;
    }

    public ImpossibleUpdateMaster(ProteinUpdateProcess process, String errorMessage, String uniprot) {
        super(process, UpdateError.impossible_update_master, errorMessage);
        this.uniprotAc = uniprot;
    }

    public ImpossibleUpdateMaster(ProteinUpdateProcess process, UpdateError errorLabel, String errorMessage, String uniprot) {
        super(process, errorLabel, errorMessage);
        this.uniprotAc = uniprot;
    }

    @Override
    @Column(name = "uniprot_ac")
    public String getUniprotAc() {
        return this.uniprotAc;
    }

    public void setUniprotAc(String uniprotAc) {
        this.uniprotAc = uniprotAc;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final ImpossibleUpdateMaster event = (ImpossibleUpdateMaster) o;

        if ( uniprotAc != null ) {
            if (!uniprotAc.equals( event.getUniprotAc())){
                return false;
            }
        }
        else if (event.getUniprotAc()!= null){
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

        if ( uniprotAc != null ) {
            code = 29 * code + uniprotAc.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final ImpossibleUpdateMaster event = (ImpossibleUpdateMaster) o;

        if ( uniprotAc != null ) {
            if (!uniprotAc.equals( event.getUniprotAc())){
                return false;
            }
        }
        else if (event.getUniprotAc()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {

        return reason;
    }
}
