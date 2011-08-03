package uk.ac.ebi.intact.dbupdate.prot.errors;

/**
 * Error for proteins having organism conflicts with uniprot
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */

public class OrganismConflict extends DefaultProteinUpdateError{

    String wrongTaxId;
    String uniprotTaxId;
    String uniprotAc;

    public OrganismConflict(String proteinAc, String wrongTaxId, String uniprotTaxId, String uniprotAc) {
        super(null, proteinAc);
        this.wrongTaxId = wrongTaxId;
        this.uniprotTaxId = uniprotTaxId;
        this.uniprotAc = uniprotAc;
    }

    public String getWrongTaxId() {
        return wrongTaxId;
    }

    public String getUniprotTaxId() {
        return uniprotTaxId;
    }

    public String getUniprotAc() {
        return uniprotAc;
    }

    @Override
    public String getErrorMessage(){
        if (this.proteinAc == null || this.wrongTaxId == null || this.uniprotTaxId == null || this.uniprotAc == null){
            return super.getErrorMessage();
        }

        StringBuffer error = new StringBuffer();
        error.append("The protein ");
        error.append(proteinAc);
        error.append(" refers to taxId ");
        error.append(wrongTaxId);
        error.append(" but is associated with uniprot entry ");
        error.append(this.uniprotAc);
        error.append(" which refers to a different taxId ") ;
        error.append(this.uniprotTaxId);;

        return error.toString();
    }
}
