package uk.ac.ebi.intact.dbupdate.prot.errors;

import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;

import java.util.Collection;

/**
 * Interface for protein update error factory
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>04/08/11</pre>
 */

public interface ProteinUpdateErrorFactory {

    ProteinUpdateError createDeadUniprotAcError(String proteinAc, String deadUniprotAc);
    ProteinUpdateError createImpossibleMergeError(String proteinAc, String originalProtein, String uniprotAc, String reason);
    ProteinUpdateError createImpossibleProteinRemappingError(String proteinAc, String errorMessage);
    ProteinUpdateError createImpossibleToDeleteError(String proteinAc, String errorMessage);
    ProteinUpdateError createImpossibleTranscriptUpdateError(String errorMessage, String uniprot);
    ProteinUpdateError createImpossibleUpdateMasterError(String errorMessage, String uniprot);
    ProteinUpdateError createInvalidCollectionOfParentsError(String proteinAc, UpdateError errorLabel, Collection<InteractorXref> isoformParents, Collection<InteractorXref> chainParents);
    ProteinUpdateError createInvalidParentXrefError(String proteinAc, String invalidParent, String reason);
    ProteinUpdateError createMatchSeveralUniprotEntriesError(String proteinAc, String uniprotAc, String taxId, UpdateError errorLabel, Collection<UniprotProtein> identitiesSameOrganism, Collection<UniprotProtein> identitiesDifferentOrganism);
    ProteinUpdateError createMultiUniprotIdentitiesError(String proteinAc, Collection<InteractorXref> identities);
    ProteinUpdateError createNonExistingMasterProteinError(String proteinAc, String deadMasterAc, String transcriptUniprotAc, String masterIntactAc);
    ProteinUpdateError createNonExistingProteinTranscriptError(String proteinAc, String deadTranscriptAc, String masterUniprotAc, String masterIntactAc);
    ProteinUpdateError createOrganismConflictError(String proteinAc, String wrongTaxId, String uniprotTaxId, String uniprotAc);
    ProteinUpdateError createUniprotSequenceNullError(String proteinAc, String uniprotAc, String intactSequence);
    ProteinUpdateError createImpossibleParentTranscriptToReviewError(String proteinAc, String reason);
    ProteinUpdateError createFatalUpdateError(String proteinAc, String uniprot, Exception e);
}