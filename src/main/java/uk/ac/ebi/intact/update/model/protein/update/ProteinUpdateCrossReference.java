package uk.ac.ebi.intact.update.model.protein.update;

import uk.ac.ebi.intact.model.Xref;
import uk.ac.ebi.intact.update.model.UpdateEvent;
import uk.ac.ebi.intact.update.model.UpdateStatus;
import uk.ac.ebi.intact.update.model.UpdatedCrossReference;
import uk.ac.ebi.intact.update.model.protein.update.events.PersistentProteinEvent;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Cross reference of a protein
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@Entity
@Table(name = "ia_protein_updated_xref")
public class ProteinUpdateCrossReference extends UpdatedCrossReference{

    public ProteinUpdateCrossReference(){
        super();
    }

    public ProteinUpdateCrossReference(Xref ref, UpdateStatus status){

        super(ref, status);
    }

    @Override
    @ManyToOne( targetEntity = PersistentProteinEvent.class )
    @JoinColumn( name = "event_id" )
    public UpdateEvent getParent() {
        return super.getParent();
    }
}
