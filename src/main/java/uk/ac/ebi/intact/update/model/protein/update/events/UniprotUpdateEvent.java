package uk.ac.ebi.intact.update.model.protein.update.events;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.update.UpdateProcess;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Event for the basic update of a uniprot protein
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@Entity
@DiscriminatorValue("UniprotUpdateEvent")
public class UniprotUpdateEvent extends ProteinEvent{

    String shortLabel;
    String fullName;

    public UniprotUpdateEvent(){
        super();
        this.shortLabel = null;
        this.fullName = null;
    }

    public UniprotUpdateEvent(UpdateProcess updateProcess, Protein protein, int index, String shortlabel, String fullname){
        super(updateProcess, EventName.uniprot_update, protein, index);
        this.shortLabel = shortlabel;
        this.fullName = fullname;
    }

    @Column(name = "shortlabel", nullable = false)
    public String getShortLabel() {
        return shortLabel;
    }

    public void setShortLabel(String shortLabel) {
        this.shortLabel = shortLabel;
    }

    @Column(name = "fullname", nullable = true)
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
