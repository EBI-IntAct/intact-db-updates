package uk.ac.ebi.intact.update.persistence.proteinupdate;

import uk.ac.ebi.intact.update.model.proteinmapping.actions.ActionReport;
import uk.ac.ebi.intact.update.model.proteinupdate.ProteinEvent;
import uk.ac.ebi.intact.update.persistence.UpdateBaseDao;

import java.io.Serializable;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02-Dec-2010</pre>
 */

public interface ProteinEventDao<T extends ProteinEvent> extends UpdateBaseDao<T>, Serializable {
        
}
