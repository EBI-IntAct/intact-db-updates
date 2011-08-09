package uk.ac.ebi.intact.update.model.protein.events;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * This events if for proteins which have a sequence identical to one of the transcripts before being updated
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>01/08/11</pre>
 */
@Entity
@DiscriminatorValue("transcript_with_same_sequence")
public class SequenceIdenticalToTranscriptEvent extends PersistentProteinEvent {

    /**
     * The uniprot ac of the transcript which has the same sequence as the protein in intact
     */
    private String matchingUniprotTranscript;

    public SequenceIdenticalToTranscriptEvent(){
        super();
        this.matchingUniprotTranscript = null;

    }

    public SequenceIdenticalToTranscriptEvent(ProteinUpdateProcess updateProcess, Protein protein, String originalUniprotAc, String matchingTranscriptAc){
        super(updateProcess, protein, originalUniprotAc);
        this.matchingUniprotTranscript = matchingTranscriptAc;
    }

    @Column(name = "matching_transcript")
    public String getMatchingUniprotTranscript() {
        return matchingUniprotTranscript;
    }

    public void setMatchingUniprotTranscript(String matchingUniprotTranscript) {
        this.matchingUniprotTranscript = matchingUniprotTranscript;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final SequenceIdenticalToTranscriptEvent event = ( SequenceIdenticalToTranscriptEvent ) o;

        if ( matchingUniprotTranscript != null ) {
            if (!matchingUniprotTranscript.equals( event.getMatchingUniprotTranscript())){
                return false;
            }
        }
        else if (event.getMatchingUniprotTranscript()!= null){
            return false;
        }

        return true;
    }

    /**
     * This class overwrites equals. To ensure proper functioning of HashTable,
     * hashCode must be overwritten, too.
     *
     * @return hash code of the object.
     */
    @Override
    public int hashCode() {

        int code = 29;

        code = 29 * code + super.hashCode();

        if ( matchingUniprotTranscript != null ) {
            code = 29 * code + matchingUniprotTranscript.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final SequenceIdenticalToTranscriptEvent event = ( SequenceIdenticalToTranscriptEvent ) o;

        if ( matchingUniprotTranscript != null ) {
            if (!matchingUniprotTranscript.equals( event.getMatchingUniprotTranscript())){
                return false;
            }
        }
        else if (event.getMatchingUniprotTranscript()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Protein having identical sequence to one transcript event : [ matching transcript ac = " + matchingUniprotTranscript != null ? matchingUniprotTranscript : "none");

        return buffer.toString();
    }
}
