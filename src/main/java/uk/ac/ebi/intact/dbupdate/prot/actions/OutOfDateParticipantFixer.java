package uk.ac.ebi.intact.dbupdate.prot.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.IntactException;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.AnnotationDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.event.OutOfDateParticipantFoundEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.listener.AbstractProteinUpdateProcessorListener;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.clone.IntactCloner;
import uk.ac.ebi.intact.model.clone.IntactClonerException;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotFeatureChain;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;
import uk.ac.ebi.intact.uniprot.model.UniprotSpliceVariant;

import java.util.ArrayList;
import java.util.Collection;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>29-Oct-2010</pre>
 */

public class OutOfDateParticipantFixer {
    private static final Log log = LogFactory.getLog( OutOfDateParticipantFixer.class );

    private void moveInteractionsOfExistingProtein(Protein source, Protein existingProtein, DaoFactory factory){
        for (Component c : source.getActiveInstances()){
            existingProtein.addActiveInstance(c);
            factory.getComponentDao().update(c);
        }
        source.getActiveInstances().clear();

        ProteinDao proteinDao = factory.getProteinDao();
        proteinDao.update((ProteinImpl)source);

        // as the protein will be completely merged, an intact secondary reference is necessary
        final String intactSecondaryLabel = "intact-secondary";
        boolean hasIntactSecondary = false;
        Institution owner = source.getOwner();

        // the database is always intact because the framework is the intact framework and when we merge two proteins of this framework, it becomes 'intact-secondary'
        CvDatabase db = factory.getCvObjectDao( CvDatabase.class ).getByPsiMiRef( CvDatabase.INTACT_MI_REF );

        if (db == null){
            db = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvDatabase.class, CvDatabase.MINT_MI_REF, CvDatabase.INTACT);
            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(db);
        }
        CvXrefQualifier intactSecondary = factory.getCvObjectDao(CvXrefQualifier.class).getByShortLabel(intactSecondaryLabel);

        if (intactSecondary == null) {
            intactSecondary = CvObjectUtils.createCvObject(owner, CvXrefQualifier.class, CvDatabase.INTACT_MI_REF, intactSecondaryLabel);
            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(intactSecondary);
        }

        for (InteractorXref ref : existingProtein.getXrefs()){
            if (ref.getCvDatabase() != null){
                if (ref.getCvDatabase().getIdentifier().equals(CvDatabase.INTACT_MI_REF)){
                    if (ref.getCvXrefQualifier() != null){
                        if (ref.getCvXrefQualifier().getShortLabel().equals(intactSecondaryLabel) && ref.getPrimaryId().equals(source.getAc())){
                            hasIntactSecondary = true;
                        }
                    }
                }
            }
        }

        if (!hasIntactSecondary){
            InteractorXref xref = new InteractorXref(owner, db, source.getAc(), intactSecondary);
            factory.getXrefDao(InteractorXref.class).persist(xref);

            existingProtein.addXref(xref);
            log.debug( "Adding 'intact-secondary' Xref to protein '"+ existingProtein.getShortLabel() +"' ("+existingProtein.getAc()+"): " + source.getAc() );
        }

        proteinDao.update((ProteinImpl)existingProtein);
    }

    private ProteinTranscript createSpliceVariant(UniprotSpliceVariant spliceVariant, Protein proteinWithConflicts, Collection<Component> componentsToFix, IntactCloner cloner, DaoFactory factory, OutOfDateParticipantFoundEvent evt){

        try {
            Protein spliceIntact = cloner.clone(proteinWithConflicts);
            InteractorXref identity = ProteinUtils.getUniprotXref(spliceIntact);
            identity.setPrimaryId(spliceVariant.getPrimaryAc());

            Institution owner = spliceIntact.getOwner();
            CvDatabase db = factory.getCvObjectDao( CvDatabase.class ).getByPsiMiRef( CvDatabase.INTACT_MI_REF );

            if (db == null){
                db = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvDatabase.class, CvDatabase.INTACT_MI_REF, CvDatabase.INTACT);
                IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(db);
            }
            CvXrefQualifier isoformParent = factory.getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.ISOFORM_PARENT_MI_REF);

            if (isoformParent == null) {
                isoformParent = CvObjectUtils.createCvObject(owner, CvXrefQualifier.class, CvXrefQualifier.ISOFORM_PARENT_MI_REF, CvXrefQualifier.ISOFORM_PARENT);
                IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoformParent);
            }
            InteractorXref parent = new InteractorXref(owner, db, proteinWithConflicts.getAc(), isoformParent);
            spliceIntact.addXref(parent);

            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(spliceIntact);

            for (Component c : componentsToFix){
                proteinWithConflicts.removeActiveInstance(c);
                spliceIntact.addActiveInstance(c);

                factory.getComponentDao().update(c);
            }

            ProteinDao proteinDao = factory.getProteinDao();

            proteinDao.update((ProteinImpl)spliceIntact);
            proteinDao.update((ProteinImpl)proteinWithConflicts);

            if (evt.getSource() instanceof ProteinUpdateProcessor){
                ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                processor.fireOnProteinCreated(new ProteinEvent(processor, evt.getDataContext(), spliceIntact));
            }

            return new ProteinTranscript(spliceIntact, spliceVariant);
        } catch (IntactClonerException e) {
            throw new ProcessorException("Impossible to clone the protein " + proteinWithConflicts.getAc());
        }
    }

    private ProteinTranscript createFeatureChain(UniprotFeatureChain featureChain, Protein proteinWithConflicts, Collection<Component> componentsToFix, IntactCloner cloner, DaoFactory factory, OutOfDateParticipantFoundEvent evt){

        try {
            Protein chainIntact = cloner.clone(proteinWithConflicts);
            InteractorXref identity = ProteinUtils.getUniprotXref(chainIntact);
            identity.setPrimaryId(featureChain.getPrimaryAc());

            Institution owner = chainIntact.getOwner();
            CvDatabase db = factory.getCvObjectDao( CvDatabase.class ).getByPsiMiRef( CvDatabase.INTACT_MI_REF );

            if (db == null){
                db = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvDatabase.class, CvDatabase.INTACT_MI_REF, CvDatabase.INTACT);
                IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(db);
            }
            CvXrefQualifier chainParent = factory.getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.CHAIN_PARENT_MI_REF);

            if (chainParent == null) {
                chainParent = CvObjectUtils.createCvObject(owner, CvXrefQualifier.class, CvXrefQualifier.CHAIN_PARENT_MI_REF, CvXrefQualifier.CHAIN_PARENT);
                IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chainParent);
            }
            InteractorXref parent = new InteractorXref(owner, db, proteinWithConflicts.getAc(), chainParent);
            chainIntact.addXref(parent);

            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chainIntact);

            for (Component c : componentsToFix){
                proteinWithConflicts.removeActiveInstance(c);
                chainIntact.addActiveInstance(c);

                factory.getComponentDao().update(c);
            }

            ProteinDao proteinDao = factory.getProteinDao();

            proteinDao.update((ProteinImpl)chainIntact);
            proteinDao.update((ProteinImpl)proteinWithConflicts);

            if (evt.getSource() instanceof ProteinUpdateProcessor){
                ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                processor.fireOnProteinCreated(new ProteinEvent(processor, evt.getDataContext(), chainIntact));
            }

            return new ProteinTranscript(chainIntact, featureChain);
        } catch (IntactClonerException e) {
            throw new ProcessorException("Impossible to clone the protein " + proteinWithConflicts.getAc());
        }
    }

    public ProteinTranscript fixParticipantWithRangeConflicts(OutOfDateParticipantFoundEvent evt, boolean createDeprecatedParticipant){

        if (evt.getSource() instanceof ProteinUpdateProcessor){
            ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

            processor.fireOnOutOfDateParticipantFound(evt);
        }

        DaoFactory factory = evt.getDataContext().getDaoFactory();
        IntactCloner cloner = new IntactCloner(true);
        Collection<Component> componentsToFix = evt.getComponentsToFix();
        Protein protein = evt.getProteinWithConflicts();
        UniprotProtein uniprotProtein = evt.getProtein();

        String sequenceWithConflicts = protein.getSequence();

        if (sequenceWithConflicts != null){
            Collection<UniprotProteinTranscript> proteinTranscripts = new ArrayList<UniprotProteinTranscript>();
            proteinTranscripts.addAll(uniprotProtein.getSpliceVariants());
            proteinTranscripts.addAll(uniprotProtein.getFeatureChains());

            if (!proteinTranscripts.isEmpty()){
                Collection<UniprotProteinTranscript> possibleMatches = new ArrayList<UniprotProteinTranscript>();

                for (UniprotProteinTranscript pt : proteinTranscripts){
                    if (sequenceWithConflicts.equalsIgnoreCase(pt.getSequence())){
                        possibleMatches.add(pt);
                    }
                }

                if (possibleMatches.size() == 1){
                    UniprotProteinTranscript match = possibleMatches.iterator().next();

                    if (match instanceof UniprotSpliceVariant){

                        for (ProteinTranscript p : evt.getPrimaryIsoforms()){
                            if (match.equals(p.getUniprotVariant())){
                                moveInteractionsOfExistingProtein(protein, p.getProtein(), factory);
                                return p;
                            }
                        }

                        for (ProteinTranscript p : evt.getSecondaryIsoforms()){
                            if (match.equals(p.getUniprotVariant())){
                                moveInteractionsOfExistingProtein(protein, p.getProtein(), factory);
                                return p;
                            }
                        }

                        return createSpliceVariant((UniprotSpliceVariant) match, protein, componentsToFix, cloner, factory, evt);
                    }
                    else if (match instanceof UniprotFeatureChain){

                        for (ProteinTranscript p : evt.getPrimaryFeatureChains()){
                            if (match.equals(p.getUniprotVariant())){
                                moveInteractionsOfExistingProtein(protein, p.getProtein(), factory);
                                return p;
                            }
                        }

                        return createFeatureChain((UniprotFeatureChain) match, protein, componentsToFix, cloner, factory, evt);
                    }
                }
            }
        }

        if (createDeprecatedParticipant){
            return createDeprecatedProtein(evt, false);
        }

        return null;
    }

    public ProteinTranscript createDeprecatedProtein(OutOfDateParticipantFoundEvent evt, boolean fireEventListeners){

        if (fireEventListeners){
            if (evt.getSource() instanceof ProteinUpdateProcessor){
                ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                processor.fireOnOutOfDateParticipantFound(evt);
            }
        }
        DaoFactory factory = evt.getDataContext().getDaoFactory();

        IntactCloner cloner = new IntactCloner(true);
        Collection<Component> componentsToFix = evt.getComponentsToFix();
        Protein protein = evt.getProteinWithConflicts();

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

            if (evt.getSource() instanceof ProteinUpdateProcessor){
                ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                processor.fireOnProteinCreated(new ProteinEvent(processor, evt.getDataContext(), noUniprotUpdate));
            }

            return new ProteinTranscript(noUniprotUpdate, null);

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
