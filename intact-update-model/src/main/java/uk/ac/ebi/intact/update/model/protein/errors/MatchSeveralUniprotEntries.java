package uk.ac.ebi.intact.update.model.protein.errors;

import org.apache.commons.collections.CollectionUtils;
import uk.ac.ebi.intact.dbupdate.prot.errors.UniprotUpdateError;
import uk.ac.ebi.intact.dbupdate.prot.errors.UpdateError;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Error for proteins matching different uniprot entries
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>08/08/11</pre>
 */
@Entity
@DiscriminatorValue("match_several_uniprot")
public class MatchSeveralUniprotEntries extends MultiUniprotIdentities implements UniprotUpdateError {

    /**
     * the uniprot ac
     */
    protected String uniprotAc;

    /**
     * the taxId of the intact protein
     */
    protected String taxId;

    /**
     * the collection of uniprot acs poitning to a different taxid from the one of the intact protein
     */
    protected Set<String> uniprotFromDifferentOrganisms = new HashSet<String>();

    public MatchSeveralUniprotEntries(){
        super();
        this.uniprotAc = null;
        this.taxId = null;
    }

    public MatchSeveralUniprotEntries(ProteinUpdateProcess process, String proteinAc, String uniprotAc, String taxId, UpdateError errorLabel) {
        super(process, proteinAc, errorLabel);
        this.uniprotAc = uniprotAc;
        this.taxId = taxId;
    }

    @Column(name = "uniprot_ac")
    public String getUniprotAc() {
        return uniprotAc;
    }

    @Column(name = "intact_taxId")
    public String getTaxId() {
        return taxId;
    }

    @ElementCollection
    @JoinTable(name = "ia_err2uniprot_diff_organisms", joinColumns = @JoinColumn(name="error_id"))
    @Column(name = "uniprot")
    public Set<String> getUniprotFromDifferentOrganisms() {
        return uniprotFromDifferentOrganisms;
    }

    public void setUniprotAc(String uniprotAc) {
        this.uniprotAc = uniprotAc;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public void setUniprotFromDifferentOrganisms(Set<String> uniprotFromDifferentOrganisms) {
        this.uniprotFromDifferentOrganisms = uniprotFromDifferentOrganisms;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final MatchSeveralUniprotEntries event = (MatchSeveralUniprotEntries) o;

        if ( uniprotAc != null ) {
            if (!uniprotAc.equals( event.getUniprotAc())){
                return false;
            }
        }
        else if (event.getUniprotAc()!= null){
            return false;
        }

        if ( taxId != null ) {
            if (!taxId.equals( event.getTaxId())){
                return false;
            }
        }
        else if (event.getTaxId()!= null){
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

        if ( taxId != null ) {
            code = 29 * code + taxId.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final MatchSeveralUniprotEntries event = (MatchSeveralUniprotEntries) o;

        if ( uniprotAc != null ) {
            if (!uniprotAc.equals( event.getUniprotAc())){
                return false;
            }
        }
        else if (event.getUniprotAc()!= null){
            return false;
        }

        if ( taxId != null ) {
            if (!taxId.equals( event.getTaxId())){
                return false;
            }
        }
        else if (event.getTaxId()!= null){
            return false;
        }

        return CollectionUtils.isEqualCollection(uniprotFromDifferentOrganisms, event.getUniprotFromDifferentOrganisms());
    }

    @Override
    public String toString() {

        if (this.proteinAc == null){
            return "";
        }

        StringBuffer error = new StringBuffer();
        error.append("The protein (TaxId = "+(taxId != null ? taxId : "null")+"");
        error.append(proteinAc);
        error.append(" has a uniprot ac (");
        error.append(uniprotAc);
        error.append(" which can match " + this.uniprotIdentities.size());
        error.append(" different uniprot entries having same taxId : ");

        writeUniprotAcs(error);

        error.append(" and which can match " + this.uniprotFromDifferentOrganisms.size());
        error.append(" different uniprot entries having a different taxId : ");

        writeUniprotAcs(error, this.uniprotFromDifferentOrganisms);

        return error.toString();
    }

}
