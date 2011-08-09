package uk.ac.ebi.intact.update.model.protein.errors;

import uk.ac.ebi.intact.dbupdate.prot.errors.UpdateError;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Errors for isoforms and feature chains impossible to update
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>08/08/11</pre>
 */
@Entity
@DiscriminatorValue("impossible_update_transcript")
public class ImpossibleTranscriptUpdate extends ImpossibleUpdateMaster {

    public ImpossibleTranscriptUpdate(){
        super(null, UpdateError.impossible_transcript_update, null, null);
    }
    public ImpossibleTranscriptUpdate(ProteinUpdateProcess process, String errorMessage, String uniprot) {
        super(process, UpdateError.impossible_transcript_update, errorMessage, uniprot);
    }
}
