package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentUniprotProteinAPIReport;
import uk.ac.ebi.intact.update.persistence.dao.protein.UniprotProteinAPIReportDao;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * The basic implementation of UniprotProteinAPIReportDao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>21-May-2010</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class UniprotProteinAPIReportDaoImpl extends MappingReportDaoImpl<PersistentUniprotProteinAPIReport> implements UniprotProteinAPIReportDao {

    /**
     * create a new UniprotProteinAPIReportDaoImpl
     */
    public UniprotProteinAPIReportDaoImpl() {
        super(PersistentUniprotProteinAPIReport.class, null);
    }

    /**
     * create a new UniprotProteinAPIReportDaoImpl with entity manager
     * @param entityManager
     */
    public UniprotProteinAPIReportDaoImpl(EntityManager entityManager) {
        super(PersistentUniprotProteinAPIReport.class, entityManager);
    }

    /**
     *
     * @param id
     * @return
     */
    public List<PersistentUniprotProteinAPIReport> getUniprotProteinAPIReportsByResultsId(long id) {
        return getSession().createCriteria(PersistentUniprotProteinAPIReport.class).createAlias("updateResult", "u")
                .add(Restrictions.eq("u.id", id)).list();
    }
}
