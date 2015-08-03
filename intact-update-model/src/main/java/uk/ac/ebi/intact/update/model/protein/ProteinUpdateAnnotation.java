package uk.ac.ebi.intact.update.model.protein;

import uk.ac.ebi.intact.model.Annotation;
import uk.ac.ebi.intact.update.model.UpdateEventImpl;
import uk.ac.ebi.intact.update.model.UpdateStatus;
import uk.ac.ebi.intact.update.model.protein.events.PersistentProteinEvent;

import javax.persistence.*;

/**
 * ProteinUpdateAnnotation of a protein
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@Entity
@Table(name = "ia_prot_updated_annot")
public class ProteinUpdateAnnotation extends UpdatedAnnotation {

    public ProteinUpdateAnnotation(){
        super();
    }

    public ProteinUpdateAnnotation(String topic, String text, UpdateStatus status){
        super(topic, text, status);
    }

    public ProteinUpdateAnnotation(Annotation annotation, UpdateStatus status){
        super(annotation, status);
    }

    @Override
    @ManyToOne( targetEntity = PersistentProteinEvent.class )
    @JoinColumn( name = "event_id" )
    public UpdateEventImpl getParent() {
        return super.getParent();
    }
}
