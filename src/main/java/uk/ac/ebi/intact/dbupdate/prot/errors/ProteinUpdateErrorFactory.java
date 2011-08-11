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

    public ProteinUpdateError createDeadUniprotAcError(String proteinAc, String deadUniprotAc);
    public ProteinUpdateError createImpossibleMergeError(String proteinAc, String originalProtein, String uniprotAc, String reason);
    public ProteinUpdateError createImpossibleProteinRemappingError(String proteinAc, String errorMessage);
    public ProteinUpdateError createImpossibleToDeleteError(String proteinAc, String errorMessage);
    public ProteinUpdateError createImpossibleTranscriptUpdateError(String errorMessage, String uniprot);
    public ProteinUpdateError createImpossibleUpdateMasterError(String errorMessage, String uniprot);
    public ProteinUpdateError createInvalidCollectionOfParentsError(String proteinAc, UpdateError errorLabel, Collection<InteractorXref> isoformParents, Collection<InteractorXref> chainParents);
    public ProteinUpdateError createInvalidParentXrefError(String proteinAc, String invalidParent, String reason);
    public ProteinUpdateError createMatchSeveralUniprotEntriesError(String proteinAc, String uniprotAc, String taxId, UpdateError errorLabel, Collection<UniprotProtein> identitiesSameOrganism, Collection<UniprotProtein> identitiesDifferentOrganism);
    public ProteinUpdateError createMultiUniprotIdentitiesError(String proteinAc, Collection<InteractorXref> identities);
    public ProteinUpdateError createNonExistingMasterProteinError(String proteinAc, String deadMasterAc, String transcriptUniprotAc, String masterIntactAc);
    public ProteinUpdateError createNonExistingProteinTranscriptError(String proteinAc, String deadTranscriptAc, String masterUniprotAc, String masterIntactAc);
    public ProteinUpdateError createOrganismConflictError(String proteinAc, String wrongTaxId, String uniprotTaxId, String uniprotAc);
    public ProteinUpdateError createUniprotSequenceNullError(String proteinAc, String uniprotAc, String intactSequence);
    public ProteinUpdateError createImpossibleParentTranscriptToReviewError(String proteinAc, String reason);
    public ProteinUpdateError createFatalUpdateError(String proteinAc, String uniprot, Exception e);
}
