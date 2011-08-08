package uk.ac.ebi.intact.dbupdate.prot.errors;

/**
 * Error for master proteins impossible to update
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */

public class ImpossibleUpdateMaster extends DefaultProteinUpdateError implements UniprotUpdateError {

    private String uniprotAc;

    public ImpossibleUpdateMaster(String errorMessage, String uniprot) {
        super(UpdateError.impossible_update_master, errorMessage);
        this.uniprotAc = uniprot;
    }

    public ImpossibleUpdateMaster(UpdateError errorLabel, String errorMessage, String uniprot) {
        super(errorLabel, errorMessage);
        this.uniprotAc = uniprot;
    }

    @Override
    public String getUniprotAc() {
        return this.uniprotAc;
    }
}
