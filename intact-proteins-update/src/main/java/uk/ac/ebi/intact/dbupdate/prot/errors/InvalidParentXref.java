package uk.ac.ebi.intact.dbupdate.prot.errors;

/**
 * Error for invalid parent xrefs
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */

public class InvalidParentXref extends DefaultProteinUpdateError implements IntactUpdateError {

    private String invalidParent;
    private String proteinAc;

    public InvalidParentXref(String proteinAc, String invalidParent, String reason) {
        super(UpdateError.invalid_parent_xref,reason);
        this.invalidParent = invalidParent;
        this.proteinAc = proteinAc;
    }

    public String getInvalidParent() {
        return invalidParent;
    }

    public String getReason(){
        return this.errorMessage;
    }

    @Override
    public String getErrorMessage(){
        if (this.invalidParent.isEmpty() || this.proteinAc == null){
            return this.errorMessage;
        }

        return "The protein " + proteinAc + " has a parent cross reference to the protein " + invalidParent +
                " which is not valid because " + this.errorMessage;
    }

    @Override
    public String getProteinAc() {
        return this.proteinAc;
    }
}