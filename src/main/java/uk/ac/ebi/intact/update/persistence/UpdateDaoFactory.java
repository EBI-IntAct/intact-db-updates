package uk.ac.ebi.intact.update.persistence;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import uk.ac.ebi.intact.update.model.*;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentMappingReport;
import uk.ac.ebi.intact.update.model.protein.range.AbstractUpdatedRange;
import uk.ac.ebi.intact.update.model.protein.update.events.PersistentProteinEvent;
import uk.ac.ebi.intact.update.persistence.impl.*;
import uk.ac.ebi.intact.update.persistence.protein.*;
import uk.ac.ebi.intact.update.persistence.protein.impl.MappingReportDaoImpl;
import uk.ac.ebi.intact.update.persistence.protein.impl.ProteinEventDaoImpl;
import uk.ac.ebi.intact.update.persistence.protein.impl.UpdatedRangeDaoImpl;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.Serializable;

/**
 * The DaoFactory for the annotated element in the CurationTools project
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-May-2010</pre>
 */
@Component
public class UpdateDaoFactory implements Serializable{
    /**
     * The entuty manager
     */
    @PersistenceContext( unitName = "intact-update" )
    private EntityManager currentEntityManager;

    /**
     * The application context
     */
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * The BlastReportDao instance
     */
    @Autowired
    private BlastReportDao blastReportDao;

    /**
     * The BlastResultsDao instance
     */
    @Autowired
    private BlastResultsDao blastResultsDao;

    /**
     * The PersistentPICRCrossReferences instance
     */
    @Autowired
    private PICRCrossReferencesDao picrCrossReferencesDao;

    /**
     * The PICRReportDao instance
     */
    @Autowired
    private PICRReportDao picrReportDao;

    /**
     * The updateResultsDao instance
     */
    @Autowired
    private IdentificationResultsDao updateResultsDao;

    /**
     * The ivalidRangeDao instance
     */
    @Autowired
    private InvalidRangeDao invalidRangeDao;

    /**
     * The proteinUpdateProcessDao instance
     */
    @Autowired
    private ProteinUpdateProcessDao proteinUpdateProcessDao;


    /**
     * Creates a UpdateDaoFactory
     */
    public UpdateDaoFactory() {
    }

    /**
     *
     * @return the entity manager
     */
    public EntityManager getEntityManager() {
        return currentEntityManager;
    }

    /**
     *
     * @param entityType
     * @param <T>
     * @return the MappingReportDao instance
     */
    public <T extends PersistentMappingReport> MappingReportDao<T> getMappingReportDao(Class<T> entityType) {
        MappingReportDao actionReportDao = getBean(MappingReportDaoImpl.class);
        actionReportDao.setEntityClass(entityType);
        return actionReportDao;
    }

    /**
     *
     * @param entityType
     * @param <T>
     * @return the UpdatedRangeDao instance
     */
    public <T extends AbstractUpdatedRange> UpdatedRangeDao<T> getUpdatedRangeDao( Class<T> entityType) {
        UpdatedRangeDao updatedRangeDao = getBean(UpdatedRangeDaoImpl.class);
        updatedRangeDao.setEntityClass(entityType);
        return updatedRangeDao;
    }

    /**
     *
     * @param entityType
     * @param <T>
     * @return the UpdateEventDao instance
     */
    public <T extends PersistentProteinEvent> ProteinEventDao<T> getProteinEventDao( Class<T> entityType) {
        ProteinEventDao<T> proteinEventDao = getBean(ProteinEventDaoImpl.class);
        proteinEventDao.setEntityClass(entityType);
        return proteinEventDao;
    }

    public <T extends UpdateEvent> UpdateEventDao<T> getUpdateEventDao( Class<T> entityType) {
        UpdateEventDao updateEventDao = getBean(UpdateEventDaoImpl.class);
        updateEventDao.setEntityClass(entityType);
        return updateEventDao;
    }

    /**
     *
     * @return the BlastReportDao
     */
    public BlastResultsDao getBlastResultsDao() {
        return blastResultsDao;
    }

    /**
     *
     * @return the PicrCrossReferencesDao
     */
    public PICRCrossReferencesDao getPicrCrossReferencesDao() {
        return picrCrossReferencesDao;
    }

    /**
     *
     * @return the IdentificationResultsDao
     */
    public IdentificationResultsDao getUpdateResultsDao() {
        return updateResultsDao;
    }

    /**
     *
     * @return the BlastReportDao
     */
    public BlastReportDao getBlastReportDao() {
        return blastReportDao;
    }

    /**
     *
     * @return the PICRReportDao
     */
    public PICRReportDao getPICRReportDao() {
        return picrReportDao;
    }

    /**
     *
     * @return the invalidRangeDao
     */
    public InvalidRangeDao getInvalidRangeDao() {
        return invalidRangeDao;
    }

   /**
     *
     * @param entityType
     * @param <T>
     * @return the UpdateEventDao instance
     */
    public <T extends UpdateProcessImpl> UpdateProcessDao<T> getUpdateProcessDao( Class<T> entityType) {
        UpdateProcessDao<T> updateProcessDao = getBean(UpdateProcessDaoImpl.class);
        updateProcessDao.setEntityClass(entityType);

        return updateProcessDao;
    }

    public ProteinUpdateProcessDao getProteinUpdateProcessDao() {
        return proteinUpdateProcessDao;
    }

    public <T extends UpdatedAnnotation> UpdatedAnnotationDao<T> getUpdatedAnnotationDao( Class<T> entityType) {
        UpdatedAnnotationDao<T> updatedAnnotationDao = getBean(UpdatedAnnotationDaoImpl.class);
        updatedAnnotationDao.setEntityClass(entityType);

        return updatedAnnotationDao;
    }

    public <T extends UpdatedCrossReference> UpdatedCrossReferenceDao<T> getUpdatedCrossReferenceDao( Class<T> entityType) {
        UpdatedCrossReferenceDao<T> updatedCrossReferenceDao = getBean(UpdatedCrossReferenceDaoImpl.class);
        updatedCrossReferenceDao.setEntityClass(entityType);

        return updatedCrossReferenceDao;
    }

    public <T extends UpdatedAlias> UpdatedAliasDao<T> getUpdatedAliasDao( Class<T> entityType) {
        UpdatedAliasDao<T> updatedAliasDao = getBean(UpdatedAliasDaoImpl.class);
        updatedAliasDao.setEntityClass(entityType);

        return updatedAliasDao;
    }

    /**
     *
     * @param beanType
     * @param <T>
     * @return the Bean
     */
    private <T> T getBean(Class<T> beanType) {
        return (T) applicationContext.getBean(StringUtils.uncapitalize(beanType.getSimpleName()));
    }

}
