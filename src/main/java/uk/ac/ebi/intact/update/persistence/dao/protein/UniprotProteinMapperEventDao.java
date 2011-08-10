package uk.ac.ebi.intact.update.persistence.dao.protein;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.protein.events.UniprotProteinMapperEvent;

import java.util.List;

/**
 * dao for protein mapping events
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Mockable
public interface UniprotProteinMapperEventDao extends ProteinEventDao<UniprotProteinMapperEvent> {

    public List<UniprotProteinMapperEvent> getSuccessfulUniprotMappingEvents(long processId);
    public List<UniprotProteinMapperEvent> getUnSuccessfulUniprotMappingEvents(long processId);
}
