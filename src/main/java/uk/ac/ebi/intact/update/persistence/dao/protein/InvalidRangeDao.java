package uk.ac.ebi.intact.update.persistence.dao.protein;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.protein.range.PersistentInvalidRange;

import java.util.Date;
import java.util.List;

/**
 * This interface allows to query the database to get invalid ranges
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02-Dec-2010</pre>
 */
@Mockable
public interface InvalidRangeDao extends UpdatedRangeDao<PersistentInvalidRange>{

    public List<PersistentInvalidRange> getAllInvalidRanges();
    public List<PersistentInvalidRange> getAllOutOfDateRanges();

    public List<PersistentInvalidRange> getInvalidRanges(long processId);
    public List<PersistentInvalidRange> getOutOfDateRanges(long processId);

    public List<PersistentInvalidRange> getInvalidRanges(Date updateddate );
    public List<PersistentInvalidRange> getOutOfDateRanges(Date updateddate);

    public List<PersistentInvalidRange> getInvalidRangesBefore(Date updateddate );
    public List<PersistentInvalidRange> getOutOfDateRangesBefore(Date updateddate);

    public List<PersistentInvalidRange> getInvalidRangesAfter(Date updateddate );
    public List<PersistentInvalidRange> getOutOfDateRangesAfter(Date updateddate);
}
