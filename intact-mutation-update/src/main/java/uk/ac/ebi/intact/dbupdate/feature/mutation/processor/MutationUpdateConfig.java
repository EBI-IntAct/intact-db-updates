package uk.ac.ebi.intact.dbupdate.feature.mutation.processor;

import uk.ac.ebi.intact.dbupdate.feature.mutation.helper.MutationUpdateDao;
import uk.ac.ebi.intact.dbupdate.feature.mutation.writer.FileReportHandler;
import uk.ac.ebi.intact.jami.dao.IntactDao;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.ShortlabelGenerator;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class MutationUpdateConfig {

    private FileReportHandler fileReportHandler;
    private IntactDao intactDao;
    private ShortlabelGenerator shortlabelGenerator;
    private MutationUpdateDao mutationUpdateDao;

    public IntactDao getIntactDao() {
        return intactDao;
    }

    public void setIntactDao(IntactDao intactDao) {
        this.intactDao = intactDao;
    }

    public ShortlabelGenerator getShortlabelGenerator() {
        return shortlabelGenerator;
    }

    public void setShortlabelGenerator(ShortlabelGenerator shortlabelGenerator) {
        this.shortlabelGenerator = shortlabelGenerator;
    }

    public FileReportHandler getFileReportHandler() {
        return fileReportHandler;
    }

    public void setFileReportHandler(FileReportHandler fileReportHandler) {
        this.fileReportHandler = fileReportHandler;
    }

    public MutationUpdateDao getMutationUpdateDao() {
        return mutationUpdateDao;
    }

    public void setMutationUpdateDao(MutationUpdateDao mutationUpdateDao) {
        this.mutationUpdateDao = mutationUpdateDao;
    }
}