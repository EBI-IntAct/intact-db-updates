package uk.ac.ebi.intact.dbupdate.prot;

import uk.ac.ebi.intact.model.Component;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>10-Nov-2010</pre>
 */

public class DuplicateReport {

    private Protein originalProtein;
    private UniprotProteinTranscript transcript;
    private Map<Protein, Collection<Component>> componentsWithFeatureConflicts;

    public DuplicateReport(){
        originalProtein = null;
        this.transcript = null;
        componentsWithFeatureConflicts = new HashMap<Protein, Collection<Component>>();
    }

    public Protein getOriginalProtein() {
        return originalProtein;
    }

    public void setOriginalProtein(Protein originalProtein) {
        this.originalProtein = originalProtein;
    }

    public Map<Protein, Collection<Component>> getComponentsWithFeatureConflicts() {
        return componentsWithFeatureConflicts;
    }

    public void setComponentsWithFeatureConflicts(Map<Protein, Collection<Component>> componentsWithFeatureConflicts) {
        this.componentsWithFeatureConflicts = componentsWithFeatureConflicts;
    }

    public UniprotProteinTranscript getTranscript() {
        return transcript;
    }

    public void setTranscript(UniprotProteinTranscript transcript) {
        this.transcript = transcript;
    }
}
