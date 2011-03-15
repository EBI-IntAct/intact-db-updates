package uk.ac.ebi.intact.update.model.protein.update.events;

import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.update.UpdateProcess;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Event for dead proteins
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@Entity
@DiscriminatorValue("DeadProteinEvent")
public class DeadProteinEvent extends ProteinEvent{

    private String uniprotReference;

    public DeadProteinEvent(){
        super();
        this.uniprotReference = null;
    }

    public DeadProteinEvent(UpdateProcess process, Protein protein, InteractorXref uniprotRef, int index){
        super(process, EventName.dead_protein, protein, index);

        setUniprotReference(uniprotRef);
    }

    @Column(name = "dead_uniprot", nullable = false)
    public String getUniprotReference() {
        return uniprotReference;
    }

    public void setUniprotReference(String uniprotReference) {
        this.uniprotReference = uniprotReference;
    }

    public void setUniprotReference(InteractorXref xRef){
        if (xRef != null){
            this.uniprotReference = xRef.getPrimaryId();
        }
    }
}
