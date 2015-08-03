package uk.ac.ebi.intact.dbupdate.prot.errors;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Error when we have several uniprot identities
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */

public class MultiUniprotIdentities extends DefaultProteinUpdateError implements IntactUpdateError {

    protected Set<String> uniprotIdentities = new HashSet<String>();
    protected String proteinAc;

    public MultiUniprotIdentities(String proteinAc) {
        super(UpdateError.multi_uniprot_identities, null);
        this.proteinAc = proteinAc;
    }

    public MultiUniprotIdentities(String proteinAc, UpdateError errorLabel) {
        super(errorLabel, null);
        this.proteinAc = proteinAc;
    }

    public Set<String> getUniprotIdentities() {
        return uniprotIdentities;
    }

    @Override
    public String getErrorMessage(){
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
    public String getProteinAc() {
        return this.proteinAc;
    }
}
