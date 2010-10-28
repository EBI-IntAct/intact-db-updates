package uk.ac.ebi.intact.update.model;

import javax.persistence.*;
import java.util.Date;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-Oct-2010</pre>
 */
@MappedSuperclass
public abstract class HibernatePersistentImpl {

    private long id;
    private Date created;

    public HibernatePersistentImpl(){
        created = null;
    }
    /**
     *
     * @return an automatically generated unique id for this object
     */
    @Id
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="SEQ_STORE")
    @SequenceGenerator(name="SEQ_STORE", sequenceName="my_sequence" )
    public Long getId() {
        return id;
    }

    /**
     * Set the unique id of this object
     * @param id : unique id
     */
    public void setId(Long id) {
        this.id = id;
    }

    @Temporal( value = TemporalType.TIMESTAMP )
    @Column(name = "created", nullable = false)
    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }    

}
