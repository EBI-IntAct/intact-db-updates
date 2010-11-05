package uk.ac.ebi.intact.dbupdate.prot.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.IntactException;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.AnnotationDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.event.OutOfDateParticipantFoundEvent;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.clone.IntactCloner;
import uk.ac.ebi.intact.model.clone.IntactClonerException;
import uk.ac.ebi.intact.model.util.CvObjectUtils;

import java.util.Collection;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>29-Oct-2010</pre>
 */

public class OutOfDateParticipantFixer extends AbstractProteinUpdateProcessorListener {
    private static final Log log = LogFactory.getLog( OutOfDateParticipantFixer.class );

    @Override
    public void onOutOfDateParticipantFound(OutOfDateParticipantFoundEvent evt) throws ProcessorException {

        DaoFactory factory = evt.getDataContext().getDaoFactory();

        IntactCloner cloner = new IntactCloner(true);
        Collection<Component> componentsToFix = evt.getComponentsToFix();
        Protein protein = evt.getProtein();

        try {
            Protein noUniprotUpdate = cloner.clone(protein);
            noUniprotUpdate.getActiveInstances().clear();
            addAnnotations(noUniprotUpdate, factory);
            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(noUniprotUpdate);

            for (Component component : componentsToFix){

                protein.removeActiveInstance(component);
                noUniprotUpdate.addActiveInstance(component);
            }

            factory.getProteinDao().update((ProteinImpl) protein);
            factory.getProteinDao().update((ProteinImpl) noUniprotUpdate);
        } catch (IntactClonerException e) {
            throw new IntactException("Could not clone protein: "+protein.getAc(), e);
        }

    }

    private void addAnnotations(Protein protein, DaoFactory factory){

        CvTopic no_uniprot_update = factory.getCvObjectDao(CvTopic.class).getByShortLabel(CvTopic.NON_UNIPROT);

        if (no_uniprot_update == null){
            no_uniprot_update = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, null, CvTopic.NON_UNIPROT);
            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(no_uniprot_update);
        }
        CvTopic caution = factory.getCvObjectDao(CvTopic.class).getByPsiMiRef(CvTopic.CAUTION_MI_REF);

        if (caution == null) {
            caution = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);
            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(caution);
        }

        boolean has_no_uniprot_update = false;
        boolean has_caution = false;
        String cautionMessage = "This protein is not up-to-date anymore with the uniprot protein because of feature conflicts.";

        for (Annotation annotation : protein.getAnnotations()){
            if (no_uniprot_update.equals(annotation.getCvTopic())){
                has_no_uniprot_update = true;
            }
            else if (caution.equals(annotation.getCvTopic())){
                if (annotation.getAnnotationText() != null){
                    if (annotation.getAnnotationText().equalsIgnoreCase(cautionMessage)){
                        has_caution = true;
                    }
                }
            }
        }
        AnnotationDao annotationDao = factory.getAnnotationDao();

        if (!has_no_uniprot_update){
            Annotation no_uniprot = new Annotation(no_uniprot_update, null);
            annotationDao.persist(no_uniprot);

            protein.addAnnotation(no_uniprot);
        }

        if (!has_caution){
            Annotation demerge = new Annotation(caution, "This protein is not up-to-date anymore with the uniprot protein because of feature conflicts.");
            annotationDao.persist(demerge);

            protein.addAnnotation(demerge);
        }
    }
}
