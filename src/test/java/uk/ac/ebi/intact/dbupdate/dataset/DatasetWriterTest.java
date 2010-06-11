package uk.ac.ebi.intact.dbupdate.dataset;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.intact.model.*;

import java.util.Collection;
import java.util.List;

/**
 * DataWriter tester
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>10-Jun-2010</pre>
 */

public class DatasetWriterTest extends BasicDatasetTest{

    private DatasetWriter writer;

    private boolean datasetHasBeenAddedToExperiment(Experiment e){
        Collection<Annotation> annotations = e.getAnnotations();

        for (Annotation a : annotations){
            if (a.getCvTopic() != null) {
                if (a.getCvTopic().getIdentifier().equals(CvTopic.DATASET_MI_REF)){
                    if (a.getAnnotationText().equalsIgnoreCase(this.writer.getProteinSelector().getDatasetValueToAdd())){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private int getNumberOfDatasetFor(Experiment e){
        Collection<Annotation> annotations = e.getAnnotations();

        int d = 0;
        for (Annotation a : annotations){
            if (a.getCvTopic() != null) {
                if (a.getCvTopic().getIdentifier().equals(CvTopic.DATASET_MI_REF)){
                    if (a.getAnnotationText().equalsIgnoreCase(this.writer.getProteinSelector().getDatasetValueToAdd())){
                        d++;
                    }
                }
            }
        }
        return d;
    }

    @Before
    public void setUpDatabase(){
        super.setUpDatabase();
        InteractorAliasSelector selector = new InteractorAliasSelector();
        selector.setFileWriterEnabled(false);
        this.writer = new DatasetWriter(intactContext);
        this.writer.setFileWriterEnabled(true);
        this.writer.setProteinSelector(selector);
    }

    @Test
    public void test_select_All_Experiments(){
        try {
            this.writer.addDatasetAnnotationToExperimentsFor("/dataset/synapseTest.csv");

            List<Experiment> experiments = this.intactContext.getDaoFactory().getExperimentDao().getAll();

            for (Experiment e : experiments){
                Assert.assertTrue(datasetHasBeenAddedToExperiment(e));
            }
        } catch (ProteinSelectorException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
    }

    @Test
    public void test_select_experiments_excludePublicationWithTooManyInteractions(){
        try {
            Experiment exp = getMockBuilder().createExperimentRandom("amph_2010_3",120);
            exp.setPublication(p1);
            exp.getXrefs().iterator().next().setPrimaryId(p1.getPublicationId());
            Collection<Component> components1 = exp.getInteractions().iterator().next().getComponents();
            components1.iterator().next().setInteractor(prot1);

            this.intactContext.getCorePersister().saveOrUpdate(exp);

            this.writer.addDatasetAnnotationToExperimentsFor("/dataset/synapseTest.csv");

            Collection<Experiment> experiments = this.intactContext.getDaoFactory().getExperimentDao().getByPubId(p1.getPublicationId());

            Assert.assertEquals(3, experiments.size());

            for (Experiment e : experiments){
                Assert.assertFalse(datasetHasBeenAddedToExperiment(e));
            }
        } catch (ProteinSelectorException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
    }

    @Test
    public void test_select_experiments_excludePublicationWithTooManyInteractionsAndNoProteinOfInterest(){
        try {
            Experiment exp = getMockBuilder().createExperimentRandom("amph_2010_3",120);
            exp.setPublication(p1);
            exp.getXrefs().iterator().next().setPrimaryId(p1.getPublicationId());

            this.intactContext.getCorePersister().saveOrUpdate(exp);

            this.writer.addDatasetAnnotationToExperimentsFor("/dataset/synapseTest.csv");

            Collection<Experiment> experiments = this.intactContext.getDaoFactory().getExperimentDao().getByPubId(p1.getPublicationId());

            for (Experiment e : experiments){
                Assert.assertFalse(datasetHasBeenAddedToExperiment(e));
            }
        } catch (ProteinSelectorException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
    }

    @Test
    public void test_select_experiments_excludedPublication(){
        try {
            Publication p = getMockBuilder().createPublication("15102471");
            Experiment exp = getMockBuilder().createExperimentRandom("amph_2020_1",12);
            exp.setPublication(p);
            exp.getXrefs().iterator().next().setPrimaryId(p.getPublicationId());
            Collection<Component> components1 = exp.getInteractions().iterator().next().getComponents();
            components1.iterator().next().setInteractor(prot1);

            this.intactContext.getCorePersister().saveOrUpdate(exp);

            this.writer.addDatasetAnnotationToExperimentsFor("/dataset/synapseTest.csv");

            Collection<Experiment> experiments = this.intactContext.getDaoFactory().getExperimentDao().getByPubId(p.getPublicationId());

            for (Experiment e : experiments){
                Assert.assertFalse(datasetHasBeenAddedToExperiment(e));
            }
        } catch (ProteinSelectorException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
    }

    @Test
    public void test_select_experiments_excludedExperimentWithDatasetPresent(){
        try {
            Experiment exp = getMockBuilder().createExperimentRandom("amph_2010_3",12);
            exp.setPublication(p1);
            exp.getXrefs().iterator().next().setPrimaryId(p1.getPublicationId());
            Annotation dataset = getMockBuilder().createAnnotation("Synapse - Interactions of proteins with an established role in the presynapse.", getMockBuilder().createCvObject(CvTopic.class, CvTopic.DATASET_MI_REF, CvTopic.DATASET));
            exp.addAnnotation(dataset);
            Collection<Component> components1 = exp.getInteractions().iterator().next().getComponents();
            components1.iterator().next().setInteractor(prot1);

            this.intactContext.getCorePersister().saveOrUpdate(exp);

            this.writer.addDatasetAnnotationToExperimentsFor("/dataset/synapseTest.csv");

            Collection<Experiment> experiments = this.intactContext.getDaoFactory().getExperimentDao().getByPubId(p1.getPublicationId());

            Assert.assertEquals("Interactions of proteins with an established role in the presynapse.", this.writer.getProteinSelector().getDatasetValueToAdd());
            for (Experiment e : experiments){
                Assert.assertTrue(datasetHasBeenAddedToExperiment(e));
                Assert.assertEquals(getNumberOfDatasetFor(e), 1);
            }
        } catch (ProteinSelectorException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
    }
}
