package uk.ac.ebi.intact.update.model.protein.errors;

import uk.ac.ebi.intact.dbupdate.prot.errors.IntactUpdateError;
import uk.ac.ebi.intact.dbupdate.prot.errors.UpdateError;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Error for dead proteins in Intact
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>08/08/11</pre>
 */
@Entity
@DiscriminatorValue("dead_uniprot")
public class DeadUniprotAc extends DefaultPersistentUpdateError implements IntactUpdateError {

    /**
     * the uniprot ac which does not exist in uniprot
     */
    protected String deadUniprot;

    /**
     * The intact protein ac
     */
    protected String proteinAc;

    public DeadUniprotAc(){
        super(null, UpdateError.dead_uniprot_ac, null);
        this.deadUniprot = null;
        this.proteinAc = null;
    }

    public DeadUniprotAc(ProteinUpdateProcess parent, String proteinAc, String deadUniprot) {
        super(parent, UpdateError.dead_uniprot_ac, null);
        this.deadUniprot = deadUniprot;
        this.proteinAc = proteinAc;
    }

    public DeadUniprotAc(ProteinUpdateProcess parent, UpdateError errorLabel, String proteinAc, String deadUniprot) {
        super(parent, errorLabel, null);
        this.deadUniprot = deadUniprot;
        this.proteinAc = proteinAc;
    }

    @Column(name = "dead_ac")
    public String getDeadUniprot() {
        return deadUniprot;
    }

    @Override
    @Column(name = "protein_ac")
    public String getProteinAc() {
        return this.proteinAc;
    }

    public void setDeadUniprot(String deadUniprot) {
        this.deadUniprot = deadUniprot;
    }

    public void setProteinAc(String proteinAc) {
        this.proteinAc = proteinAc;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final DeadUniprotAc event = (DeadUniprotAc) o;

        if ( deadUniprot != null ) {
            if (!deadUniprot.equals( event.getDeadUniprot())){
                return false;
            }
        }
        else if (event.getDeadUniprot()!= null){
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

        if ( deadUniprot != null ) {
            code = 29 * code + deadUniprot.hashCode();
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

        final DeadUniprotAc event = (DeadUniprotAc) o;

        if ( deadUniprot != null ) {
            if (!deadUniprot.equals( event.getDeadUniprot())){
                return false;
            }
        }
        else if (event.getDeadUniprot()!= null){
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
        if (this.deadUniprot == null || this.proteinAc == null){
            return "";
        }

        StringBuffer error = new StringBuffer();
        error.append("The protein ");
        error.append(proteinAc);
        error.append(" refers to a dead uniprot ac ");
        error.append(deadUniprot);

        return error.toString();
    }
}
