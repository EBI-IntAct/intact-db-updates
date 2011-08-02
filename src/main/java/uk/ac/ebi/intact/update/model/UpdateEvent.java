package uk.ac.ebi.intact.update.model;

/**
 * Interface for update events
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02/08/11</pre>
 */

public interface UpdateEvent extends HibernateUpdatePersistent{

    public UpdateProcess getParent();
}
