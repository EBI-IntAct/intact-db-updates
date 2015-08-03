package uk.ac.ebi.intact.dbupdate.prot;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;
import uk.ac.ebi.intact.uniprot.model.UniprotSpliceVariant;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>10-Nov-2010</pre>
 */

public class ProteinTranscript {

    private Protein protein;
    private UniprotProteinTranscript uniprotVariant;

    public ProteinTranscript(Protein prot, UniprotProteinTranscript var){
        this.protein = prot;
        this.uniprotVariant = var;
    }

    public Protein getProtein() {
        return protein;
    }

    public void setProtein(Protein protein) {
        this.protein = protein;
    }

    public UniprotProteinTranscript getUniprotVariant() {
        return uniprotVariant;
    }

    public void setUniprotVariant(UniprotProteinTranscript uniprotVariant) {
        this.uniprotVariant = uniprotVariant;
    }
}
