package uk.ac.ebi.intact.dbupdate.prot;

import uk.ac.ebi.intact.model.Annotation;
import uk.ac.ebi.intact.model.InteractorXref;
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

    private UniprotProteinTranscript transcript;
    private Map<Protein, RangeUpdateReport> componentsWithFeatureConflicts;
    private String uniprotSequence;
    private String crc64;
    private boolean hasShiftedRanges;

    private Map<String, Collection<String>> movedInteractions = new HashMap<String, Collection<String>>();
    private Map<String, Collection<String>> updatedTranscripts = new HashMap<String, Collection<String>>();
    private Map<String, Collection<InteractorXref>> addedXRefs = new HashMap<String, Collection<InteractorXref>>();
    private Map<String, Collection<Annotation>> addedAnnotations = new HashMap<String, Collection<Annotation>>();

    public DuplicateReport(){
        this.transcript = null;
        componentsWithFeatureConflicts = new HashMap<Protein, RangeUpdateReport>();
        uniprotSequence = null;
        crc64 = null;
        hasShiftedRanges = false;
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

    public Map<String, Collection<InteractorXref>> getAddedXRefs() {
        return addedXRefs;
    }

    public Map<String, Collection<Annotation>> getAddedAnnotations() {
        return addedAnnotations;
    }

    public Map<String, Collection<String>> getUpdatedTranscripts() {
        return updatedTranscripts;
    }
}
