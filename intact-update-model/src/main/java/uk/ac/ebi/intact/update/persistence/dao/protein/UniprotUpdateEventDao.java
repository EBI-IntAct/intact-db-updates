package uk.ac.ebi.intact.update.persistence.dao.protein;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.protein.events.UniprotUpdateEvent;

import java.util.List;

/**
 * Dao for uniprot update events
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Mockable
public interface UniprotUpdateEventDao extends ProteinEventDao<UniprotUpdateEvent> {

    public List<UniprotUpdateEvent> getUniprotUpdateEventWithUpdatedShortLabel(long processId);
    public List<UniprotUpdateEvent> getUniprotUpdateEventWithUpdatedFullName(long processId);
    public List<UniprotUpdateEvent> getUniprotUpdateEventWithUniprotQuery(long processId, String query);
    public List<UniprotUpdateEvent> getUniprotUpdateEventWithUpdatedXrefs(long processId);
    public List<UniprotUpdateEvent> getUniprotUpdateEventWithUpdatedAnnotations(long processId);
    public List<UniprotUpdateEvent> getUniprotUpdateEventWithUpdatedAliases(long processId);
}
