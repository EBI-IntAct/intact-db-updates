package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.model.Protein;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02-Dec-2010</pre>
 */

public class ProteinTranscriptWithSameSequenceEvent extends ProteinEvent{

    private String uniprotTranscriptAc;
    private String currentUniprotAc;

    public ProteinTranscriptWithSameSequenceEvent(Object source, DataContext dataContext, Protein protein, String currentAc, String transcriptAc) {
        super(source, dataContext, protein);
        this.uniprotTranscriptAc = transcriptAc;
        this.currentUniprotAc = currentAc;
    }

    public String getUniprotTranscriptAc() {
        return uniprotTranscriptAc;
    }

    public String getCurrentUniprotAc() {
        return currentUniprotAc;
    }
}
