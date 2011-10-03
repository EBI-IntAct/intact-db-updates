package uk.ac.ebi.intact.dbupdate.prot.actions.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.core.persistence.dao.XrefDao;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessorConfig;
import uk.ac.ebi.intact.dbupdate.prot.errors.ProteinUpdateError;
import uk.ac.ebi.intact.dbupdate.prot.errors.ProteinUpdateErrorFactory;
import uk.ac.ebi.intact.dbupdate.prot.errors.UpdateError;
import uk.ac.ebi.intact.dbupdate.prot.actions.IntactTranscriptParentUpdater;
import uk.ac.ebi.intact.dbupdate.prot.event.InvalidIntactParentFoundEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateErrorEvent;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.service.IdentifierChecker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class checks, updates and create intact parent cross references when necessary (for isoforms and feature chains)
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>06-Dec-2010</pre>
 */

public class IntactTranscriptParentUpdaterImpl implements IntactTranscriptParentUpdater {

    /**
     * The logger of this class
     */
    private static final Log log = LogFactory.getLog( IntactTranscriptParentUpdaterImpl.class );

    /**
     *
     * @param evt : the event containing the protein transcript to check
     * @return true if the protein transcript has either a single isoform parent or a single chain parent
     * and if this parent ac still exists in the database. If not, will try to update it by looking for intact proteins
     * having the parent ac as secondary ac.
     */
    public boolean checkConsistencyProteinTranscript(ProteinEvent evt, List<Protein> transcriptsToReview){
        // get the errorFactory in case we may need it
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        ProteinUpdateErrorFactory errorFactory = config.getErrorFactory();

        // get the protein
        Protein protein = evt.getProtein();

        // boolean value to know if the protein can be updated
        boolean canBeUpdated = true;

        // collect the isoform parents
        Collection<InteractorXref> isoformParentXRefs = ProteinUtils.extractIsoformParentCrossReferencesFrom(protein);
        // collect the feature chain parents
        Collection<InteractorXref> chainParentXRefs = ProteinUtils.extractChainParentCrossReferencesFrom(protein);

        // the protein does ahave at least one parent xref
        if (!isoformParentXRefs.isEmpty() || !chainParentXRefs.isEmpty()){
            // if the protein has both isoform parent xrefs and chain parent xrefs, it is an error which will be logged in 'process_erros.csv'
            // this protein cannot be updated
            if (!isoformParentXRefs.isEmpty() && !chainParentXRefs.isEmpty()){
                if (evt.getSource() instanceof ProteinUpdateProcessor ){
                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                    ProteinUpdateError bothParents = errorFactory.createInvalidCollectionOfParentsError(protein.getAc(), UpdateError.both_isoform_and_chain_xrefs, isoformParentXRefs, chainParentXRefs);

                    processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), bothParents, protein, evt.getUniprotIdentity()));
                }
                canBeUpdated = false;
            }
            // the transcript is either an isoform or a feature chain
            else {
                // get the collection of parent xrefs
                Collection<InteractorXref> parents = isoformParentXRefs.isEmpty() ? chainParentXRefs : isoformParentXRefs;
                ProteinDao proteinDao = evt.getDataContext().getDaoFactory().getProteinDao();
                XrefDao<InteractorXref> xRefDao = evt.getDataContext().getDaoFactory().getXrefDao(InteractorXref.class);

                // initial number of parents before clean up
                int totalNumberOfParents = parents.size();

                // for each parent, we check that the parent ac is still valid in the database. If not, try to remap the parent ac
                for (InteractorXref parent : parents){

                    if (parent.getPrimaryId().equals(protein.getAc())){
                        if (evt.getSource() instanceof ProteinUpdateProcessor ){
                            ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                            ProteinUpdateError invalidParent = errorFactory.createInvalidParentXrefError(protein.getAc(), parent.getPrimaryId(), "The parent xref of the protein refers to itself.");
                            processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), invalidParent, protein, evt.getUniprotIdentity()));

                            // now delete the invalid xref
                            ProteinTools.deleteInteractorXRef(protein, evt.getDataContext(), parent);
                            totalNumberOfParents --;
                        }
                        transcriptsToReview.add(protein);
                    }
                    else {
                        // the protein parent
                        Protein par = proteinDao.getByAc(parent.getPrimaryId());

                        if (par == null){

                            DaoFactory factory = evt.getDataContext().getDaoFactory();

                            // get the intact database
                            CvDatabase intact = factory.getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.INTACT_MI_REF);

                            if (intact == null){
                                intact = CvObjectUtils.createCvObject(parent.getOwner(), CvDatabase.class, CvDatabase.INTACT_MI_REF, CvDatabase.INTACT);
                                factory.getCvObjectDao(CvDatabase.class).persist(intact);
                            }

                            // get the intact-secondary xref qualifier
                            CvXrefQualifier intactSecondary = factory.getCvObjectDao(CvXrefQualifier.class).getByShortLabel("intact-secondary");

                            if (intactSecondary == null){
                                intactSecondary = CvObjectUtils.createCvObject(parent.getOwner(), CvXrefQualifier.class, null, "intact-secondary");
                                factory.getCvObjectDao(CvXrefQualifier.class).persist(intactSecondary);
                            }

                            // collect all proteins in intact having the parent ac as intact-secondary
                            List<ProteinImpl> remappedParents = proteinDao.getByXrefLike(intact, intactSecondary, parent.getPrimaryId());

                            // the protein ac cannot be remapped, an error is logged in 'process_errors.csv'
                            // the protein cannot be updated
                            if (remappedParents.size() == 0){
                                if (evt.getSource() instanceof ProteinUpdateProcessor ){
                                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                                    ProteinUpdateError invalidParent = errorFactory.createInvalidParentXrefError(protein.getAc(), parent.getPrimaryId(), "The parent xref of the protein refers to a dead protein in IntAct.");
                                    processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), invalidParent, protein, evt.getUniprotIdentity()));
                                }
                                // now delete the invalid xref
                                ProteinTools.deleteInteractorXRef(protein, evt.getDataContext(), parent);
                                totalNumberOfParents --;

                                // add the transcript to the list of transcripts to review
                                transcriptsToReview.add(protein);
                            }
                            // only one protein in intact has the parent ac as intact-secondary.
                            // update the parent xref and log in 'invalid_parent.csv'
                            else if (remappedParents.size() == 1){

                                String parentAc = remappedParents.iterator().next().getAc();
                                if (evt.getSource() instanceof ProteinUpdateProcessor ){
                                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                                    InvalidIntactParentFoundEvent invalidEvent = new InvalidIntactParentFoundEvent(evt.getSource(), evt.getDataContext(), protein, evt.getUniprotIdentity(), parent.getPrimaryId(), parentAc);
                                    processor.fireOnInvalidIntactParentFound(invalidEvent);
                                }
                                parent.setPrimaryId(parentAc);
                                xRefDao.update(parent);
                            }
                            // we have more than one protein having the parent ac as intact-secondary.
                            // we cannot choose, we log in 'process_errors.csv' and we cannot update the protein
                            else {
                                if (evt.getSource() instanceof ProteinUpdateProcessor ){
                                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                                    ProteinUpdateError invalidParent = errorFactory.createInvalidParentXrefError(protein.getAc(), parent.getPrimaryId(), "The parent xref of the protein refers to a dead protein in IntAct which has been demerged with several intact proteins.");
                                    processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), invalidParent, protein, evt.getUniprotIdentity()));
                                }

                                // now delete the invalid xref
                                ProteinTools.deleteInteractorXRef(protein, evt.getDataContext(), parent);
                                totalNumberOfParents --;
                                // add the transcript to the list of transcripts to review
                                transcriptsToReview.add(protein);
                            }
                        }
                        else {
                            if (ProteinUtils.isFeatureChain(par) || ProteinUtils.isSpliceVariant(par)){
                                if (evt.getSource() instanceof ProteinUpdateProcessor ){
                                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                                    ProteinUpdateError invalidParent = errorFactory.createInvalidParentXrefError(protein.getAc(), parent.getPrimaryId(), "The parent xref of the protein refers to an isoform or feature chain in IntAct which is not a master protein.");
                                    processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), invalidParent, protein, evt.getUniprotIdentity()));

                                }
                                // now delete the invalid xref
                                ProteinTools.deleteInteractorXRef(protein, evt.getDataContext(), parent);
                                totalNumberOfParents --;
                                // add the transcript to the list of transcripts to review
                                transcriptsToReview.add(protein);
                            }
                        }
                    }
                }

                // the protein cannot be updated because we don't know which parent to choose'
                if (totalNumberOfParents > 1){
                    if (evt.getSource() instanceof ProteinUpdateProcessor ){
                        ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                        ProteinUpdateError severalParents = errorFactory.createInvalidCollectionOfParentsError(protein.getAc(), UpdateError.several_intact_parents, isoformParentXRefs, chainParentXRefs);
                        processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), severalParents, protein, evt.getUniprotIdentity()));
                    }
                    canBeUpdated = false;
                }
            }
        }
        // transcript without parent xrefs : can be updated later
        else {
            transcriptsToReview.add(protein);
        }

        return canBeUpdated;
    }

    /**
     * For each protein transcript, check if the parent xrefs are valid, if not try to update it. If not possible, remove the protein
     * transcript from the update case as it cannot be updated.
     *
     * For each protein transcript found without parent xref, add it to the list of protein transcripts without parent xrefs
     * @param transcriptsToReview : collection of transcript to check
     * @param evt : update case event containing the list of all proteins in intact matching a single uniprot entry
     * @param proteinWithoutParents : the list of protein transcript without parents to fill
     */
    private void checkConsistencyOf(Collection<ProteinTranscript> transcriptsToReview, UpdateCaseEvent evt, List<Protein> proteinWithoutParents){
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        ProteinUpdateErrorFactory errorFactory = config.getErrorFactory();

        // the list of protein transcript to remove from the update case event because impossible to update
        Collection<ProteinTranscript> transcriptToDelete = new ArrayList<ProteinTranscript>();

        // for each protein transcript to review
        for (ProteinTranscript p : transcriptsToReview){
            // get the protein
            Protein protein = p.getProtein();

            // get all possible parent xrefs
            Collection<InteractorXref> isoformParentXRefs = ProteinUtils.extractIsoformParentCrossReferencesFrom(protein);
            Collection<InteractorXref> chainParentXRefs = ProteinUtils.extractChainParentCrossReferencesFrom(protein);

            // the protein has both isoform and feature chain parents, it is an error.
            // the protein is removed from the proteins to update
            if (!isoformParentXRefs.isEmpty() && !chainParentXRefs.isEmpty()){
                if (evt.getSource() instanceof ProteinUpdateProcessor ){
                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                    ProteinUpdateError bothIsoformAnChain = errorFactory.createInvalidCollectionOfParentsError(protein.getAc(), UpdateError.both_isoform_and_chain_xrefs, isoformParentXRefs, chainParentXRefs);

                    processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), bothIsoformAnChain, protein, evt.getQuerySentToService()));
                }
                transcriptToDelete.add(p);
            }
            else {
                // get the collection of parent xrefs
                Collection<InteractorXref> parents = isoformParentXRefs.isEmpty() ? chainParentXRefs : isoformParentXRefs;
                ProteinDao proteinDao = evt.getDataContext().getDaoFactory().getProteinDao();
                XrefDao<InteractorXref> xRefDao = evt.getDataContext().getDaoFactory().getXrefDao(InteractorXref.class);

                // initial number of parents before clean up
                int totalNumberOfParents = parents.size();

                // if only no parent has been found, add the protein to the list of protein transcripts without parents
                if (parents.size() == 0){
                    proteinWithoutParents.add(protein);
                }

                // check the consistencye of each parent xref
                for (InteractorXref parent : parents){
                    if (parent.getPrimaryId().equals(protein.getAc())){
                        if (evt.getSource() instanceof ProteinUpdateProcessor ){
                            ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                            ProteinUpdateError invalidParent = errorFactory.createInvalidParentXrefError(protein.getAc(), parent.getPrimaryId(), "The parent xref of the protein refers to itself.");
                            processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), invalidParent, protein, evt.getQuerySentToService()));
                        }

                        if (!proteinWithoutParents.contains(protein)){
                            proteinWithoutParents.add(protein);
                        }
                    }
                    else{
                        Protein par = proteinDao.getByAc(parent.getPrimaryId());

                        if (par == null){
                            DaoFactory factory = evt.getDataContext().getDaoFactory();
                            CvDatabase intact = factory.getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.INTACT_MI_REF);

                            if (intact == null){
                                intact = CvObjectUtils.createCvObject(parent.getOwner(), CvDatabase.class, CvDatabase.INTACT_MI_REF, CvDatabase.INTACT);
                                factory.getCvObjectDao(CvDatabase.class).persist(intact);
                            }

                            CvXrefQualifier intactSecondary = factory.getCvObjectDao(CvXrefQualifier.class).getByShortLabel("intact-secondary");

                            if (intactSecondary == null){
                                intactSecondary = CvObjectUtils.createCvObject(parent.getOwner(), CvXrefQualifier.class, null, "intact-secondary");
                                factory.getCvObjectDao(CvXrefQualifier.class).persist(intactSecondary);
                            }

                            List<ProteinImpl> remappedParents = proteinDao.getByXrefLike(intact, intactSecondary, parent.getPrimaryId());

                            if (remappedParents.size() == 0){
                                if (evt.getSource() instanceof ProteinUpdateProcessor ){
                                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                                    ProteinUpdateError invalidParent = errorFactory.createInvalidParentXrefError(protein.getAc(), parent.getPrimaryId(), "The parent xref of the protein refers to a dead protein in IntAct.");
                                    processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), invalidParent, protein, evt.getQuerySentToService()));
                                }

                                // now delete the invalid xref
                                ProteinTools.deleteInteractorXRef(protein, evt.getDataContext(), parent);
                                totalNumberOfParents --;

                                if (!proteinWithoutParents.contains(protein)){
                                    proteinWithoutParents.add(protein);
                                }
                            }
                            else if (remappedParents.size() == 1){
                                String parentAc = remappedParents.iterator().next().getAc();

                                if (evt.getSource() instanceof ProteinUpdateProcessor ){
                                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                                    String uniprot = evt.getProtein() != null ? evt.getProtein().getPrimaryAc() : evt.getQuerySentToService();

                                    InvalidIntactParentFoundEvent invalidEvent = new InvalidIntactParentFoundEvent(evt.getSource(), evt.getDataContext(), protein, uniprot, parent.getPrimaryId(), parentAc);
                                    processor.fireOnInvalidIntactParentFound(invalidEvent);
                                }

                                parent.setPrimaryId(remappedParents.iterator().next().getAc());
                                xRefDao.update(parent);
                            }
                            else {

                                if (evt.getSource() instanceof ProteinUpdateProcessor ){
                                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                                    ProteinUpdateError invalidParent = errorFactory.createInvalidParentXrefError(protein.getAc(), parent.getPrimaryId(), "The parent xref of the protein refers to a dead protein in IntAct which has been demerged with several intact proteins.");
                                    processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), invalidParent, protein, evt.getQuerySentToService()));
                                }

                                // now delete the invalid xref
                                ProteinTools.deleteInteractorXRef(protein, evt.getDataContext(), parent);
                                totalNumberOfParents --;

                                if (!transcriptToDelete.contains(p)){
                                    transcriptToDelete.add(p);
                                }
                            }
                        }
                        else {
                            if (ProteinUtils.isFeatureChain(par) || ProteinUtils.isSpliceVariant(par)){
                                if (evt.getSource() instanceof ProteinUpdateProcessor ){
                                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                                    ProteinUpdateError invalidParent = errorFactory.createInvalidParentXrefError(protein.getAc(), parent.getPrimaryId(), "The parent xref of the protein refers to a feature chain or isoform in IntAct but not to a master protein.");
                                    processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), invalidParent, protein, evt.getQuerySentToService()));
                                }

                                // now delete the invalid xref
                                ProteinTools.deleteInteractorXRef(protein, evt.getDataContext(), parent);
                                totalNumberOfParents --;

                                if (!proteinWithoutParents.contains(protein)){
                                    proteinWithoutParents.add(protein);
                                }
                            }
                        }
                    }
                }

                // if we have several parents, the protein cannot be updated
                if (totalNumberOfParents > 1){
                    if (evt.getSource() instanceof ProteinUpdateProcessor ){
                        ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                        ProteinUpdateError severalParents = errorFactory.createInvalidCollectionOfParentsError(protein.getAc(), UpdateError.several_intact_parents, isoformParentXRefs, chainParentXRefs);

                        processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), severalParents, protein, evt.getQuerySentToService()));

                    }
                    transcriptToDelete.add(p);
                }
            }
        }

        transcriptsToReview.removeAll(transcriptToDelete);
    }

    /**
     *
     * @param evt : evt containing all the proteins attached to a single uniprot entry
     * @return the list of protein transcripts without any parents.
     * Remove all protein transcripts with invalid parents from the list of proteins to update
     */
    public List<Protein> checkConsistencyOfAllTranscripts(UpdateCaseEvent evt){
        List<Protein> proteinTranscriptsWithoutParent = new ArrayList<Protein>();

        checkConsistencyOf(evt.getPrimaryIsoforms(), evt, proteinTranscriptsWithoutParent);
        checkConsistencyOf(evt.getSecondaryIsoforms(), evt, proteinTranscriptsWithoutParent);
        checkConsistencyOf(evt.getPrimaryFeatureChains(), evt, proteinTranscriptsWithoutParent);

        return proteinTranscriptsWithoutParent;
    }

    /**
     * Create a valid parent xref for this protein transcript
     * @param transcripts
     * @param masterProtein
     * @param context
     * @param processor
     */
    public void createParentXRefs(List<Protein> transcripts, Protein masterProtein, String uniprot, DataContext context, ProteinUpdateProcessor processor){
        DaoFactory factory = context.getDaoFactory();

        if (masterProtein != null){
            String masterAc = masterProtein.getAc();

            if (masterAc != null){
                CvDatabase db = factory.getCvObjectDao( CvDatabase.class ).getByPsiMiRef( CvDatabase.INTACT_MI_REF );

                if (db == null){
                    db = CvObjectUtils.createCvObject(masterProtein.getOwner(), CvDatabase.class, CvDatabase.INTACT_MI_REF, CvDatabase.INTACT);
                    factory.getCvObjectDao(CvDatabase.class).saveOrUpdate(db);
                }

                for (Protein t : transcripts){
                    InteractorXref uniprotIdentity = ProteinUtils.getUniprotXref(t);

                    CvXrefQualifier parentXRef = null;

                    if (IdentifierChecker.isSpliceVariantId(uniprotIdentity.getPrimaryId())){
                        parentXRef = factory.getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.ISOFORM_PARENT_MI_REF);

                        if (parentXRef == null) {
                            parentXRef = CvObjectUtils.createCvObject(masterProtein.getOwner(), CvXrefQualifier.class, CvXrefQualifier.ISOFORM_PARENT_MI_REF, CvXrefQualifier.ISOFORM_PARENT);
                            factory.getCvObjectDao(CvXrefQualifier.class).saveOrUpdate(parentXRef);
                        }
                    }
                    else {
                        parentXRef = factory.getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.CHAIN_PARENT_MI_REF);

                        if (parentXRef == null) {
                            parentXRef = CvObjectUtils.createCvObject(masterProtein.getOwner(), CvXrefQualifier.class, CvXrefQualifier.CHAIN_PARENT_MI_REF, CvXrefQualifier.CHAIN_PARENT);
                            factory.getCvObjectDao(CvXrefQualifier.class).saveOrUpdate(parentXRef);
                        }
                    }

                    InteractorXref parent = new InteractorXref(t.getOwner(), db, masterAc, parentXRef);

                    if (!t.getXrefs().contains(parent)){

                        InvalidIntactParentFoundEvent invalidEvent = new InvalidIntactParentFoundEvent(processor, context, t, uniprot, null, masterAc);
                        processor.fireOnInvalidIntactParentFound(invalidEvent);

                        factory.getXrefDao(InteractorXref.class).persist(parent);
                        t.addXref(parent);

                        // update the transcript
                        factory.getProteinDao().update((ProteinImpl) t);
                    }
                }
            }
        }
    }
}
