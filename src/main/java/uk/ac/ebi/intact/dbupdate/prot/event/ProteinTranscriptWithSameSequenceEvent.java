package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02-Dec-2010</pre>
 */

public class ProteinTranscriptWithSameSequenceEvent extends ProteinEvent{

    private String uniprotTranscriptAc;

    public ProteinTranscriptWithSameSequenceEvent(Object source, DataContext dataContext, Protein protein, UniprotProtein uniprotProtein, String transcriptAc) {
        super(source, dataContext, protein, uniprotProtein);
        this.uniprotTranscriptAc = transcriptAc;
    }

    public String getUniprotTranscriptAc() {
        return uniprotTranscriptAc;
    }
}
