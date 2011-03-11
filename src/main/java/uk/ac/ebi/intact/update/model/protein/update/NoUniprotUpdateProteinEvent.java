package uk.ac.ebi.intact.update.model.protein.update;

import uk.ac.ebi.intact.update.model.protein.mapping.results.UpdateResults;

import javax.persistence.*;
import java.util.Date;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>01-Dec-2010</pre>
 */

public class NoUniprotUpdateProteinEvent extends ProteinEvent{

    private UpdateResults identificationResult;

    public NoUniprotUpdateProteinEvent(){
        super();
        this.identificationResult = null;
    }

    public NoUniprotUpdateProteinEvent(Date created, UpdateResults result, int index){
        super(EventName.non_uniprot_protein, created, index);
        this.identificationResult = result;
    }

    @OneToOne
    @JoinColumn(name = "identification_result_id")
    public UpdateResults getIdentificationResult() {
        return identificationResult;
    }

    public void setIdentificationResult(UpdateResults identificationResult) {
        this.identificationResult = identificationResult;
    }

}
