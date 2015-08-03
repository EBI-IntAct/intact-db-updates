package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import uk.ac.ebi.intact.protein.mapping.actions.ActionName;
import uk.ac.ebi.intact.protein.mapping.actions.status.StatusLabel;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentMappingReport;
import uk.ac.ebi.intact.update.model.unit.UpdateBasicTestCase;
import uk.ac.ebi.intact.update.persistence.dao.protein.MappingReportDao;

import java.util.List;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>17/10/12</pre>
 */

public class MappingReportDaoImpl2Test extends UpdateBasicTestCase {

    @Test
    @DirtiesContext
    public void search_ActionReportByName_Unsuccessful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getUpdateDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals(0, actionReportDao.countAll());

        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<PersistentMappingReport> r = actionReportDao.getByActionName(ActionName.SEARCH_uniprot_name);

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    @DirtiesContext
    public void search_ActionReportByStatus_successful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getUpdateDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<PersistentMappingReport> r = actionReportDao.getByReportStatus(StatusLabel.TO_BE_REVIEWED);

        Assert.assertTrue(!r.isEmpty());
        Assert.assertTrue(r.get(0).getStatusLabel().equals(StatusLabel.TO_BE_REVIEWED));
    }

    @Test
    @DirtiesContext
    public void search_ActionReportByStatus_Unsuccessful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getUpdateDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<PersistentMappingReport> r = actionReportDao.getByReportStatus(StatusLabel.COMPLETED);

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    @DirtiesContext
    public void search_ActionReportWithWarnings_successful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getUpdateDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<PersistentMappingReport> r = actionReportDao.getAllReportsWithWarnings();

        Assert.assertTrue(!r.isEmpty());
        Assert.assertTrue(!r.get(0).getWarnings().isEmpty());
    }

}
