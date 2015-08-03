package uk.ac.ebi.intact.update.model.protein.errors;

import org.apache.commons.collections.CollectionUtils;
import uk.ac.ebi.intact.dbupdate.prot.errors.IntactUpdateError;
import uk.ac.ebi.intact.dbupdate.prot.errors.UpdateError;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;

import javax.persistence.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Error for proteins having several uniprot identities
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>08/08/11</pre>
 */
@Entity
@DiscriminatorValue("multi_uniprot")
public class MultiUniprotIdentities extends DefaultPersistentUpdateError  implements IntactUpdateError{

    /**
     * The collection of uniprot acs that the intact protein refers to
     */
    protected Set<String> uniprotIdentities = new HashSet<String>();

    /**
     * The intact protein ac
     */
    protected String proteinAc;

    public MultiUniprotIdentities(){
        super(null, UpdateError.multi_uniprot_identities, null);
        this.proteinAc = null;
    }

    public MultiUniprotIdentities(ProteinUpdateProcess process, String proteinAc) {
        super(process, UpdateError.multi_uniprot_identities, null);
        this.proteinAc = proteinAc;
    }

    public MultiUniprotIdentities(ProteinUpdateProcess process, String proteinAc, UpdateError errorLabel) {
        super(process, errorLabel, null);
        this.proteinAc = proteinAc;
    }

    @ElementCollection
    @JoinTable(name = "ia_err2uniprot", joinColumns = @JoinColumn(name="error_id"))
    @Column(name = "uniprot")
    public Set<String> getUniprotIdentities() {
        return uniprotIdentities;
    }

    @Override
    @Column(name = "protein_ac")
    public String getProteinAc() {
        return this.proteinAc;
    }

    public void setUniprotIdentities(Set<String> uniprotIdentities) {
        this.uniprotIdentities = uniprotIdentities;
    }

    public void setProteinAc(String proteinAc) {
        this.proteinAc = proteinAc;
    }

    protected void writeUniprotAcs(StringBuffer error) {
        int i =0;

        for (String uniprot : uniprotIdentities){
            error.append(uniprot);

            if (i < uniprotIdentities.size()){
                error.append(", ");
            }
            i++;
        }
    }

    protected static void writeUniprotAcs(StringBuffer error, Collection<String> uniprotAcs) {
        int i =0;

        for (String uniprot : uniprotAcs){
            error.append(uniprot);

            if (i < uniprotAcs.size()){
                error.append(", ");
            }
            i++;
        }
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final MultiUniprotIdentities event = (MultiUniprotIdentities) o;

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

        final MultiUniprotIdentities event = (MultiUniprotIdentities) o;

        if ( proteinAc != null ) {
            if (!proteinAc.equals( event.getProteinAc())){
                return false;
            }
        }
        else if (event.getProteinAc()!= null){
            return false;
        }

        return CollectionUtils.isEqualCollection(uniprotIdentities, event.getUniprotIdentities());
    }

    @Override
    public String toString() {

        if (this.uniprotIdentities.isEmpty() || this.proteinAc == null){
            return "";
        }

        StringBuffer error = new StringBuffer();
        error.append("The protein ");
        error.append(proteinAc);
        error.append(" has " + this.uniprotIdentities.size());
        error.append(" uniprot identities : ");

        writeUniprotAcs(error);

        return error.toString();
    }
}
