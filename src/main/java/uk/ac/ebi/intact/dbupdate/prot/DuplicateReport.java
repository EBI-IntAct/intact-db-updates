package uk.ac.ebi.intact.dbupdate.prot;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A report containing the results of the merge of several duplicated proteins
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>10-Nov-2010</pre>
 */

public class DuplicateReport {

    private Protein originalProtein;
    private UniprotProteinTranscript transcript;
    private Map<Protein, RangeUpdateReport> componentsWithFeatureConflicts;
    private String uniprotSequence;
    private String crc64;
    private boolean hasShiftedRanges;

    private Map<String, Collection<String>> movedInteractions = new HashMap<String, Collection<String>>();

    public DuplicateReport(){
        originalProtein = null;
        this.transcript = null;
        componentsWithFeatureConflicts = new HashMap<Protein, RangeUpdateReport>();
        uniprotSequence = null;
        crc64 = null;
        hasShiftedRanges = false;
    }

    public Protein getOriginalProtein() {
        return originalProtein;
    }

    public void setOriginalProtein(Protein originalProtein) {
        this.originalProtein = originalProtein;
    }

    public Map<Protein, RangeUpdateReport> getComponentsWithFeatureConflicts() {
        return componentsWithFeatureConflicts;
    }

    public void setComponentsWithFeatureConflicts(Map<Protein, RangeUpdateReport> componentsWithFeatureConflicts) {
        this.componentsWithFeatureConflicts = componentsWithFeatureConflicts;
    }

    public UniprotProteinTranscript getTranscript() {
        return transcript;
    }

    public void setTranscript(UniprotProteinTranscript transcript) {
        this.transcript = transcript;
    }

    public String getUniprotSequence() {
        return uniprotSequence;
    }

    public void setUniprotSequence(String uniprotSequence) {
        this.uniprotSequence = uniprotSequence;
    }

    public String getCrc64() {
        return crc64;
    }

    public void setCrc64(String crc64) {
        this.crc64 = crc64;
    }

    public boolean hasShiftedRanges() {
        return hasShiftedRanges;
    }

    public void setHasShiftedRanges(boolean hasShiftedRanges) {
        this.hasShiftedRanges = hasShiftedRanges;
    }

    public Map<String, Collection<String>> getMovedInteractions() {
        return movedInteractions;
    }
}
