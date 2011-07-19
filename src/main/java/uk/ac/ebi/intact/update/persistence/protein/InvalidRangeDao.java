package uk.ac.ebi.intact.update.persistence.protein;

import uk.ac.ebi.intact.update.model.protein.update.events.range.PersistentInvalidRange;

import java.util.Date;
import java.util.List;

/**
 * This interface allows to query the database to get invalid ranges
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02-Dec-2010</pre>
 */

public interface InvalidRangeDao extends UpdatedRangeDao<PersistentInvalidRange>{

    public List<PersistentInvalidRange> getAllInvalidRanges();
    public List<PersistentInvalidRange> getAllOutOfDateRanges();
    public List<PersistentInvalidRange> getAllOutOfDateRangesWithoutSequenceVersion();
    public List<PersistentInvalidRange> getAllOutOfDateRangesWithSequenceVersion();

    public List<PersistentInvalidRange> getInvalidRanges(long processId);
    public List<PersistentInvalidRange> getOutOfDateRanges(long processId);
    public List<PersistentInvalidRange> getOutOfDateRangesWithoutSequenceVersion(long processId);
    public List<PersistentInvalidRange> getOutOfDateRangesWithSequenceVersion(long processId);

    public List<PersistentInvalidRange> getInvalidRanges(Date updateddate );
    public List<PersistentInvalidRange> getOutOfDateRanges(Date updateddate);
    public List<PersistentInvalidRange> getOutOfDateRangesWithoutSequenceVersion(Date updateddate);
    public List<PersistentInvalidRange> getOutOfDateRangesWithSequenceVersion(Date updateddate);

    public List<PersistentInvalidRange> getInvalidRangesBefore(Date updateddate );
    public List<PersistentInvalidRange> getOutOfDateRangesBefore(Date updateddate);
    public List<PersistentInvalidRange> getOutOfDateRangesWithoutSequenceVersionBefore(Date updateddate);
    public List<PersistentInvalidRange> getOutOfDateRangesWithSequenceVersionBefore(Date updateddate);

    public List<PersistentInvalidRange> getInvalidRangesAfter(Date updateddate );
    public List<PersistentInvalidRange> getOutOfDateRangesAfter(Date updateddate);
    public List<PersistentInvalidRange> getOutOfDateRangesWithoutSequenceVersionAfter(Date updateddate);
    public List<PersistentInvalidRange> getOutOfDateRangesWithSequenceVersionAfter(Date updateddate);
}
