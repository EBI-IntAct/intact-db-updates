package uk.ac.ebi.intact.dbupdate.prot.errors;

import uk.ac.ebi.intact.dbupdate.prot.UpdateError;

/**
 * Error when a merge is impossible to do between two proteins
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */

public class ImpossibleMerge extends DefaultProteinUpdateError {

    String originalProtein;

    public ImpossibleMerge(String proteinAc, String originalProtein, String reason) {
        super(UpdateError.impossible_merge, reason, proteinAc);

        this.originalProtein = originalProtein;
    }

    public String getOriginalProtein() {
        return originalProtein;
    }

    public String getReason(){
        return this.errorMessage;
    }

    @Override
    public String getErrorMessage(){
        if (this.errorMessage == null || this.proteinAc == null || originalProtein == null){
            return super.getErrorMessage();
        }

        StringBuffer error = new StringBuffer();
        error.append("The protein ");
        error.append(proteinAc);
        error.append(" could not be merged with the protein ");
        error.append(originalProtein);
        error.append(" because ");
        error.append(this.errorMessage);

        return error.toString();
    }
}
