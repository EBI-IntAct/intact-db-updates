package uk.ac.ebi.intact.dbupdate.cv.remapper;

import uk.ac.ebi.intact.dbupdate.cv.CvUpdateContext;
import uk.ac.ebi.intact.model.CvDagObject;

import java.util.Map;
import java.util.Set;

/**
 * Interface for classes which remaps an obsolete term to a valid term
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19/01/12</pre>
 */

public interface ObsoleteCvRemapper {

    public void remapObsoleteCvTerm(CvUpdateContext updateContext);
    public Map<String, Set<CvDagObject>> getRemappedCvToUpdate();
    public void clear();
}
