package uk.ac.ebi.intact.dbupdate.cv.updater;

import uk.ac.ebi.intact.dbupdate.cv.CvUpdateContext;
import uk.ac.ebi.intact.dbupdate.cv.events.UpdatedEvent;

/**
 * Interface for annotation updater
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19/01/12</pre>
 */

public interface CvAnnotationUpdater {

    public void updateAnnotations(CvUpdateContext updateContext, UpdatedEvent updateEvt);
    public void clear();
}
