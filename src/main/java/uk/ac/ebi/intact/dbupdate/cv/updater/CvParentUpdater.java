package uk.ac.ebi.intact.dbupdate.cv.updater;

import uk.ac.ebi.intact.dbupdate.cv.CvUpdateContext;
import uk.ac.ebi.intact.dbupdate.cv.events.UpdatedEvent;
import uk.ac.ebi.intact.model.CvDagObject;

import java.util.Map;
import java.util.Set;

/**
 * Interface for parent updater
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19/01/12</pre>
 */

public interface CvParentUpdater {

    public void updateParents(CvUpdateContext updateContext, UpdatedEvent updateEvt);
    public void clear();
    public Map<String, Set<CvDagObject>> getMissingParents();
}
