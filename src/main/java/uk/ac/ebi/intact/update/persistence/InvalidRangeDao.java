package uk.ac.ebi.intact.update.persistence;

import uk.ac.ebi.intact.update.model.protein.update.events.range.InvalidRange;

import java.util.Date;
import java.util.List;

/**
 * This interface allows to query the database to get invalid ranges
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02-Dec-2010</pre>
 */

public interface InvalidRangeDao extends UpdatedRangeDao<InvalidRange>{

    public List<InvalidRange> getAllInvalidRanges();
    public List<InvalidRange> getAllOutOfDateRanges();
    public List<InvalidRange> getAllOutOfDateRangesWithoutSequenceVersion();
    public List<InvalidRange> getAllOutOfDateRangesWithSequenceVersion();

    public List<InvalidRange> getInvalidRanges(long processId);
    public List<InvalidRange> getOutOfDateRanges(long processId);
    public List<InvalidRange> getOutOfDateRangesWithoutSequenceVersion(long processId);
    public List<InvalidRange> getOutOfDateRangesWithSequenceVersion(long processId);

    public List<InvalidRange> getInvalidRanges(Date updateddate );
    public List<InvalidRange> getOutOfDateRanges(Date updateddate);
    public List<InvalidRange> getOutOfDateRangesWithoutSequenceVersion(Date updateddate);
    public List<InvalidRange> getOutOfDateRangesWithSequenceVersion(Date updateddate);

    public List<InvalidRange> getInvalidRangesBefore(Date updateddate );
    public List<InvalidRange> getOutOfDateRangesBefore(Date updateddate);
    public List<InvalidRange> getOutOfDateRangesWithoutSequenceVersionBefore(Date updateddate);
    public List<InvalidRange> getOutOfDateRangesWithSequenceVersionBefore(Date updateddate);

    public List<InvalidRange> getInvalidRangesAfter(Date updateddate );
    public List<InvalidRange> getOutOfDateAfter(Date updateddate);
    public List<InvalidRange> getOutOfDateRangesWithoutSequenceVersionAfter(Date updateddate);
    public List<InvalidRange> getOutOfDateRangesWithSequenceVersionAfter(Date updateddate);
}
