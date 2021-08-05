package uk.ac.ebi.intact.dbupdate.prot.model;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>10-Nov-2010</pre>
 */

public class ProteinTranscript {

    private Protein protein;
    private UniprotProteinTranscript uniprotProteinTranscript;

    public ProteinTranscript(Protein protein, UniprotProteinTranscript uniprotProteinTranscript){
        this.protein = protein;
        this.uniprotProteinTranscript = uniprotProteinTranscript;
    }

    public Protein getProtein() {
        return protein;
    }

    public void setProtein(Protein protein) {
        this.protein = protein;
    }

    public UniprotProteinTranscript getUniprotProteinTranscript() {
        return uniprotProteinTranscript;
    }

    public void setUniprotProteinTranscript(UniprotProteinTranscript uniprotProteinTranscript) {
        this.uniprotProteinTranscript = uniprotProteinTranscript;
    }
}