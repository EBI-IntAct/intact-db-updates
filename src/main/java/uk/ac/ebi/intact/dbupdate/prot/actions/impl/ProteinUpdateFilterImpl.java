package uk.ac.ebi.intact.dbupdate.prot.actions.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.dbupdate.prot.*;
import uk.ac.ebi.intact.dbupdate.prot.actions.ProteinUpdateFilter;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.dbupdate.prot.UpdateError;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateErrorEvent;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.util.ProteinUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * The protein update filter which is looking at no uniprot proteins or proteins with several different uniprot
 * identities and filter them before the update
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03-Nov-2010</pre>
 */

public class ProteinUpdateFilterImpl implements ProteinUpdateFilter{

    /**
     * The logger for this class
     */
    private static final Log log = LogFactory.getLog( ProteinUpdateFilterImpl.class );

    /**
     * 
     * @param evt : event containing the protein to filter
     * @return the unique uniprot identity of this protein if it exists and the protein is
     * not no-uniprot-update and the protein doesn't have different uniprot identities, null otherwise
     * @throws ProcessorException
     */
    public String filterOnUniprotIdentity(ProteinEvent evt) throws ProcessorException {

        // the protein to look at
        Protein protein = evt.getProtein();

        // filter 'no-uniprot-update' proteins
        if (!ProteinUtils.isFromUniprot(protein)) {

            if (evt.getSource() instanceof ProteinUpdateProcessor) {
                final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                updateProcessor.fireNonUniprotProteinFound(evt);
            }
        }
        else {
            Set<InteractorXref> uniprotIdentities = ProteinTools.getDistinctUniprotIdentities(protein);

            // no uniprot identity, it is a protein not from uniprot
            if (uniprotIdentities.size() == 0){
                if (evt.getSource() instanceof ProteinUpdateProcessor) {
                    final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                    updateProcessor.fireNonUniprotProteinFound(evt);
                }
            }
            else if (uniprotIdentities.size() > 1){
                if (evt.getSource() instanceof ProteinUpdateProcessor) {
                    final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                    updateProcessor.fireOnProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), "The protein " + protein.getAc() + " has several uniprot identities " +xRefToString(uniprotIdentities), UpdateError.multi_uniprot_identities, protein));
                }
            }
            else {
                InteractorXref uniprotIdentity = uniprotIdentities.iterator().next();

                // the primaryId is never null in intact and is never empty
                return uniprotIdentity.getPrimaryId().trim();
            }
        }
        return null;
    }

    /**
     * Filter proteins having several uniprot identities, no-uniprot-update or no uniprot identity
     * and remove them from the list of proteins to update
     * @param proteins : the proteins to filter
     * @param caseEvent : the event containing all the intact proteins for a single uniprot entry
     */
    private void processListOfProteins(Collection<Protein> proteins, UpdateCaseEvent caseEvent){
        for (Iterator<? extends Protein> proteinIterator = proteins.iterator(); proteinIterator.hasNext();) {
            Protein protein = proteinIterator.next();

            // no-uniprot-update protein to remove
            if (!ProteinUtils.isFromUniprot(protein)) {
                ProteinEvent evt = new ProteinEvent(caseEvent.getSource(), caseEvent.getDataContext(), protein);
                if (evt.getSource() instanceof ProteinUpdateProcessor) {
                    final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                    updateProcessor.fireNonUniprotProteinFound(evt);
                }
                proteinIterator.remove();
            }
            // protein with multi uniprot identities to remove
            else if (!ProteinTools.hasUniqueDistinctUniprotIdentity(protein)){
                ProteinEvent evt = new ProteinEvent(caseEvent.getSource(), caseEvent.getDataContext(), protein);
                if (evt.getSource() instanceof ProteinUpdateProcessor) {
                    final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                    updateProcessor.fireOnProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), "The protein " + protein.getAc() + " has several uniprot identities " +xRefToString(ProteinTools.getAllUniprotIdentities(protein)), UpdateError.multi_uniprot_identities, protein));
                }
                proteinIterator.remove();
            }
        }
    }

    /**
     * Filter proteins isoforms or feature chains having several uniprot identities, no-uniprot-update or no uniprot identity
     * @param proteins : the isoforms/feature chains to look at
     * @param caseEvent
     */
    private void processListOfProteinsTranscript(Collection<ProteinTranscript> proteins, UpdateCaseEvent caseEvent){
        for (Iterator<ProteinTranscript> proteinIterator = proteins.iterator(); proteinIterator.hasNext();) {
            ProteinTranscript t = proteinIterator.next();
            Protein protein = t.getProtein();

            // no-uniprot-update protein to remove
            if (!ProteinUtils.isFromUniprot(protein)) {
                ProteinEvent evt = new ProteinEvent(caseEvent.getSource(), caseEvent.getDataContext(), protein);
                if (evt.getSource() instanceof ProteinUpdateProcessor) {
                    final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                    updateProcessor.fireNonUniprotProteinFound(evt);
                }
                proteinIterator.remove();
            }
            // protein with multi uniprot identities to remove
            else if (!ProteinTools.hasUniqueDistinctUniprotIdentity(protein)){
                ProteinEvent evt = new ProteinEvent(caseEvent.getSource(), caseEvent.getDataContext(), protein);
                if (evt.getSource() instanceof ProteinUpdateProcessor) {
                    final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                    updateProcessor.fireOnProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), "The protein " + protein.getAc() + " has several uniprot identities " +xRefToString(ProteinTools.getAllUniprotIdentities(protein)), UpdateError.multi_uniprot_identities, protein));
                }
                proteinIterator.remove();
            }
        }
    }

    /**
     * Filter proteins having several uniprot identities, no-uniprot-update or no uniprot identity
     * and remove them from the list of proteins to update
     * @param evt : the evet containing all master proteins, isoforms and feature chains to update
     */
    public void filterNonUniprotAndMultipleUniprot(UpdateCaseEvent evt) {
        // primary master proteins
        Collection<Protein> primaryProteins = evt.getPrimaryProteins();
        // secondary master proteins
        Collection<Protein> secondaryProteins = evt.getSecondaryProteins();
        // primary isoforms
        Collection<ProteinTranscript> primaryIsoforms = evt.getPrimaryIsoforms();
        // secondary isoforms
        Collection<ProteinTranscript> secondaryIsoforms = evt.getSecondaryIsoforms();
        // primary feature chains
        Collection<ProteinTranscript> primaryChains = evt.getPrimaryFeatureChains();

        // process all the proteins for this uniprot entry
        processListOfProteins(primaryProteins, evt);
        processListOfProteins(secondaryProteins, evt);
        processListOfProteinsTranscript(primaryIsoforms, evt);
        processListOfProteinsTranscript(secondaryIsoforms, evt);
        processListOfProteinsTranscript(primaryChains, evt);
    }

    private String xRefToString(Collection<InteractorXref> refs){
        StringBuilder sb = new StringBuilder();

        for (InteractorXref ref : refs) {

            String qual = (ref.getCvXrefQualifier() != null)? "("+ref.getCvXrefQualifier().getShortLabel()+")" : "";

            sb.append(ref.getCvDatabase().getShortLabel()+":"+ref.getPrimaryId()+qual);
            sb.append(", ");
        }

        return sb.toString();
    }
}
