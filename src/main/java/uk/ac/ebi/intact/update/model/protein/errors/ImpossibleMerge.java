package uk.ac.ebi.intact.update.model.protein.errors;

import uk.ac.ebi.intact.dbupdate.prot.errors.*;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Error for proteins impossible to merge
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>08/08/11</pre>
 */
@Entity
@DiscriminatorValue("impossible_merge")
public class ImpossibleMerge extends DefaultPersistentUpdateError implements IntactUpdateError, UniprotUpdateError {
    /**
     * The intact protein ac of the original protein
     */
    private String originalProtein;

    /**
     * the uniprot ac of the duplicate
     */
    private String uniprotAc;

    /**
     * The intact protein ac of the duplicate
     */
    private String proteinAc;

    public ImpossibleMerge(){
        super(null, UpdateError.impossible_merge, null);
        this.originalProtein = null;
        this.uniprotAc = null;
        this.proteinAc = null;
    }

    public ImpossibleMerge(ProteinUpdateProcess process, String proteinAc, String originalProtein, String uniprotAc, String reason) {
        super(process, UpdateError.impossible_merge, reason);

        this.proteinAc = proteinAc;
        this.originalProtein = originalProtein;
        this.uniprotAc = uniprotAc;
    }

    @Column(name = "original_protein")
    public String getOriginalProtein() {
        return originalProtein;
    }

    @Override
    @Column(name = "protein_ac")
    public String getProteinAc() {
        return this.proteinAc;
    }

    @Override
    @Column(name = "uniprot_ac")
    public String getUniprotAc() {
        return this.uniprotAc;
    }

    public void setOriginalProtein(String originalProtein) {
        this.originalProtein = originalProtein;
    }

    public void setUniprotAc(String uniprotAc) {
        this.uniprotAc = uniprotAc;
    }

    public void setProteinAc(String proteinAc) {
        this.proteinAc = proteinAc;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final ImpossibleMerge event = (ImpossibleMerge) o;

        if ( originalProtein != null ) {
            if (!originalProtein.equals( event.getOriginalProtein())){
                return false;
            }
        }
        else if (event.getOriginalProtein()!= null){
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

        if ( originalProtein != null ) {
            code = 29 * code + originalProtein.hashCode();
        }

        if ( proteinAc != null ) {
            code = 29 * code + proteinAc.hashCode();
        }

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

        final ImpossibleMerge event = (ImpossibleMerge) o;

        if ( originalProtein != null ) {
            if (!originalProtein.equals( event.getOriginalProtein())){
                return false;
            }
        }
        else if (event.getOriginalProtein()!= null){
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
        if (this.reason == null || (this.proteinAc == null && this.uniprotAc == null)){
            return super.getReason();
        }

        StringBuffer error = new StringBuffer();
        error.append("The protein ");
        error.append(proteinAc != null ? proteinAc : "");
        error.append(" having uniprot ac "+uniprotAc);
        error.append(" could not be merged ");

        if (originalProtein != null){
            error.append(" with the protein ");
            error.append(originalProtein);
        }

        error.append(" because ");
        error.append(this.reason);

        return error.toString();
    }
}
