package uk.ac.ebi.intact.dbupdate.prot.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.core.persistence.dao.XrefDao;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.UpdateError;
import uk.ac.ebi.intact.dbupdate.prot.event.InvalidIntactParentFoundEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateErrorEvent;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.ProteinUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>06-Dec-2010</pre>
 */

public class IntactParentUpdater {

    private static final Log log = LogFactory.getLog( IntactParentUpdater.class );

    public boolean checkConsistencyProteinTranscript(ProteinEvent evt){
        Protein protein = evt.getProtein();

        boolean canBeUpdated = true;

        Collection<InteractorXref> isoformParentXRefs = ProteinUtils.extractIsoformParentCrossReferencesFrom(protein);
        Collection<InteractorXref> chainParentXRefs = ProteinUtils.extractChainParentCrossReferencesFrom(protein);

        Collection<String> parentAc = new ArrayList<String>();

        if (!isoformParentXRefs.isEmpty() || !chainParentXRefs.isEmpty()){
            if (!isoformParentXRefs.isEmpty() && !chainParentXRefs.isEmpty()){
                if (evt.getSource() instanceof ProteinUpdateProcessor ){
                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                    processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), "The protein " + protein.getAc() + " has " + isoformParentXRefs.size() + " " +
                            "isoform parents and " + chainParentXRefs + " chain parents.", UpdateError.both_isoform_and_chain_xrefs, protein, evt.getUniprotIdentity()));
                }
                canBeUpdated = false;
            }
            else {
                Collection<InteractorXref> parents = isoformParentXRefs.isEmpty() ? chainParentXRefs : isoformParentXRefs;
                ProteinDao proteinDao = evt.getDataContext().getDaoFactory().getProteinDao();
                XrefDao<InteractorXref> xRefDao = evt.getDataContext().getDaoFactory().getXrefDao(InteractorXref.class);

                if (parents.size() > 1){
                    if (evt.getSource() instanceof ProteinUpdateProcessor ){
                        ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                        processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), "The protein transcript " + protein.getAc() + " has "+parents.size()+" different intact parents."
                                , UpdateError.several_intact_parents, protein));
                    }
                    canBeUpdated = false;
                }

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
        else {
            if (evt.getSource() instanceof ProteinUpdateProcessor ){
                ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), "The protein transcript " + protein.getAc() + " doesn't have any parents."
                        , UpdateError.transcript_without_parent, protein));
            }
            canBeUpdated = false;
        }

        return canBeUpdated;
    }

    private void checkConsistencyOf(Collection<ProteinTranscript> transcriptsToReview, UpdateCaseEvent evt){
        Collection<ProteinTranscript> transcriptToDelete = new ArrayList<ProteinTranscript>();

        for (ProteinTranscript p : transcriptsToReview){
            Protein protein = p.getProtein();

            Collection<InteractorXref> isoformParentXRefs = ProteinUtils.extractIsoformParentCrossReferencesFrom(protein);
            Collection<InteractorXref> chainParentXRefs = ProteinUtils.extractChainParentCrossReferencesFrom(protein);

            Collection<String> parentAc = new ArrayList<String>();

            if (!isoformParentXRefs.isEmpty() && !chainParentXRefs.isEmpty()){
                if (evt.getSource() instanceof ProteinUpdateProcessor ){
                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                    processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), "The protein " + protein.getAc() + " has " + isoformParentXRefs.size() + " " +
                            "isoform parents and " + chainParentXRefs + " chain parents.", UpdateError.both_isoform_and_chain_xrefs, protein, evt.getUniprotServiceResult().getQuerySentToService()));
                }
                transcriptToDelete.add(p);
            }
            else {
                Collection<InteractorXref> parents = isoformParentXRefs.isEmpty() ? chainParentXRefs : isoformParentXRefs;
                ProteinDao proteinDao = evt.getDataContext().getDaoFactory().getProteinDao();
                XrefDao<InteractorXref> xRefDao = evt.getDataContext().getDaoFactory().getXrefDao(InteractorXref.class);

                if (parents.size() > 1){
                    if (evt.getSource() instanceof ProteinUpdateProcessor ){
                        ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                        processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), "The protein transcript " + protein.getAc() + " has "+parents.size()+" different intact parents."
                                , UpdateError.several_intact_parents, protein, evt.getUniprotServiceResult().getQuerySentToService()));

                    }
                    transcriptToDelete.add(p);
                }

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

    public void checkConsistencyOfAllTranscripts(UpdateCaseEvent evt){
        checkConsistencyOf(evt.getPrimaryIsoforms(), evt);
        checkConsistencyOf(evt.getSecondaryIsoforms(), evt);
        checkConsistencyOf(evt.getPrimaryFeatureChains(), evt);
    }
}
