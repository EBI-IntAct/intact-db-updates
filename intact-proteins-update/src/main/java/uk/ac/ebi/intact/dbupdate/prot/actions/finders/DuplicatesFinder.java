package uk.ac.ebi.intact.dbupdate.prot.actions.finders;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.commons.util.Crc64;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.actions.fixers.DuplicatesFixer;
import uk.ac.ebi.intact.dbupdate.prot.event.DuplicatesFoundEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.dbupdate.prot.model.ProteinTranscript;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Duplicate detection for proteins.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class DuplicatesFinder {

    /**
     * The logger of this class
     */
    private static final Log log = LogFactory.getLog( DuplicatesFixer.class );

    /**
     * In the update case event, if the list of primary proteins and secondary proteins contain more than 1 protein, returns
     * a DuplicatesFoundEvent.
     * It is not necessary to check the uniprot ac because a update case event is supposed to represent a single uniprot entry and so
     * we should have only one master protein matching this uniprot entry.
     * @param evt : the update case event containing the list of primary proteins and secondary proteins
     * @return DuplicatesFoundEvent if the evt has more than one primary protein and one or more secondary proteins, null otherwise
     * @throws ProcessorException
     */
    public DuplicatesFoundEvent findProteinDuplicates(UpdateCaseEvent evt) throws ProcessorException {

        // list of protein duplicates
        List<Protein> possibleDuplicates = new ArrayList<>(evt.getPrimaryProteins().size() + evt.getSecondaryIsoforms().size());

        // add all primary proteins and secondary proteins
        possibleDuplicates.addAll(evt.getPrimaryProteins());
        possibleDuplicates.addAll(evt.getSecondaryProteins());

        // if there are duplicates (more than 1 result), check and fix when necessary
        if (possibleDuplicates.size() > 1) {

            // get the uniprot proteins in case of range update
            UniprotProtein uniprotProtein = evt.getProtein();

            String uniprotSequence = uniprotProtein.getSequence();
            String uniprotCrc64 = uniprotProtein.getCrc64();
            String organism = uniprotProtein.getOrganism() != null ? String.valueOf(uniprotProtein.getOrganism().getTaxid()) : null;

            // in case of master protein, we merge the duplicates
            DuplicatesFoundEvent duplEvt = new DuplicatesFoundEvent( evt.getSource(), evt.getDataContext(), possibleDuplicates, uniprotSequence, uniprotCrc64, uniprotProtein.getPrimaryAc(), organism);
            return duplEvt;
        }

        // if no duplicates, there is no DuplicateFoundEvent to return
        return null;
    }

    /**
     * In the update case event, if the list of primary isoforms and secondary isoforms contain more than 1 protein, it means that we have possibly duplicates
     * of the same isoform. As a uniprot entry can have several different isoforms and a same isoform can be remapped to several uniprot entries (parents),
     * a duplicate of an isoform has :
     * - same uniprot identity
     * - same intact isoform parent
     * @param evt : the update case event containing the list of primary isoforms and secondary isoforms
     * @return a collection of DuplicateFoundEvent for each possible set of duplicates (empty list if no duplicates)
     * @throws ProcessorException
     */
    public Collection<DuplicatesFoundEvent> findIsoformDuplicates(UpdateCaseEvent evt) throws ProcessorException {

        // the possible duplicates are both in the primary isoforms and secondary isoforms
        List<ProteinTranscript> possibleDuplicates = new ArrayList<>(evt.getPrimaryIsoforms().size() + evt.getSecondaryIsoforms().size());

        possibleDuplicates.addAll(evt.getPrimaryIsoforms());
        possibleDuplicates.addAll(evt.getSecondaryIsoforms());

        return findProteinTranscriptDuplicates(possibleDuplicates, evt.getDataContext(), (ProteinUpdateProcessor) evt.getSource(), true);
    }

    /**
     * In the update case event, if the list of primary feature chains contains more than 1 protein, it means that we have possibly duplicates
     * of the same feature chain. As a uniprot entry can have several different feature chains and a same feature chain can be remapped to several uniprot entries (parents),
     * a duplicate of a feature chain has :
     * - same uniprot identity
     * - same intact chain parent
     * @param evt : the update case event containing the list of primary proteins and secondary proteins
     * @return a collection of DuplicateFoundEvent for each possible set of duplicates (empty list if no duplicates)
     * @throws ProcessorException
     */
    public Collection<DuplicatesFoundEvent> findFeatureChainDuplicates(UpdateCaseEvent evt) throws ProcessorException {

        // the possible duplicates are in the list of primary feature chains
        List<ProteinTranscript> possibleDuplicates = new ArrayList<>(evt.getPrimaryFeatureChains());

        return findProteinTranscriptDuplicates(possibleDuplicates, evt.getDataContext(), (ProteinUpdateProcessor) evt.getSource(), false);

    }

    /**
     *
     * @param parents : the list of parent xrefs of one transcript
     * @param parentsToCompare : the list of parent xrefs of the other transcript
     * @return true if both collections contain the same cross references
     */
    private boolean hasSameParents(Collection<InteractorXref> parents, Collection<InteractorXref> parentsToCompare){
        // if the collections of xrefs don't have the same length, the list of parents are differents
        if (parentsToCompare.size() != parents.size()){
            return false;
        }
        // both collections have the same length
        else {
            for (InteractorXref refParent : parents){
                boolean hasFoundParent = false;

                for (InteractorXref ref : parentsToCompare){
                    if (ref.getPrimaryId().equals(refParent.getPrimaryId())){
                        hasFoundParent = true;
                        break;
                    }
                }

                if (!hasFoundParent){
                    return false;
                }
            }
        }

        return true;
    }

    /**
     *
     * @param possibleDuplicates  : the list containing the possible duplicates of a same transcript
     * @param context : the datacontext
     * @param processor : the proteinProcessor
     * @param isSpliceVariant : a boolean value to indicate if the transcript is a splice variant (true) or a feature chain (false)
     * @return  the collection of duplicetFoundEvent for each set of duplicated transcript in the list of possible duplicates
     * @throws ProcessorException
     */
    private Collection<DuplicatesFoundEvent> findProteinTranscriptDuplicates(List<ProteinTranscript> possibleDuplicates, DataContext context, ProteinUpdateProcessor processor, boolean isSpliceVariant) throws ProcessorException {
        // the list containing the duplicateFoundEvents
        Collection<DuplicatesFoundEvent> duplicateEvents = new ArrayList<>();

        // if there are possible duplicates (more than 1 result), check and fix when necessary
        if (possibleDuplicates.size() > 1) {

            // the collection containing all the possible duplicates
            Collection<ProteinTranscript> totalProteins = new ArrayList<>(possibleDuplicates);

            // the collection which will contain the duplicates of a same protein transcript
            Collection<ProteinTranscript> duplicates = new ArrayList<>(possibleDuplicates.size());

            // while the list of possible duplicates has not been fully treated, we need to check the duplicates
            while (totalProteins.size() > 0){
                // clear the list of duplicates of a same transcript
                duplicates.clear();

                // pick the first protein of the list and add it in the list of duplicates
                Iterator<ProteinTranscript> iterator = totalProteins.iterator();

                ProteinTranscript trans = iterator.next();
                Protein protToCompare = trans.getProtein();

                // get the uniprot identity of this protein
                InteractorXref firstIdentity = ProteinUtils.getUniprotXref(protToCompare);
                String firstUniprotAc = null;

                if (firstIdentity != null){
                    firstUniprotAc = firstIdentity.getPrimaryId();
                }

                // this first protein represents a uniprot transcript and is added to the list of duplicates of this transcript
                duplicates.add(trans);

                // extract the parents of this protein
                Collection<InteractorXref> transcriptParent;

                // if splice variant, the isoform-parents
                if (isSpliceVariant){
                    transcriptParent = ProteinUtils.extractIsoformParentCrossReferencesFrom(protToCompare);
                }
                // if feature chain, the chain-parents
                else {
                    transcriptParent = ProteinUtils.extractChainParentCrossReferencesFrom(protToCompare);
                }

                // we compare the parents of this first protein against the parents of the other proteins
                while (iterator.hasNext()){
                    // we extract the parents of the next protein to compare
                    ProteinTranscript trans2 = iterator.next();
                    Protein proteinCompared = trans2.getProtein();

                    // get the uniprot identity of this protein
                    InteractorXref secondIdentity = ProteinUtils.getUniprotXref(proteinCompared);
                    String secondUniprotAc = null;

                    if (secondIdentity != null){
                        secondUniprotAc = secondIdentity.getPrimaryId();
                    }

                    // if both uniprot identities are identical or null, we may have a duplicate. Need to check the parents
                    if ((firstUniprotAc != null && secondUniprotAc != null && firstUniprotAc.equalsIgnoreCase(secondUniprotAc)) || (firstUniprotAc == null && secondUniprotAc == null)){
                        Collection<InteractorXref> transcriptParents2;

                        if (isSpliceVariant){
                            transcriptParents2 = ProteinUtils.extractIsoformParentCrossReferencesFrom(proteinCompared);
                        }
                        else {
                            transcriptParents2 = ProteinUtils.extractChainParentCrossReferencesFrom(proteinCompared);
                        }

                        // if the parents are identical, we add the protein to the list of duplicates
                        if (hasSameParents(transcriptParent, transcriptParents2)){
                            duplicates.add(trans2);
                        }
                    }
                }

                // if we have more than two proteins in the duplicate list, we merge them
                if (duplicates.size() > 1){
                    // get the uniprot transcript
                    UniprotProteinTranscript transcript = trans.getUniprotProteinTranscript();

                    // set the uniprot sequence and CRC64 of the event
                    String uniprotSequence = null;
                    String uniprotCrc64 = null;
                    String primaryAc = null;
                    String organism = null;

                    if (transcript != null){
                        uniprotSequence = transcript.getSequence();
                        uniprotCrc64 = Crc64.getCrc64(uniprotSequence);
                        primaryAc = transcript.getPrimaryAc();
                        organism = transcript.getOrganism() != null ? String.valueOf(transcript.getOrganism().getTaxid()) : null;
                    }

                    // list of duplicates
                    Collection<Protein> duplicateToFix = new ArrayList<>(duplicates.size());

                    for (ProteinTranscript t : duplicates){
                        duplicateToFix.add(t.getProtein());
                    }

                    // create the DuplicateFoundEvent and add it to the list of duplicateFoundEvent
                    DuplicatesFoundEvent duplEvt = new DuplicatesFoundEvent( processor,context,duplicateToFix, uniprotSequence, uniprotCrc64, primaryAc, organism);
                    duplicateEvents.add(duplEvt);
                }

                // we remove the processed proteins from the list of protein to process
                totalProteins.removeAll(duplicates);
            }
        }

        return duplicateEvents;
    }
}