package uk.ac.ebi.intact.dbupdate.prot.errors;

/**
 * This error is for the case of a dead uniprot ac and the option of fixing dead proteins has been set to false
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */

public class DeadUniprotAc extends DefaultProteinUpdateError implements IntactUpdateError{

    protected String deadUniprot;
    protected String proteinAc;

    public DeadUniprotAc(String proteinAc, String deadUniprot) {
        super(UpdateError.dead_uniprot_ac, null);
        this.deadUniprot = deadUniprot;
        this.proteinAc = proteinAc;
    }

    public DeadUniprotAc(UpdateError errorLabel, String proteinAc, String deadUniprot) {
        super(errorLabel, null);
        this.deadUniprot = deadUniprot;
        this.proteinAc = proteinAc;
    }

    public String getDeadUniprot() {
        return deadUniprot;
    }

    @Override
    public String getErrorMessage(){
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

    @Override
    public String getProteinAc() {
        return this.proteinAc;
    }
}
