package uk.ac.ebi.intact.dbupdate.prot.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.IntactException;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.AnnotationDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.event.OutOfDateParticipantFoundEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
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
        Collection<Component> componentsToMove = new ArrayList<Component>(source.getActiveInstances());

        for (Component c : componentsToMove){
            source.removeActiveInstance(c);
            existingProtein.addActiveInstance(c);
            factory.getComponentDao().update(c);
        }

        ProteinDao proteinDao = factory.getProteinDao();
        proteinDao.update((ProteinImpl)source);
        proteinDao.update((ProteinImpl)existingProtein);
    }

    private ProteinTranscript createSpliceVariant(UniprotSpliceVariant spliceVariant, Protein proteinWithConflicts, Collection<Component> componentsToFix, DaoFactory factory, OutOfDateParticipantFoundEvent evt){

        Protein spliceIntact = cloneProtein(factory, proteinWithConflicts);
        InteractorXref identity = ProteinUtils.getUniprotXref(spliceIntact);
        identity.setPrimaryId(spliceVariant.getPrimaryAc());
        spliceIntact.getAliases().clear();
        spliceIntact.getXrefs().clear();
        spliceIntact.addXref(identity);

        Institution owner = spliceIntact.getOwner();
        CvDatabase db = factory.getCvObjectDao( CvDatabase.class ).getByPsiMiRef( CvDatabase.INTACT_MI_REF );

        if (db == null){
            db = CvObjectUtils.createCvObject(owner, CvDatabase.class, CvDatabase.INTACT_MI_REF, CvDatabase.INTACT);
            factory.getCvObjectDao(CvDatabase.class).persist(db);
        }
        CvXrefQualifier isoformParent = factory.getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.ISOFORM_PARENT_MI_REF);

        if (isoformParent == null) {
            isoformParent = CvObjectUtils.createCvObject(owner, CvXrefQualifier.class, CvXrefQualifier.ISOFORM_PARENT_MI_REF, CvXrefQualifier.ISOFORM_PARENT);
            factory.getCvObjectDao(CvXrefQualifier.class).persist(isoformParent);
        }
        InteractorXref parent = new InteractorXref(owner, db, proteinWithConflicts.getAc(), isoformParent);
        spliceIntact.addXref(parent);

        factory.getProteinDao().update((ProteinImpl) spliceIntact);

        for (Component c : componentsToFix){
            proteinWithConflicts.removeActiveInstance(c);
            spliceIntact.addActiveInstance(c);
            c.setInteractorAc(spliceIntact.getAc());

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
    }

    private ProteinTranscript createFeatureChain(UniprotFeatureChain featureChain, Protein proteinWithConflicts, Collection<Component> componentsToFix, DaoFactory factory, OutOfDateParticipantFoundEvent evt){

        Protein chainIntact = cloneProtein(factory, proteinWithConflicts);
        InteractorXref identity = ProteinUtils.getUniprotXref(chainIntact);
        identity.setPrimaryId(featureChain.getPrimaryAc());
        chainIntact.getAliases().clear();
        chainIntact.getXrefs().clear();
        chainIntact.addXref(identity);

        Institution owner = chainIntact.getOwner();
        CvDatabase db = factory.getCvObjectDao( CvDatabase.class ).getByPsiMiRef( CvDatabase.INTACT_MI_REF );

        if (db == null){
            db = CvObjectUtils.createCvObject(owner, CvDatabase.class, CvDatabase.INTACT_MI_REF, CvDatabase.INTACT);
            factory.getCvObjectDao(CvDatabase.class).persist(db);
        }
        CvXrefQualifier chainParent = factory.getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.CHAIN_PARENT_MI_REF);

        if (chainParent == null) {
            chainParent = CvObjectUtils.createCvObject(owner, CvXrefQualifier.class, CvXrefQualifier.CHAIN_PARENT_MI_REF, CvXrefQualifier.CHAIN_PARENT);
            factory.getCvObjectDao(CvXrefQualifier.class).persist(chainParent);
        }
        InteractorXref parent = new InteractorXref(owner, db, proteinWithConflicts.getAc(), chainParent);
        chainIntact.addXref(parent);

        factory.getCvObjectDao(CvXrefQualifier.class).persist(chainParent);

        for (Component c : componentsToFix){
            proteinWithConflicts.removeActiveInstance(c);
            chainIntact.addActiveInstance(c);
            c.setInteractorAc(chainIntact.getAc());

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
    }

    public ProteinTranscript fixParticipantWithRangeConflicts(OutOfDateParticipantFoundEvent evt, boolean createDeprecatedParticipant){

        if (evt.getSource() instanceof ProteinUpdateProcessor){
            ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

            processor.fireOnOutOfDateParticipantFound(evt);
        }

        DaoFactory factory = evt.getDataContext().getDaoFactory();

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
                            if (match.equals(p.getUniprotVariant()) && !ProteinTools.isSequenceChanged(p.getProtein().getSequence(), sequenceWithConflicts)){
                                moveInteractionsOfExistingProtein(protein, p.getProtein(), factory);
                                return p;
                            }
                        }

                        for (ProteinTranscript p : evt.getSecondaryIsoforms()){
                            if (match.equals(p.getUniprotVariant()) && !ProteinTools.isSequenceChanged(p.getProtein().getSequence(), sequenceWithConflicts)){
                                moveInteractionsOfExistingProtein(protein, p.getProtein(), factory);
                                return p;
                            }
                        }

                        return createSpliceVariant((UniprotSpliceVariant) match, protein, componentsToFix, factory, evt);
                    }
                    else if (match instanceof UniprotFeatureChain){

                        for (ProteinTranscript p : evt.getPrimaryFeatureChains()){
                            if (match.equals(p.getUniprotVariant()) && !ProteinTools.isSequenceChanged(p.getProtein().getSequence(), sequenceWithConflicts)){
                                moveInteractionsOfExistingProtein(protein, p.getProtein(), factory);
                                return p;
                            }
                        }

                        return createFeatureChain((UniprotFeatureChain) match, protein, componentsToFix, factory, evt);
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

        Collection<Component> componentsToFix = evt.getComponentsToFix();
        Protein protein = evt.getProteinWithConflicts();

        Protein noUniprotUpdate = cloneProtein(factory, protein);

        addAnnotations(noUniprotUpdate, factory);

        for (Component component : componentsToFix){
            protein.removeActiveInstance(component);
            noUniprotUpdate.addActiveInstance(component);
            component.setInteractorAc(noUniprotUpdate.getAc());
            factory.getComponentDao().update(component);
        }

        factory.getProteinDao().update((ProteinImpl) protein);
        factory.getProteinDao().update((ProteinImpl) noUniprotUpdate);

        if (evt.getSource() instanceof ProteinUpdateProcessor){
            ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

            processor.fireOnProteinCreated(new ProteinEvent(processor, evt.getDataContext(), noUniprotUpdate));
        }

        return new ProteinTranscript(noUniprotUpdate, null);
    }

    private Protein cloneProtein(DaoFactory factory, Protein protein) {
        Protein created = new ProteinImpl(protein.getOwner(), protein.getBioSource(), protein.getShortLabel(), protein.getCvInteractorType());

        factory.getProteinDao().persist((ProteinImpl) created);

        for (InteractorAlias a : protein.getAliases()){
            InteractorAlias copy = new InteractorAlias(a.getOwner(), created, a.getCvAliasType(), a.getName());
            copy.setParent(created);
            factory.getAliasDao(InteractorAlias.class).persist(copy);
            created.addAlias(copy);
        }

        for (InteractorXref x : protein.getXrefs()){
            InteractorXref copy = new InteractorXref(x.getOwner(), x.getCvDatabase(), x.getPrimaryId(), x.getCvXrefQualifier());
            copy.setParent(created);
            copy.setParentAc(created.getAc());
            factory.getXrefDao(InteractorXref.class).persist(copy);
            created.addXref(copy);
        }

        created.setCrc64(protein.getCrc64());
        created.setSequence(protein.getSequence());

        factory.getProteinDao().update((ProteinImpl) created);
        return created;
    }

    private void addAnnotations(Protein protein, DaoFactory factory){

        CvTopic no_uniprot_update = factory.getCvObjectDao(CvTopic.class).getByShortLabel(CvTopic.NON_UNIPROT);

        if (no_uniprot_update == null){
            no_uniprot_update = CvObjectUtils.createCvObject(protein.getOwner(), CvTopic.class, null, CvTopic.NON_UNIPROT);
            factory.getCvObjectDao(CvTopic.class).persist(no_uniprot_update);
        }
        CvTopic caution = factory.getCvObjectDao(CvTopic.class).getByPsiMiRef(CvTopic.CAUTION_MI_REF);

        if (caution == null) {
            caution = CvObjectUtils.createCvObject(protein.getOwner(), CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);
            factory.getCvObjectDao(CvTopic.class).persist(caution);
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

        factory.getProteinDao().update((ProteinImpl) protein);
    }
}
