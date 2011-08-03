package uk.ac.ebi.intact.dbupdate.prot.errors;

import uk.ac.ebi.intact.dbupdate.prot.UpdateError;

/**
 * This error is for protein transcripts impossible to update
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */

public class ImpossibleTranscriptUpdate extends DefaultProteinUpdateError {
    public ImpossibleTranscriptUpdate(String errorMessage, String proteinAc) {
        super(UpdateError.impossible_transcript_update, errorMessage, proteinAc);
    }
}
