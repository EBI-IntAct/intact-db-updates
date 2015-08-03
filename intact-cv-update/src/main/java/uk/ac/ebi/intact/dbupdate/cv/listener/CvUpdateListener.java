package uk.ac.ebi.intact.dbupdate.cv.listener;

import uk.ac.ebi.intact.dbupdate.cv.events.*;

import java.util.EventListener;

/**
 * Interface to implement for all listeners of CvUpdate
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>11/11/11</pre>
 */

public interface CvUpdateListener extends EventListener {

    public void onObsoleteRemappedTerm(ObsoleteRemappedEvent evt);

    public void onObsoleteTermImpossibleToRemap(ObsoleteTermImpossibleToRemapEvent evt);

    public void onUpdatedCvTerm(UpdatedEvent evt);

    public void onCreatedCvTerm(CreatedTermEvent evt);

    public void onUpdateError(UpdateErrorEvent evt);

    public void onDeletedCvTerm(DeletedTermEvent evt);
    }
