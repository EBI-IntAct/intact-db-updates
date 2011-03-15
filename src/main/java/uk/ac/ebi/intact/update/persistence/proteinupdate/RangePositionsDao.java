package uk.ac.ebi.intact.update.persistence.proteinupdate;

import uk.ac.ebi.intact.update.model.protein.update.events.range.RangePositions;
import uk.ac.ebi.intact.update.persistence.UpdateBaseDao;

import java.io.Serializable;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02-Dec-2010</pre>
 */

public interface RangePositionsDao<T extends RangePositions> extends UpdateBaseDao<T>, Serializable {
}
