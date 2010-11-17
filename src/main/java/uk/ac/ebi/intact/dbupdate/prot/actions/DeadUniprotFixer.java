package uk.ac.ebi.intact.dbupdate.prot.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.AnnotationDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.core.persistence.dao.XrefDao;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.util.protein.utils.XrefUpdaterUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This listener is fixing proteins in IntAct which cannot match any uniprot protein anymore and update them as dead proteins
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>01-Oct-2010</pre>
 */

public class DeadUniprotFixer {

    private static final Log log = LogFactory.getLog( DeadUniprotFixer.class );

    public void fixDeadProtein(ProteinEvent evt) throws ProcessorException {
        DaoFactory factory = evt.getDataContext().getDaoFactory();

        Protein protein = evt.getProtein();

        updateAnnotations(protein, factory);
        updateXRefs(protein, factory);
        
        if (evt.getSource() instanceof ProteinUpdateProcessor) {
            final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
            updateProcessor.fireOnUniprotDeadEntry(new ProteinEvent(updateProcessor, evt.getDataContext(), evt.getProtein()));
        }
    }

    /**
     * Two new annotations will be added : a 'no-uniprot-update' and a 'caution' explaining that this protein is now obsolete in uniprot
     * @param protein :the dead protein in IntAct
     */
    private void updateAnnotations(Protein protein, DaoFactory factory){

        Collection<Annotation> annotations = protein.getAnnotations();
        CvTopic no_uniprot_update = factory.getCvObjectDao(CvTopic.class).getByShortLabel(CvTopic.NON_UNIPROT);

        if (no_uniprot_update == null){
            no_uniprot_update = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, null, CvTopic.NON_UNIPROT);
            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(no_uniprot_update);
        }
        CvTopic caution = factory.getCvObjectDao(CvTopic.class).getByPsiMiRef(CvTopic.CAUTION_MI_REF);

        if (caution == null) {
            caution = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);
            factory.getCvObjectDao(CvTopic.class).saveOrUpdate(caution);
        }

        boolean has_no_uniprot_update = false;
        boolean has_caution_obsolete = false;
        String cautionMessage = "The uniprot Ac has been obsolete.";

        for (Annotation annotation : annotations){
            if (no_uniprot_update.equals(annotation.getCvTopic())){
                has_no_uniprot_update = true;
            }
            else if (caution.equals(annotation.getCvTopic())){
                if (annotation.getAnnotationText() != null){
                    if (annotation.getAnnotationText().equalsIgnoreCase(cautionMessage)){
                        has_caution_obsolete = true;
                    }
                }
            }
        }

        AnnotationDao annotationDao = factory.getAnnotationDao();
        ProteinDao proteinDao = factory.getProteinDao();

        if (!has_no_uniprot_update){
            Annotation no_uniprot = new Annotation(no_uniprot_update, null);
            annotationDao.persist(no_uniprot);

            protein.addAnnotation(no_uniprot);
        }
        if (!has_caution_obsolete){
            Annotation obsolete = new Annotation(caution, cautionMessage);
            annotationDao.persist(obsolete);

            protein.addAnnotation(obsolete);
        }

        proteinDao.update((ProteinImpl) protein);
    }

    /**
     * This method removes all the cross references which are not intact cross references and replace the uniprot identity with 'uniprot-removed-ac'
     * @param protein : the dead protein in IntAct
     */
    private void updateXRefs(Protein protein, DaoFactory factory){

        List<InteractorXref> uniprotIdentities = ProteinTools.getAllUniprotIdentities(protein);
        if (uniprotIdentities.size() > 1){
            XrefUpdaterUtils.fixDuplicateOfSameUniprotIdentity(ProteinTools.getAllUniprotIdentities(protein), protein);
        }

        Collection<InteractorXref> xRefs = protein.getXrefs();
        XrefDao<InteractorXref> refDao = factory.getXrefDao(InteractorXref.class);

        Collection<InteractorXref> xRefsToRemove = new ArrayList<InteractorXref>();

        for (InteractorXref ref : xRefs){
            boolean toDelete = false;
            boolean isUniprotIdentity = false;

            CvDatabase cvDb = ref.getCvDatabase();
            String cvDbMi = cvDb.getIdentifier();
            CvXrefQualifier cvQualifier = ref.getCvXrefQualifier();

            if(!CvDatabase.INTACT_MI_REF.equals(cvDbMi)){
                if (CvDatabase.UNIPROT_MI_REF.equals(cvDbMi)){
                    if (cvQualifier != null){
                        if (cvQualifier.getIdentifier() != null){
                            if (cvQualifier.getIdentifier().equals(CvXrefQualifier.IDENTITY_MI_REF)){
                                isUniprotIdentity = true;
                            }
                            else {
                                toDelete = true;
                            }
                        }
                        else if (CvXrefQualifier.IDENTITY.equalsIgnoreCase(cvQualifier.getShortLabel())){
                            isUniprotIdentity = true;
                        }
                        else {
                            toDelete = true;
                        }
                    }
                    else {
                        toDelete = true;
                    }
                }
                else {
                    toDelete = true;
                }
            }

            if (toDelete){
                xRefsToRemove.add(ref);
            }
            else if (isUniprotIdentity){
                CvXrefQualifier uniprot_removed_ac = IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByShortLabel(CvXrefQualifier.UNIPROT_REMOVED_AC);

                if (uniprot_removed_ac == null){
                    uniprot_removed_ac = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvXrefQualifier.class, null, CvXrefQualifier.UNIPROT_REMOVED_AC);
                    factory.getCvObjectDao(CvXrefQualifier.class).saveOrUpdate(uniprot_removed_ac);
                }

                ref.setCvXrefQualifier(uniprot_removed_ac);
                refDao.update(ref);
            }
        }

        for (InteractorXref refToRemove : xRefsToRemove){
            protein.removeXref(refToRemove);

            refDao.delete(refToRemove);
        }

        factory.getProteinDao().update((ProteinImpl) protein);
    }
}
