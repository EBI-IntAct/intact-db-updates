package uk.ac.ebi.intact.update.model.protein.events;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Event for deleted proteins
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02/08/11</pre>
 */
@Entity
@DiscriminatorValue("deleted_protein")
public class DeletedProteinEvent extends PersistentProteinEvent{
    public DeletedProteinEvent() {
        super();
    }

    public DeletedProteinEvent(ProteinUpdateProcess process, Protein protein) {
        super(process, protein);
    }

    public DeletedProteinEvent(ProteinUpdateProcess process, Protein protein, String uniprotAc) {
        super(process, protein, uniprotAc);
    }

    public DeletedProteinEvent(ProteinUpdateProcess process, String proteinAc) {
        super(process, proteinAc);
    }

    public DeletedProteinEvent(ProteinUpdateProcess process, String proteinAc, String uniprotAc) {
        super(process, proteinAc, uniprotAc);
    }
}
