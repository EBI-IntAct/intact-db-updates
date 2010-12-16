package uk.ac.ebi.intact.dbupdate.prot.actions.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.persistence.dao.AnnotationDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.core.persistence.dao.XrefDao;
import uk.ac.ebi.intact.dbupdate.prot.*;
import uk.ac.ebi.intact.dbupdate.prot.actions.DeadUniprotProteinFixer;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateErrorEvent;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.util.protein.utils.XrefUpdaterUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;
import java.util.List;

/**
 * This listener is fixing proteins in IntAct which cannot match any uniprot protein anymore and update them as dead proteins
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>01-Oct-2010</pre>
 */

public class DeadUniprotProteinFixerImpl implements DeadUniprotProteinFixer{

    /**
     * Logger for this class
     */
    private static final Log log = LogFactory.getLog( DeadUniprotProteinFixerImpl.class );

    /**
     * Update the protein as a dead protein :
     * - 'no-uniprot-update' annotation
     * - 'caution' sequence has been withdrawn from uniprot
     * - remove all other cross references (not intact xrefs)
     * - convert the uniprot identity into 'uniprot-removed-ac'
     * @param evt : protein event containing the protein to update and the uniprot identifier of this protein
     * @throws ProcessorException
     */
    public void fixDeadProtein(ProteinEvent evt) throws ProcessorException {

        // get the protein to update
        Protein protein = evt.getProtein();

        // add no-uniprot-update and the caution
        updateAnnotations(protein, evt.getDataContext());

        if (evt.getSource() instanceof ProteinUpdateProcessor) {

            final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();

            // delete all other xrefs which are not intact or uniprot identity and update the uniprot identity
            updateXRefs(protein, evt.getDataContext(), updateProcessor);

            // log the dead protein in 'dead_proteins.csv'
            updateProcessor.fireOnUniprotDeadEntry(new ProteinEvent(updateProcessor, evt.getDataContext(), evt.getProtein()));
        }
    }

    /**
     * Two new annotations will be added : a 'no-uniprot-update' and a 'caution' explaining that this protein is now obsolete in uniprot
     * @param protein :the dead protein in IntAct
     */
    private void updateAnnotations(Protein protein, DataContext context){
        DaoFactory factory = context.getDaoFactory();
        Collection<Annotation> annotations = protein.getAnnotations();

        // no-uniprot update CvTopic
        CvTopic no_uniprot_update = factory.getCvObjectDao(CvTopic.class).getByShortLabel(CvTopic.NON_UNIPROT);

        if (no_uniprot_update == null){
            no_uniprot_update = CvObjectUtils.createCvObject(protein.getOwner(), CvTopic.class, null, CvTopic.NON_UNIPROT);
            factory.getCvObjectDao(CvTopic.class).persist(no_uniprot_update);
        }

        // caution CvTopic
        CvTopic caution = factory.getCvObjectDao(CvTopic.class).getByPsiMiRef(CvTopic.CAUTION_MI_REF);

        if (caution == null) {
            caution = CvObjectUtils.createCvObject(protein.getOwner(), CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);
            factory.getCvObjectDao(CvTopic.class).saveOrUpdate(caution);
        }

        boolean has_no_uniprot_update = false;
        boolean has_caution_obsolete = false;
        String cautionMessage = "The sequence has been withdrawn from uniprot.";

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

        // if no 'no-uniprot-update' exists, add the annotation
        if (!has_no_uniprot_update){
            Annotation no_uniprot = new Annotation(no_uniprot_update, null);
            annotationDao.persist(no_uniprot);

            protein.addAnnotation(no_uniprot);
        }
        // if no 'caution' exists, add the annotation
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
    private void updateXRefs(Protein protein, DataContext context, ProteinUpdateProcessor processor){
        DaoFactory factory = context.getDaoFactory();

        // get the list of uniprot identities (are supposed to be duplicates of the same uniprot ac because
        // this method is always used after filtering proteins with several distinct uniprot acs)
        List<InteractorXref> uniprotIdentities = ProteinTools.getAllUniprotIdentities(protein);

        // if we have more than one uniprot identity, merge the uniprot identities and keep the oldest xref
        if (uniprotIdentities.size() > 1){
            XrefUpdaterUtils.fixDuplicateOfSameUniprotIdentity(ProteinTools.getAllUniprotIdentities(protein), protein, context, processor);
        }

        // get the xrefs of the protein
        Collection<InteractorXref> xRefs = protein.getXrefs();
        XrefDao<InteractorXref> refDao = factory.getXrefDao(InteractorXref.class);

        // the collection of xrefs to delete
        Collection<InteractorXref> xRefsToRemove = new ArrayList<InteractorXref>();

        for (InteractorXref ref : xRefs){
            boolean toDelete = false;
            boolean isUniprotIdentity = false;

            CvDatabase cvDb = ref.getCvDatabase();
            String cvDbMi = cvDb.getIdentifier();
            CvXrefQualifier cvQualifier = ref.getCvXrefQualifier();

            // if the database is not intact, the cross reference may be deleted
            if(!CvDatabase.INTACT_MI_REF.equals(cvDbMi)){
                // if the database is uniprot, the cross references can be deleted only if it is not a uniprot identity
                if (CvDatabase.UNIPROT_MI_REF.equals(cvDbMi)){
                    // check that the uniprot xref has a qualifier identity
                    if (cvQualifier != null){
                        if (cvQualifier.getIdentifier() != null){
                            // it is uniprot identity and we cannot delete this XRef
                            if (cvQualifier.getIdentifier().equals(CvXrefQualifier.IDENTITY_MI_REF)){
                                isUniprotIdentity = true;
                            }
                            // it is not a uniprot identity, we can delete this cross reference
                            else {
                                toDelete = true;
                            }
                        }
                        // if the qualifier doesn't have a MI number, we check the shortlabel
                        else if (CvXrefQualifier.IDENTITY.equalsIgnoreCase(cvQualifier.getShortLabel())){
                            isUniprotIdentity = true;
                        }
                        else {
                            toDelete = true;
                        }
                    }
                    // the xref can be deleted because not identity
                    else {
                        toDelete = true;
                    }
                }
                // if the cross reference is neither from intact nor from uniprot, we delete it
                else {
                    toDelete = true;
                }
            }

            // if the xref can be deleted, we delete it
            if (toDelete){
                if (!xRefsToRemove.contains(ref)){
                    xRefsToRemove.add(ref);
                }
            }
            // if the xref is uniprot identity, we update it as uniprot-removed-ac
            else if (isUniprotIdentity){
                CvXrefQualifier uniprot_removed_ac = factory.getCvObjectDao(CvXrefQualifier.class).getByShortLabel(CvXrefQualifier.UNIPROT_REMOVED_AC);

                if (uniprot_removed_ac == null){
                    uniprot_removed_ac = CvObjectUtils.createCvObject(protein.getOwner(), CvXrefQualifier.class, null, CvXrefQualifier.UNIPROT_REMOVED_AC);
                    factory.getCvObjectDao(CvXrefQualifier.class).saveOrUpdate(uniprot_removed_ac);
                }

                ref.setCvXrefQualifier(uniprot_removed_ac);
                refDao.update(ref);
            }
        }

        // we delete the xrefs to be deleted
        for (InteractorXref refToRemove : xRefsToRemove){
            ProteinTools.deleteInteractorXRef(protein, context, refToRemove, processor);
        }

        // update the protein to be deleted
        factory.getProteinDao().update((ProteinImpl) protein);
    }
}
