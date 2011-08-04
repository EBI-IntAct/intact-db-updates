package uk.ac.ebi.intact.dbupdate.prot.errors;

/**
 * This class is for protein transcripts without valid parent but impossible to review
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>04/08/11</pre>
 */

public class ImpossibleTranscriptParentReview extends DefaultIntactProteinUpdateError{
    public ImpossibleTranscriptParentReview(String errorMessage, String proteinAc) {
        super(UpdateError.impossible_transcript_parent_review, errorMessage, proteinAc);
    }
}
