package uk.ac.ebi.intact.update.model.proteinupdate;

import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.update.model.HibernatePersistentImpl;
import uk.ac.ebi.intact.update.model.proteinmapping.results.IdentificationResults;
import uk.ac.ebi.intact.update.model.proteinmapping.results.UpdateResults;
import uk.ac.ebi.intact.update.model.proteinupdate.protein.Annotation;
import uk.ac.ebi.intact.update.model.proteinupdate.protein.IntactProtein;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
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
