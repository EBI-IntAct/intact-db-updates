package uk.ac.ebi.intact.update.persistence;

import uk.ac.ebi.intact.update.model.protein.update.events.range.PersistentUpdatedRange;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * This interface allows to query the database to get invalid ranges
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02-Dec-2010</pre>
 */

public interface UpdatedRangeDao<T extends PersistentUpdatedRange> extends UpdateBaseDao<T>, Serializable {

    public List<T> getUpdatedRangesByRangeAc(String rangeAc);
    public List<T> getUpdatedRangesByComponentAc(String componentAc);
    public List<T> getUpdatedRangesByUpdateProcessId(long processId);
    public List<T> getUpdatedRangesByUpdateDate(Date updateDate);
    public List<T> getUpdatedRangesHavingUpdatedFeatureAnnotations();
    public List<T> getUpdatedRangesHavingUpdatedFeatureAnnotations(long processId);
    public List<T> getUpdatedRangesHavingUpdatedFeatureAnnotations(Date date);

    public List<T> getUpdatedRangesByRangeAcAndDate(String rangeAc, Date updateDate);
    public List<T> getUpdatedRangesByComponentAcAndDate(String componentAc, Date updateDate);

    public List<T> getUpdatedRangesBeforeUpdateDate(Date updateDate);
    public List<T> getUpdatedRangesHavingUpdatedFeatureAnnotationsBefore(Date date);

    public List<T> getUpdatedRangesByRangeAcAndBeforeDate(String rangeAc, Date updateDate);
    public List<T> getUpdatedRangesByComponentAcAndBeforeDate(String componentAc, Date updateDate);

    public List<T> getUpdatedRangesAfterUpdateDate(Date updateDate);
    public List<T> getUpdatedRangesHavingUpdatedFeatureAnnotationsAfter(Date date);

    public List<T> getUpdatedRangesByRangeAcAndAfterDate(String rangeAc, Date updateDate);
    public List<T> getUpdatedRangesByComponentAcAndAfterDate(String componentAc, Date updateDate);

    public List<T> getUpdatedRangesByRangeAcAndProcessId(String rangeAc, long processId);
    public List<T> getUpdatedRangesByComponentAcAndProcessId(String componentAc, long processId);
}
