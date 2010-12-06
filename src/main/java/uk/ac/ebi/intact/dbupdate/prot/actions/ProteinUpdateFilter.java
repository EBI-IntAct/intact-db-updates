package uk.ac.ebi.intact.dbupdate.prot.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.dbupdate.prot.*;
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
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03-Nov-2010</pre>
 */

public class ProteinUpdateFilter {

    private static final Log log = LogFactory.getLog( ProteinUpdateFilter.class );

    public String filterOnUniprotIdentity(ProteinEvent evt) throws ProcessorException {

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

    private void processListOfProteins(Collection<Protein> proteins, UpdateCaseEvent caseEvent){
        for (Iterator<? extends Protein> proteinIterator = proteins.iterator(); proteinIterator.hasNext();) {
            Protein protein = proteinIterator.next();

            if (!ProteinUtils.isFromUniprot(protein)) {
                ProteinEvent evt = new ProteinEvent(caseEvent.getSource(), caseEvent.getDataContext(), protein);
                if (evt.getSource() instanceof ProteinUpdateProcessor) {
                    final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                    updateProcessor.fireNonUniprotProteinFound(evt);
                }
                proteinIterator.remove();
            }
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

    private void processListOfProteinsTranscript(Collection<ProteinTranscript> proteins, UpdateCaseEvent caseEvent){
        for (Iterator<ProteinTranscript> proteinIterator = proteins.iterator(); proteinIterator.hasNext();) {
            ProteinTranscript t = proteinIterator.next();
            Protein protein = t.getProtein();

            if (!ProteinUtils.isFromUniprot(protein)) {
                ProteinEvent evt = new ProteinEvent(caseEvent.getSource(), caseEvent.getDataContext(), protein);
                if (evt.getSource() instanceof ProteinUpdateProcessor) {
                    final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                    updateProcessor.fireNonUniprotProteinFound(evt);
                }
                proteinIterator.remove();
            }
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

    public void filterNonUniprotAndMultipleUniprot(UpdateCaseEvent evt) {
        Collection<Protein> primaryProteins = evt.getPrimaryProteins();
        Collection<Protein> secondaryProteins = evt.getSecondaryProteins();
        Collection<ProteinTranscript> primaryIsoforms = evt.getPrimaryIsoforms();
        Collection<ProteinTranscript> secondaryIsoforms = evt.getSecondaryIsoforms();
        Collection<ProteinTranscript> primaryChains = evt.getPrimaryFeatureChains();

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
