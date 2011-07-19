package uk.ac.ebi.intact.update.model;

import java.io.Serializable;

/**
 * The interface to implement for the classes we want to persist
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>20-May-2010</pre>
 */

public interface HibernateUpdatePersistent extends Serializable {

    /**
     *
     * @return an unique identifier for this object in the database
     */
    public Long getId();

    /**
     * Set the unique identifier
     * @param id
     */
    public void setId(Long id);

    public boolean isIdenticalTo(Object o);
}
