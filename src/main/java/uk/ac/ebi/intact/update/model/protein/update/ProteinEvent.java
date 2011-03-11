package uk.ac.ebi.intact.update.model.protein.update;

import uk.ac.ebi.intact.update.model.HibernatePersistentImpl;

import javax.persistence.*;
import java.util.Date;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="objClass", discriminatorType=DiscriminatorType.STRING)
@DiscriminatorValue("ProteinEvent")
@Table(name = "ia_protein_event")
public class ProteinEvent extends HibernatePersistentImpl {

    EventName name;
    int index;

    public ProteinEvent(){
        super();
        this.name = EventName.uniprot_update;
        this.index = 0;
    }

    public ProteinEvent(EventName name, Date created, int index){
        super();
        this.name = name;
        setCreated(created);
        this.index = index;
    }

    @Column(name = "name", nullable = false)
    @Enumerated(EnumType.STRING)
    public EventName getName() {
        return name;
    }

    public void setName(EventName name) {
        this.name = name;
    }

    @Column(name = "index", nullable = false)
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
