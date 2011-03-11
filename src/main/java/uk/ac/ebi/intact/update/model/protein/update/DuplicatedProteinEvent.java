package uk.ac.ebi.intact.update.model.protein.update;

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
@DiscriminatorValue("DuplicatedProteinEvent")
public class DuplicatedProteinEvent extends ProteinEvent{

    private String originalProteinAc;

    public DuplicatedProteinEvent(){
        super();
        this.originalProteinAc = null;
    }

    public DuplicatedProteinEvent(Date created, String originalProteinAc, int index){
        super(EventName.protein_duplicate, created, index);
        this.originalProteinAc = originalProteinAc;
    }

    @Column(name = "original_ac", nullable = false)
    public String getOriginalProteinAc() {
        return originalProteinAc;
    }

    public void setOriginalProteinAc(String originalProteinAc) {
        this.originalProteinAc = originalProteinAc;
    }
}
