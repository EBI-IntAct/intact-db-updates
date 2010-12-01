package uk.ac.ebi.intact.update.model.proteinupdate;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.Date;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>25-Nov-2010</pre>
 */
@Entity
@DiscriminatorValue("SecondaryProteinEvent")
public class SecondaryProteinEvent extends ProteinEvent{

    String secondaryAc;
    String primaryAc;

    public SecondaryProteinEvent(){
        super();
        this.secondaryAc = null;
        this.primaryAc = null;

    }

    public SecondaryProteinEvent(Date created, String secondaryAc, String primaryAc, int index){
        super(EventName.secondary_protein, created, index);
        this.primaryAc = primaryAc;
        this.secondaryAc = secondaryAc;
    }

    @Column(name = "secondary_ac", nullable = false)
    public String getSecondaryAc() {
        return secondaryAc;
    }

    public void setSecondaryAc(String secondaryAc) {
        this.secondaryAc = secondaryAc;
    }

    @Column(name = "primary_ac", nullable = false)
    public String getPrimaryAc() {
        return primaryAc;
    }

    public void setPrimaryAc(String primaryAc) {
        this.primaryAc = primaryAc;
    }
}
