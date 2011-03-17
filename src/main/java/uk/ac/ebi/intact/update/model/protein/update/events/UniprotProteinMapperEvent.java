package uk.ac.ebi.intact.update.model.protein.update.events;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.mapping.results.UpdateMappingResults;
import uk.ac.ebi.intact.update.model.protein.update.UpdateProcess;

import javax.persistence.CascadeType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

/**
 * Event for remapping of non uniprot proteins
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>16/03/11</pre>
 */

public class UniprotProteinMapperEvent extends ProteinEvent{

    UpdateMappingResults identificationResults;

    public UniprotProteinMapperEvent(){
        super();
    }

    public UniprotProteinMapperEvent(UpdateProcess process, EventName name, Protein protein, int index, UpdateMappingResults identificationResults){
        super(process, name, protein, index);

        this.identificationResults = identificationResults;
    }

    @OneToOne(orphanRemoval = true, cascade = {
        CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
    @JoinColumn(name = "identification_id")
    public UpdateMappingResults getIdentificationResults() {
        return identificationResults;
    }

    public void setIdentificationResults(UpdateMappingResults identificationResults) {
        this.identificationResults = identificationResults;
    }
}
