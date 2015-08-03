package uk.ac.ebi.intact.update.model.protein.events;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Event for created proteins
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02/08/11</pre>
 */
@Entity
@DiscriminatorValue("created_protein")
public class CreatedProteinEvent extends PersistentProteinEvent{

    public CreatedProteinEvent() {
        super();
    }

    public CreatedProteinEvent(ProteinUpdateProcess process, Protein protein) {
        super(process, protein);
    }

    public CreatedProteinEvent(ProteinUpdateProcess process, Protein protein, String uniprotAc) {
        super(process, protein, uniprotAc);
    }

    public CreatedProteinEvent(ProteinUpdateProcess process, String proteinAc) {
        super(process, proteinAc);
    }

    public CreatedProteinEvent(ProteinUpdateProcess process, String proteinAc, String uniprotAc) {
        super(process, proteinAc, uniprotAc);
    }
}
