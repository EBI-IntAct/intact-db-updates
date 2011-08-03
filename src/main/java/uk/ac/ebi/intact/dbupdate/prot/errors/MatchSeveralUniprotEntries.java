package uk.ac.ebi.intact.dbupdate.prot.errors;

import uk.ac.ebi.intact.dbupdate.prot.UpdateError;

import java.util.HashSet;
import java.util.Set;

/**
 * Error for uniprot ac matching several uniprot entries
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */

public class MatchSeveralUniprotEntries extends MultiUniprotIdentities {

    protected String currentUniprotAc;
    protected String taxId;
    protected Set<String> uniprotFromDifferentOrganisms = new HashSet<String>();

    public MatchSeveralUniprotEntries(String proteinAc, String uniprotAc, String taxId, UpdateError errorLabel) {
        super(proteinAc, errorLabel);
        this.currentUniprotAc = uniprotAc;
        this.taxId = taxId;
    }

    public String getCurrentUniprotAc() {
        return currentUniprotAc;
    }

    public String getTaxId() {
        return taxId;
    }

    @Override
    public String getErrorMessage(){
        if (this.taxId == null || this.proteinAc == null){
            return "";
        }

        StringBuffer error = new StringBuffer();
        error.append("The protein (TaxId = "+taxId+"");
        error.append(proteinAc);
        error.append(" has a uniprot ac (");
        error.append(currentUniprotAc);
        error.append(" which can match " + this.uniprotIdentities.size());
        error.append(" different uniprot entries having same taxId : ");

        writeUniprotAcs(error);

        error.append(" and which can match " + this.uniprotFromDifferentOrganisms.size());
        error.append(" different uniprot entries having a different taxId : ");

        writeUniprotAcs(error, this.uniprotFromDifferentOrganisms);

        return error.toString();
    }
}
