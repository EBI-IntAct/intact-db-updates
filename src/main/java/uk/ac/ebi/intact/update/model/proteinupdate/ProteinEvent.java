package uk.ac.ebi.intact.update.model.proteinupdate;

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

    public ProteinEvent(){
        super();
        this.name = EventName.uniprot_update;
    }

    public ProteinEvent(EventName name, Date created){
        super();
        this.name = name;
        setCreated(created);
    }

    @Column(name = "name", nullable = false)
    @Enumerated(EnumType.STRING)
    public EventName getName() {
        return name;
    }

    public void setName(EventName name) {
        this.name = name;
    }
}
