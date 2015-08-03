package uk.ac.ebi.intact.update.model.protein.errors;

import org.apache.commons.lang.exception.ExceptionUtils;
import uk.ac.ebi.intact.dbupdate.prot.errors.ProteinUpdateError;
import uk.ac.ebi.intact.dbupdate.prot.errors.ProteinUpdateErrorFactory;
import uk.ac.ebi.intact.dbupdate.prot.errors.UpdateError;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;

import java.util.Collection;

/**
 * This class is extending UpdateErrorfactory but creates persistentUpdateErrors
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>08/08/11</pre>
 */

public class PersistentUpdateErrorFactory implements ProteinUpdateErrorFactory{

    @Override
    public ProteinUpdateError createDeadUniprotAcError(String proteinAc, String deadUniprotAc) {
        return new DeadUniprotAc(null, proteinAc, deadUniprotAc);
    }

    @Override
    public ProteinUpdateError createImpossibleMergeError(String proteinAc, String originalProtein, String uniprotAc, String reason) {
        return new ImpossibleMerge(null, proteinAc, originalProtein, uniprotAc, reason);
    }

    @Override
    public ProteinUpdateError createImpossibleProteinRemappingError(String proteinAc, String errorMessage) {
        return new ImpossibleProteinRemapping(null, errorMessage, proteinAc);
    }

    @Override
    public ProteinUpdateError createImpossibleToDeleteError(String proteinAc, String errorMessage) {
        return new ImpossibleToDelete(null, errorMessage, proteinAc);
    }

    @Override
    public ProteinUpdateError createImpossibleTranscriptUpdateError(String errorMessage, String uniprot) {
        return new ImpossibleTranscriptUpdate(null, errorMessage, uniprot);
    }

    @Override
    public ProteinUpdateError createImpossibleUpdateMasterError(String errorMessage, String uniprot) {
        return new ImpossibleUpdateMaster(null, errorMessage, uniprot);
    }

    @Override
    public ProteinUpdateError createInvalidCollectionOfParentsError(String proteinAc, UpdateError errorLabel, Collection<InteractorXref> isoformParents, Collection<InteractorXref> chainParents) {
        InvalidCollectionOfParents error = new InvalidCollectionOfParents(null, proteinAc, errorLabel);

        for (InteractorXref ref : isoformParents){
            error.getIsoformParents().add(ref.getPrimaryId());
        }
        for (InteractorXref ref : chainParents){
            error.getChainParents().add(ref.getPrimaryId());
        }

        return error;
    }

    @Override
    public ProteinUpdateError createInvalidParentXrefError(String proteinAc, String invalidParent, String reason) {
        return new InvalidParentXref(null, proteinAc, invalidParent, reason);
    }

    @Override
    public ProteinUpdateError createMatchSeveralUniprotEntriesError(String proteinAc, String uniprotAc, String taxId, UpdateError errorLabel, Collection<UniprotProtein> identitiesSameOrganism, Collection<UniprotProtein> identitiesDifferentOrganism) {
        MatchSeveralUniprotEntries error = new MatchSeveralUniprotEntries(null, proteinAc, uniprotAc, taxId, errorLabel);

        for (UniprotProtein p : identitiesSameOrganism){
            error.getUniprotIdentities().add(p.getPrimaryAc());
        }
        for (UniprotProtein p : identitiesDifferentOrganism){
            error.getUniprotFromDifferentOrganisms().add(p.getPrimaryAc());
        }
        return error;
    }

    @Override
    public ProteinUpdateError createMultiUniprotIdentitiesError(String proteinAc, Collection<InteractorXref> identities) {
        MultiUniprotIdentities error = new MultiUniprotIdentities(null, proteinAc);

        for (InteractorXref ref : identities){
            error.getUniprotIdentities().add(ref.getPrimaryId());
        }

        return error;
    }

    @Override
    public ProteinUpdateError createNonExistingMasterProteinError(String proteinAc, String deadMasterAc, String transcriptUniprotAc, String transcriptIntactAc) {
        return new NonExistingMasterProtein(null, proteinAc, deadMasterAc, transcriptUniprotAc, transcriptIntactAc);
    }

    @Override
    public ProteinUpdateError createNonExistingProteinTranscriptError(String proteinAc, String deadTranscriptAc, String masterUniprotAc, String masterIntactAc) {
        return new NonExistingProteinTranscript(null, proteinAc, deadTranscriptAc, masterUniprotAc, masterIntactAc);
    }

    @Override
    public ProteinUpdateError createOrganismConflictError(String proteinAc, String wrongTaxId, String uniprotTaxId, String uniprotAc) {
        return new OrganismConflict(null, proteinAc, wrongTaxId, uniprotTaxId, uniprotAc);
    }

    @Override
    public ProteinUpdateError createUniprotSequenceNullError(String proteinAc, String uniprotAc, String intactSequence) {
        return new UniprotSequenceNull(null, proteinAc, uniprotAc, intactSequence);
    }

    @Override
    public ProteinUpdateError createImpossibleParentTranscriptToReviewError(String proteinAc, String reason) {
        return new ImpossibleParentToReview(null, reason, proteinAc);
    }

    @Override
    public ProteinUpdateError createFatalUpdateError(String proteinAc, String uniprot, Exception e) {
        String errorMessage = ExceptionUtils.getFullStackTrace(e);
        return new FatalUpdateError(null, proteinAc, uniprot, errorMessage);
    }
}
