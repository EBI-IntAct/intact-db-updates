package uk.ac.ebi.intact.dbupdate.prot.errors;

import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;

import java.util.Collection;

/**
 * Default implementation of the update error factory
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>04/08/11</pre>
 */

public class DefaultProteinUpdateErrorFactory implements ProteinUpdateErrorFactory {
    @Override
    public ProteinUpdateError createDeadUniprotAcError(String proteinAc, String deadUniprotAc) {
        return new DeadUniprotAc(proteinAc, deadUniprotAc);
    }

    @Override
    public ProteinUpdateError createImpossibleMergeError(String proteinAc, String originalProtein, String uniprotAc, String reason) {
        return new ImpossibleMerge(proteinAc, originalProtein, uniprotAc, reason);
    }

    @Override
    public ProteinUpdateError createImpossibleProteinRemappingError(String proteinAc, String errorMessage) {
        return new ImpossibleProteinRemapping(proteinAc, errorMessage);
    }

    @Override
    public ProteinUpdateError createImpossibleToDeleteError(String proteinlabel, String errorMessage) {
        return new ImpossibleToDelete(errorMessage, proteinlabel);
    }

    @Override
    public ProteinUpdateError createImpossibleTranscriptUpdateError(String errorMessage, String uniprot) {
        return new ImpossibleTranscriptUpdate(errorMessage, uniprot);
    }

    @Override
    public ProteinUpdateError createImpossibleUpdateMasterError(String errorMessage, String uniprot) {
        return new ImpossibleUpdateMaster(errorMessage, uniprot);
    }

    @Override
    public ProteinUpdateError createInvalidCollectionOfParentsError(String proteinAc, UpdateError errorLabel, Collection<InteractorXref> isoformParents, Collection<InteractorXref> chainParents) {
        InvalidCollectionOfParents invalid = new InvalidCollectionOfParents(proteinAc, errorLabel);

        for (InteractorXref ref : isoformParents){
            invalid.getIsoformParents().add(ref.getPrimaryId());
        }
        for (InteractorXref ref : chainParents){
            invalid.getChainParents().add(ref.getPrimaryId());
        }
        return invalid;
    }

    @Override
    public ProteinUpdateError createInvalidParentXrefError(String proteinAc, String invalidParent, String reason) {
        return new InvalidParentXref(proteinAc, invalidParent, reason);
    }

    @Override
    public ProteinUpdateError createMatchSeveralUniprotEntriesError(String proteinAc, String uniprotAc, String taxId, UpdateError errorLabel, Collection<UniprotProtein> identitiesSameOrganism, Collection<UniprotProtein> identitiesDifferentOrganism) {
        MatchSeveralUniprotEntries matchSeveralUniprot = new MatchSeveralUniprotEntries(proteinAc, uniprotAc, taxId, errorLabel);
        for (UniprotProtein p : identitiesSameOrganism){
            matchSeveralUniprot.getUniprotIdentities().add(p.getPrimaryAc());
        }
        for (UniprotProtein p : identitiesDifferentOrganism){
            matchSeveralUniprot.getUniprotFromDifferentOrganisms().add(p.getPrimaryAc());
        }
        return matchSeveralUniprot;
    }

    @Override
    public ProteinUpdateError createMultiUniprotIdentitiesError(String proteinAc, Collection<InteractorXref> identities) {
        MultiUniprotIdentities matchSeveralUniprot = new MultiUniprotIdentities(proteinAc);
        for (InteractorXref ref : identities){
            matchSeveralUniprot.getUniprotIdentities().add(ref.getPrimaryId());
        }
        return matchSeveralUniprot;
    }

    @Override
    public ProteinUpdateError createNonExistingMasterProteinError(String proteinAc, String deadMasterAc, String transcriptUniprotAc, String transcriptIntactAc) {
        return new NonExistingMasterProtein(proteinAc, deadMasterAc, transcriptUniprotAc, transcriptIntactAc);
    }

    @Override
    public ProteinUpdateError createNonExistingProteinTranscriptError(String proteinAc, String deadTranscriptAc, String masterUniprotAc, String masterIntactAc) {
        return new NonExistingProteinTranscript(proteinAc, deadTranscriptAc, masterUniprotAc, masterIntactAc);
    }

    @Override
    public ProteinUpdateError createOrganismConflictError(String proteinAc, String wrongTaxId, String uniprotTaxId, String uniprotAc) {
        return new OrganismConflict(proteinAc, wrongTaxId, uniprotTaxId, uniprotAc);
    }

    @Override
    public ProteinUpdateError createUniprotSequenceNullError(String proteinAc, String uniprotAc, String intactSequence) {
        return new UniprotSequenceNull(proteinAc, uniprotAc, intactSequence);
    }

    @Override
    public ProteinUpdateError createImpossibleParentTranscriptToReviewError(String proteinAc, String error) {
        return new ImpossibleTranscriptParentReview(error, proteinAc);
    }

    @Override
    public ProteinUpdateError createFatalUpdateError(String proteinAc, String uniprot, Exception e) {
        return new FatalErrorDuringUpdate(proteinAc, uniprot, e);
    }
}
