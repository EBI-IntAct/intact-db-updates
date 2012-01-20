package uk.ac.ebi.intact.dbupdate.cv.updater;

import uk.ac.ebi.intact.dbupdate.cv.CvUpdateContext;
import uk.ac.ebi.intact.dbupdate.cv.events.UpdatedEvent;
import uk.ac.ebi.intact.model.CvDagObject;

import java.util.Map;
import java.util.Set;

/**
 * Interface for a cv updater
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19/01/12</pre>
 */

public interface CvUpdater {

    public void updateTerm(CvUpdateContext updateContext) throws CvUpdateException;
    public void updateParents(CvUpdateContext updateContext, UpdatedEvent updateEvt);
    public void updateAnnotations(CvUpdateContext updateContext, UpdatedEvent updateEvt);
    public void updateXrefs(CvUpdateContext updateContext, UpdatedEvent updateEvt);
    public void updateAliases(CvUpdateContext updateContext, UpdatedEvent updateEvt);

    public Map<String, Set<CvDagObject>> getMissingParents();
    public Set<String> getProcessedTerms();
    public CvParentUpdater getCvParentUpdater();
    public CvAliasUpdater getCvAliasUpdater();
    public CvXrefUpdater getCvXrefUpdater();
    public CvAnnotationUpdater getCvAnnotationUpdater();
    public UsedInClassAnnotationUpdater getUsedInClassAnnotationUpdater();

    public void clear();
}
