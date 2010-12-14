package uk.ac.ebi.intact.dbupdate.prot.actions.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.core.persistence.dao.XrefDao;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.UpdateError;
import uk.ac.ebi.intact.dbupdate.prot.actions.IntactTranscriptParentUpdater;
import uk.ac.ebi.intact.dbupdate.prot.event.InvalidIntactParentFoundEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateErrorEvent;
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
    public boolean checkConsistencyProteinTranscript(ProteinEvent evt){
        // get the protein
        Protein protein = evt.getProtein();

        // boolean value to know if the protein can be updated
        boolean canBeUpdated = true;

        // collect the isoform parents
        Collection<InteractorXref> isoformParentXRefs = ProteinUtils.extractIsoformParentCrossReferencesFrom(protein);
        // collect the feature chain parents
        Collection<InteractorXref> chainParentXRefs = ProteinUtils.extractChainParentCrossReferencesFrom(protein);

        // list of parents acs
        Collection<String> parentAc = new ArrayList<String>();

        // the protein does ahave at least one parent xref
        if (!isoformParentXRefs.isEmpty() || !chainParentXRefs.isEmpty()){
            // if the protein has both isoform parent xrefs and chain parent xrefs, it is an error which will be logged in 'process_erros.csv'
            // this protein cannot be updated
            if (!isoformParentXRefs.isEmpty() && !chainParentXRefs.isEmpty()){
                if (evt.getSource() instanceof ProteinUpdateProcessor ){
                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                    processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), "The protein " + protein.getAc() + " has " + isoformParentXRefs.size() + " " +
                            "isoform parents and " + chainParentXRefs + " chain parents.", UpdateError.both_isoform_and_chain_xrefs, protein, evt.getUniprotIdentity()));
                }
                canBeUpdated = false;
            }
            // the transcript is either an isoform or a feature chain
            else {
                // get the collection of parent xrefs
                Collection<InteractorXref> parents = isoformParentXRefs.isEmpty() ? chainParentXRefs : isoformParentXRefs;
                ProteinDao proteinDao = evt.getDataContext().getDaoFactory().getProteinDao();
                XrefDao<InteractorXref> xRefDao = evt.getDataContext().getDaoFactory().getXrefDao(InteractorXref.class);

                // if we have more than one parent, we will check all the parents but it is an erro logged in 'process-errors.csv
                // // the protein cannot be updated because we don't know which parent to choose'
                if (parents.size() > 1){
                    if (evt.getSource() instanceof ProteinUpdateProcessor ){
                        ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                        processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), "The protein transcript " + protein.getAc() + " has "+parents.size()+" different intact parents."
                                , UpdateError.several_intact_parents, protein));
                    }
                    canBeUpdated = false;
                }

                // for each parent, we check that the parent ac is still valid in the database. If not, try to remap the parent ac
                for (InteractorXref parent : parents){
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
                            canBeUpdated = false;
                            if (evt.getSource() instanceof ProteinUpdateProcessor ){
                                ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                                processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), "The protein transcript " + protein.getAc() + " has a parent protein (" + parent.getPrimaryId() + "" +
                                        ") which doesn't exist in Intact anymore and no other proteins in intact refers to it as intact-secondary."
                                        , UpdateError.dead_parent_xref, protein));

                                InvalidIntactParentFoundEvent invalidEvent = new InvalidIntactParentFoundEvent(evt.getSource(), evt.getDataContext(), protein, parent.getPrimaryId(), parentAc);
                                processor.fireOnInvalidIntactParentFound(invalidEvent);
                            }
                        }
                        // only one protein in intact has the parent ac as intact-secondary.
                        // update the parent xref and log in 'invalid_parent.csv'
                        else if (remappedParents.size() == 1){
                            parentAc.add(remappedParents.iterator().next().getAc());
                            if (evt.getSource() instanceof ProteinUpdateProcessor ){
                                ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                                InvalidIntactParentFoundEvent invalidEvent = new InvalidIntactParentFoundEvent(evt.getSource(), evt.getDataContext(), protein, parent.getPrimaryId(), parentAc);
                                processor.fireOnInvalidIntactParentFound(invalidEvent);
                            }
                            parent.setPrimaryId(remappedParents.iterator().next().getAc());
                            xRefDao.update(parent);
                        }
                        // we have more than one protein having the parent ac as intact-secondary.
                        // we cannot choose, we log in 'process_errors.csv' and we cannot update the protein
                        else {
                            canBeUpdated = false;

                            for (Protein p : remappedParents){
                                parentAc.add(p.getAc());
                            }

                            if (evt.getSource() instanceof ProteinUpdateProcessor ){
                                ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                                processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), "The protein transcript " + protein.getAc() + " has a parent protein (" + parent.getPrimaryId() + "" +
                                        ") which doesn't exist in Intact anymore and for which the ac is the intact-secondary ac of "+remappedParents.size()+" other proteins."
                                        , UpdateError.several_intact_parents, protein));

                                InvalidIntactParentFoundEvent invalidEvent = new InvalidIntactParentFoundEvent(evt.getSource(), evt.getDataContext(), protein, parent.getPrimaryId(), parentAc);
                                processor.fireOnInvalidIntactParentFound(invalidEvent);
                            }
                        }
                    }
                }
            }
        }
        // transcript without parent xrefs : log in 'error.csv' but can be updated later
        else {
            if (evt.getSource() instanceof ProteinUpdateProcessor ){
                ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), "The protein transcript " + protein.getAc() + " doesn't have any parents."
                        , UpdateError.transcript_without_parent, protein));
            }
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
        // the list of protein transcript to remove from the update case event because impossible to update
        Collection<ProteinTranscript> transcriptToDelete = new ArrayList<ProteinTranscript>();

        // for each protein transcript to review
        for (ProteinTranscript p : transcriptsToReview){
            // get the protein
            Protein protein = p.getProtein();

            // get all possible parent xrefs
            Collection<InteractorXref> isoformParentXRefs = ProteinUtils.extractIsoformParentCrossReferencesFrom(protein);
            Collection<InteractorXref> chainParentXRefs = ProteinUtils.extractChainParentCrossReferencesFrom(protein);

            // list of parent acs for one protein
            Collection<String> parentAc = new ArrayList<String>();

            // the protein has both isoform and feature chain parents, it is an error.
            // the protein is removed from the proteins to update
            if (!isoformParentXRefs.isEmpty() && !chainParentXRefs.isEmpty()){
                if (evt.getSource() instanceof ProteinUpdateProcessor ){
                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                    processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), "The protein " + protein.getAc() + " has " + isoformParentXRefs.size() + " " +
                            "isoform parents and " + chainParentXRefs + " chain parents.", UpdateError.both_isoform_and_chain_xrefs, protein, evt.getUniprotServiceResult().getQuerySentToService()));
                }
                transcriptToDelete.add(p);
            }
            else {
                // get the collection of parent xrefs
                Collection<InteractorXref> parents = isoformParentXRefs.isEmpty() ? chainParentXRefs : isoformParentXRefs;
                ProteinDao proteinDao = evt.getDataContext().getDaoFactory().getProteinDao();
                XrefDao<InteractorXref> xRefDao = evt.getDataContext().getDaoFactory().getXrefDao(InteractorXref.class);

                // if we have several parents, the protein cannot be updated
                if (parents.size() > 1){
                    if (evt.getSource() instanceof ProteinUpdateProcessor ){
                        ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                        processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), "The protein transcript " + protein.getAc() + " has "+parents.size()+" different intact parents."
                                , UpdateError.several_intact_parents, protein, evt.getUniprotServiceResult().getQuerySentToService()));

                    }
                    transcriptToDelete.add(p);
                }
                // if only no parent has been found, add the protein to the list of protein transcripts without parents
                else if (parents.size() == 0){
                    if (evt.getSource() instanceof ProteinUpdateProcessor ){
                        ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                        processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), "The protein transcript " + protein.getAc() + " does not have any intact parents."
                                , UpdateError.transcript_without_parent, protein, evt.getUniprotServiceResult().getQuerySentToService()));

                    }
                    proteinWithoutParents.add(protein);
                }

                // check the consistencye of each parent xref
                for (InteractorXref parent : parents){
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
                                processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), "The protein transcript " + protein.getAc() + " has a parent protein (" + parent.getPrimaryId() + "" +
                                        ") which doesn't exist in Intact anymore and no other proteins in intact refers to it as intact-secondary."
                                        , UpdateError.dead_parent_xref, protein, evt.getUniprotServiceResult().getQuerySentToService()));

                                InvalidIntactParentFoundEvent invalidEvent = new InvalidIntactParentFoundEvent(evt.getSource(), evt.getDataContext(), protein, parent.getPrimaryId(), parentAc);
                                processor.fireOnInvalidIntactParentFound(invalidEvent);
                            }

                            if (!transcriptToDelete.contains(p)){
                                transcriptToDelete.add(p);
                            }
                        }
                        else if (remappedParents.size() == 1){
                            parentAc.add(remappedParents.iterator().next().getAc());

                            parent.setPrimaryId(remappedParents.iterator().next().getAc());
                            xRefDao.update(parent);

                            if (evt.getSource() instanceof ProteinUpdateProcessor ){
                                ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                                InvalidIntactParentFoundEvent invalidEvent = new InvalidIntactParentFoundEvent(evt.getSource(), evt.getDataContext(), protein, parent.getPrimaryId(), parentAc);
                                processor.fireOnInvalidIntactParentFound(invalidEvent);
                            }
                        }
                        else {
                            for (Protein p2 : remappedParents){
                                parentAc.add(p2.getAc());
                            }

                            if (evt.getSource() instanceof ProteinUpdateProcessor ){
                                ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                                processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), "The protein transcript " + protein.getAc() + " has a parent protein (" + parent.getPrimaryId() + "" +
                                        ") which doesn't exist in Intact anymore and for which the ac is the intact-secondary ac of "+remappedParents.size()+" other proteins."
                                        , UpdateError.several_intact_parents, protein, evt.getUniprotServiceResult().getQuerySentToService()));

                                InvalidIntactParentFoundEvent invalidEvent = new InvalidIntactParentFoundEvent(evt.getSource(), evt.getDataContext(), protein, parent.getPrimaryId(), parentAc);
                                processor.fireOnInvalidIntactParentFound(invalidEvent);
                            }

                            if (!transcriptToDelete.contains(p)){
                                transcriptToDelete.add(p);
                            }
                        }
                    }
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
    public void createParentXRefs(List<Protein> transcripts, Protein masterProtein, DataContext context, ProteinUpdateProcessor processor){
        DaoFactory factory = context.getDaoFactory();

        if (masterProtein != null){
            String masterAc = masterProtein.getAc();

            if (masterAc != null){
                CvDatabase db = factory.getCvObjectDao( CvDatabase.class ).getByPsiMiRef( CvDatabase.INTACT_MI_REF );

                if (db == null){
                    db = CvObjectUtils.createCvObject(masterProtein.getOwner(), CvDatabase.class, CvDatabase.INTACT_MI_REF, CvDatabase.INTACT);
                    factory.getCvObjectDao(CvDatabase.class).saveOrUpdate(db);
                }

                Collection<String> parentAcs = new ArrayList<String> (1);
                parentAcs.add(masterAc);

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
                        InvalidIntactParentFoundEvent invalidEvent = new InvalidIntactParentFoundEvent(processor, context, t, null, parentAcs);
                        processor.fireOnInvalidIntactParentFound(invalidEvent);

                        factory.getXrefDao(InteractorXref.class).persist(parent);
                        t.addXref(parent);

                        factory.getProteinDao().update((ProteinImpl) t);
                    }
                }
            }
        }
    }
}
