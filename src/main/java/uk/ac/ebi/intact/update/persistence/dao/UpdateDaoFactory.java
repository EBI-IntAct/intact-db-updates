package uk.ac.ebi.intact.update.persistence.dao;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import uk.ac.ebi.intact.update.model.UpdateEventImpl;
import uk.ac.ebi.intact.update.model.UpdateProcessImpl;
import uk.ac.ebi.intact.update.model.protein.UpdatedAnnotation;
import uk.ac.ebi.intact.update.model.protein.errors.DefaultPersistentUpdateError;
import uk.ac.ebi.intact.update.model.protein.events.PersistentProteinEvent;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentMappingReport;
import uk.ac.ebi.intact.update.model.protein.range.AbstractUpdatedRange;
import uk.ac.ebi.intact.update.persistence.dao.impl.UpdateEventDaoImpl;
import uk.ac.ebi.intact.update.persistence.dao.impl.UpdateProcessDaoImpl;
import uk.ac.ebi.intact.update.persistence.dao.protein.*;
import uk.ac.ebi.intact.update.persistence.dao.protein.impl.*;

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

    @Autowired
    private UpdatedCrossReferenceDao updatedCrossReferenceDao;

    @Autowired
    private UpdatedAliasDao updatedAliasDao;

    @Autowired
    private DeadProteinEventDao deadProteinEventDao;

    @Autowired
    private DeletedProteinEventDao deletedProteinEventDao;

    @Autowired
    private CreatedProteinEventDao createdProteinEventDao;

    @Autowired
    private IntactTranscriptUpdateEventDao intactTranscriptEventDao;

    @Autowired
    private SecondaryProteinEventDao secondaryProteinEventDao;

    @Autowired
    private SequenceIdenticalToTranscriptEventDao sequenceIdenticalToTranscriptEventDao;

    @Autowired
    private SequenceUpdateEventDao sequenceUpdateEventDao;

    @Autowired
    private UniprotProteinMapperEventDao uniprotProteinMapperEventDao;

    @Autowired
    private UniprotUpdateEventDao uniprotUpdateEventDao;

    @Autowired
    private DuplicatedProteinEventDao duplicatedProteinEventDao;

    @Autowired
    private OutOfDateParticipantEventDao outOfDateParticipantEventDao;

    @Autowired
    private DeletedComponentEventDao deletedComponentEventDao;

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

    public <T extends DefaultPersistentUpdateError> ProteinUpdateErrorDao<T> getProteinUpdateErrorDao(Class<T> entityType) {
        ProteinUpdateErrorDao proteinUpdateErrorDao = getBean(ProteinUpdateErrorDaoImpl.class);
        proteinUpdateErrorDao.setEntityClass(entityType);
        return proteinUpdateErrorDao;
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

    public <T extends UpdateEventImpl> UpdateEventDao<T> getUpdateEventDao( Class<T> entityType) {
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

    public UpdatedCrossReferenceDao getUpdatedCrossReferenceDao() {
        return updatedCrossReferenceDao;
    }

    public UpdatedAliasDao getUpdatedAliasDao() {

        return updatedAliasDao;
    }

    public PICRReportDao getPicrReportDao() {
        return picrReportDao;
    }

    public DeadProteinEventDao getDeadProteinEventDao() {
        return deadProteinEventDao;
    }

    public DeletedProteinEventDao getDeletedProteinEventDao() {
        return deletedProteinEventDao;
    }

    public CreatedProteinEventDao getCreatedProteinEventDao() {
        return createdProteinEventDao;
    }

    public IntactTranscriptUpdateEventDao getIntactTranscriptEventDao() {
        return intactTranscriptEventDao;
    }

    public SecondaryProteinEventDao getSecondaryProteinEventDao() {
        return secondaryProteinEventDao;
    }

    public SequenceIdenticalToTranscriptEventDao getSequenceIdenticalToTranscriptEventDao() {
        return sequenceIdenticalToTranscriptEventDao;
    }

    public SequenceUpdateEventDao getSequenceUpdateEventDao() {
        return sequenceUpdateEventDao;
    }

    public UniprotProteinMapperEventDao getUniprotProteinMapperEventDao() {
        return uniprotProteinMapperEventDao;
    }

    public UniprotUpdateEventDao getUniprotUpdateEventDao() {
        return uniprotUpdateEventDao;
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

    public DuplicatedProteinEventDao getDuplicatedProteinEventDao() {
        return duplicatedProteinEventDao;
    }

    public OutOfDateParticipantEventDao getOutOfDateParticipantEventDao() {
        return outOfDateParticipantEventDao;
    }

    public DeletedComponentEventDao getDeletedComponentEventDao() {
        return deletedComponentEventDao;
    }
}
