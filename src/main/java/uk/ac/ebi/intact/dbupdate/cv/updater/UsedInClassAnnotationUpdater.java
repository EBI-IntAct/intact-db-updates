package uk.ac.ebi.intact.dbupdate.cv.updater;

import uk.ac.ebi.intact.dbupdate.cv.CvUpdateContext;
import uk.ac.ebi.intact.dbupdate.cv.events.UpdatedEvent;
import uk.ac.ebi.intact.model.CvDagObject;

/**
 * Interface for used in class annotation updater
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>20/01/12</pre>
 */

public interface UsedInClassAnnotationUpdater {

    public boolean canUpdate(CvDagObject cvObject);
    public void updateAnnotations(CvUpdateContext updateContext, UpdatedEvent updateEvt);
    public String getClassSeparator();
    public void clear();
}
