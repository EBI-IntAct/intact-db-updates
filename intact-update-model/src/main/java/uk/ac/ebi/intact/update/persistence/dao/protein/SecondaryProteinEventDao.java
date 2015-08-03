package uk.ac.ebi.intact.update.persistence.dao.protein;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.protein.events.SecondaryProteinEvent;

import java.util.List;

/**
 * Dao for secondary protein events
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Mockable
public interface SecondaryProteinEventDao extends ProteinEventDao<SecondaryProteinEvent> {

    public List<SecondaryProteinEvent> getSecondaryProteinEventsBySecondaryAc(long processId, String secondary);
}
