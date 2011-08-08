package uk.ac.ebi.intact.dbupdate.prot.errors;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Error for protein transcripts having feature chain and isoform parents
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */

public class InvalidCollectionOfParents extends DefaultProteinUpdateError implements IntactUpdateError{

    private Set<String> isoformParents = new HashSet<String>();
    private Set<String> chainParents = new HashSet<String>();
    private String proteinAc;

    public InvalidCollectionOfParents(String proteinAc, UpdateError errorLabel) {
        super(errorLabel, null);
        this.proteinAc = proteinAc;
    }

    public Collection<String> getIsoformParents() {
        return isoformParents;
    }

    public Collection<String> getChainParents() {
        return chainParents;
    }

    @Override
    public String getErrorMessage(){
        if (this.proteinAc == null){
            return "";
        }

        StringBuffer error = new StringBuffer();
        error.append("The protein ");
        error.append(proteinAc);
        error.append(" has " + this.isoformParents.size());
        error.append(" isoform parents : ");

        writeIntactAcs(error, this.isoformParents);

        error.append(" and has " + this.chainParents.size());
        error.append(" chain parents : ");

        writeIntactAcs(error, this.chainParents);

        return error.toString();
    }

    protected static void writeIntactAcs(StringBuffer error, Collection<String> acs) {
        int i =0;

        for (String uniprot : acs){
            error.append(uniprot);

            if (i < acs.size()){
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
