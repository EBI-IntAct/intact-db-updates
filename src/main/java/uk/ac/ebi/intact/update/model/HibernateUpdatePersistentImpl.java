package uk.ac.ebi.intact.update.model;

import javax.persistence.*;

/**
 * An abstract super class for persistence
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-Oct-2010</pre>
 */
@MappedSuperclass
public abstract class HibernateUpdatePersistentImpl implements HibernateUpdatePersistent {

    private long id;

    public HibernateUpdatePersistentImpl(){
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

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof HibernateUpdatePersistentImpl) ) {
            return false;
        }

        final HibernateUpdatePersistentImpl persistent = (HibernateUpdatePersistentImpl) o;

        if ( id != persistent.getId() ) {
            return false;
        }

        return true;
    }

    /**
     * This class overwrites equals. To ensure proper functioning of HashTable,
     * hashCode must be overwritten, too.
     *
     * @return hash code of the object.
     */
    @Override
    public int hashCode() {

        int code = 29;

        code = 29 * code + Long.toString(id).hashCode();

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof HibernateUpdatePersistentImpl) ) {
            return false;
        }

        return true;
    }
}
