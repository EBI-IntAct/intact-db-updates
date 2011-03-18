package uk.ac.ebi.intact.update.persistence.dao.impl;

import junit.framework.Assert;
import org.junit.Test;
import uk.ac.ebi.intact.update.model.protein.update.events.range.InvalidRange;
import uk.ac.ebi.intact.update.model.unit.UpdateBasicTestCase;
import uk.ac.ebi.intact.update.persistence.InvalidRangeDao;

/**
 * Unit test for InvalidRangeDaoImpl
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>17/03/11</pre>
 */

public class InvalidRangeDaoImplTest extends UpdateBasicTestCase {

    @Test
    public void save_all_invalids(){
        InvalidRangeDao invalidRangeDao = getDaoFactory().getInvalidRangeDao();

        InvalidRange invalid = getMockBuilder().createInvalidRange();

        invalidRangeDao.persist(invalid);
        Assert.assertEquals(1, invalidRangeDao.countAll());
    }

    @Test
    public void delete_all_invalids(){
        InvalidRangeDao invalidRangeDao = getDaoFactory().getInvalidRangeDao();

        InvalidRange invalid = getMockBuilder().createInvalidRange();

        invalidRangeDao.persist(invalid);
        Assert.assertEquals(1, invalidRangeDao.countAll());

        invalidRangeDao.delete(invalid);
        Assert.assertEquals(0, invalidRangeDao.countAll());
    }

    @Test
    public void get_all_invalids(){
        InvalidRangeDao invalidRangeDao = getDaoFactory().getInvalidRangeDao();

        InvalidRange invalid = getMockBuilder().createInvalidRange();

        invalidRangeDao.persist(invalid);
        Assert.assertEquals(1, invalidRangeDao.getAllInvalidRanges().size());
        Assert.assertEquals(0, invalidRangeDao.getAllOutOfDateRanges().size());
    }

    @Test
    public void get_all_out_of_date(){
        InvalidRangeDao invalidRangeDao = getDaoFactory().getInvalidRangeDao();

        InvalidRange invalid = getMockBuilder().createOutOfDateRange();

        invalidRangeDao.persist(invalid);
        Assert.assertEquals(0, invalidRangeDao.getAllInvalidRanges().size());
        Assert.assertEquals(1, invalidRangeDao.getAllOutOfDateRanges().size());
    }
}
