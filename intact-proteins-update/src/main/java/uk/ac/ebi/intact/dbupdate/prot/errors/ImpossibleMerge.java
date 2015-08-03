package uk.ac.ebi.intact.dbupdate.prot.errors;

/**
 * Error when a merge is impossible to do between two proteins
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */

public class ImpossibleMerge extends DefaultProteinUpdateError implements IntactUpdateError, UniprotUpdateError {

    private String originalProtein;
    private String uniprotAc;
    private String proteinAc;

    public ImpossibleMerge(String proteinAc, String originalProtein, String uniprotAc, String reason) {
        super(UpdateError.impossible_merge, reason);

        this.proteinAc = proteinAc;
        this.originalProtein = originalProtein;
        this.uniprotAc = uniprotAc;
    }

    public String getOriginalProtein() {
        return originalProtein;
    }

    public String getReason(){
        return this.errorMessage;
    }

    @Override
    public String getErrorMessage(){
        if (this.errorMessage == null || (this.proteinAc == null && this.uniprotAc == null)){
            return super.getErrorMessage();
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
        error.append(this.errorMessage);

        return error.toString();
    }

    @Override
    public String getProteinAc() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getUniprotAc() {
        return this.uniprotAc;
    }
}
