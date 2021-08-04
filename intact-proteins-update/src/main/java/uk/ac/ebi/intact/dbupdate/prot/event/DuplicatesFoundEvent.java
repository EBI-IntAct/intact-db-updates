package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.dbupdate.prot.report.RangeUpdateReport;
import uk.ac.ebi.intact.dbupdate.prot.util.AdditionalInfoMap;
import uk.ac.ebi.intact.model.Annotation;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Fired when duplicates are found
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class DuplicatesFoundEvent extends MultiProteinEvent {

    /**
     * The following map stores the number of interactions for all the proteins (using ACs as key)
     */
    private AdditionalInfoMap<Integer> originalActiveInstancesCount;

    private String uniprotSequence;

    private String uniprotCrc64;

    private String primaryUniprotAc;

    private String uniprotTaxId;

    private Map<Protein, RangeUpdateReport> componentsWithFeatureConflicts = new HashMap<>();
    private boolean hasShiftedRanges;

    private Map<String, Set<String>> movedInteractions = new HashMap<>();
    private Map<String, Collection<String>> updatedTranscripts = new HashMap<>();
    private Map<String, Collection<InteractorXref>> movedXrefs = new HashMap<>();
    private Map<String, Collection<Annotation>> addedAnnotations = new HashMap<>();

    private Map<String, RangeUpdateReport> updatedRanges = new HashMap<>();

    /**
     * An event involving a list of proteins.
     */
    public DuplicatesFoundEvent(Object source, DataContext dataContext, Collection<Protein> proteins, String uniprotSequence, String uniprotCrc64, String primaryAc, String uniprotTaxId) {
        super(source, dataContext, proteins);

        this.originalActiveInstancesCount = new AdditionalInfoMap<>();

        for (Protein prot : proteins) {
            originalActiveInstancesCount.put(prot.getAc(), prot.getActiveInstances().size());
        }
        this.uniprotSequence = uniprotSequence;
        this.uniprotCrc64 = uniprotCrc64;
        this.primaryUniprotAc = primaryAc;
        this.uniprotTaxId = uniprotTaxId;
    }

    public AdditionalInfoMap<Integer> getOriginalActiveInstancesCount() {
        return originalActiveInstancesCount;
    }

    public String getUniprotTaxId() {
        return uniprotTaxId;
    }

    public String getUniprotSequence() {
        return uniprotSequence;
    }

    public String getUniprotCrc64() {
        return uniprotCrc64;
    }

    public String getPrimaryUniprotAc() {
        return primaryUniprotAc;
    }

    public Map<Protein, RangeUpdateReport> getComponentsWithFeatureConflicts() {
        return componentsWithFeatureConflicts;
    }

    public boolean hasShiftedRanges() {
        return hasShiftedRanges;
    }

    public void setHasShiftedRanges(boolean hasShiftedRanges) {
        this.hasShiftedRanges = hasShiftedRanges;
    }

    public Map<String, Set<String>> getMovedInteractions() {
        return movedInteractions;
    }

    public Map<String, Collection<String>> getUpdatedTranscripts() {
        return updatedTranscripts;
    }

    public Map<String, Collection<InteractorXref>> getMovedXrefs() {
        return movedXrefs;
    }

    public Map<String, Collection<Annotation>> getAddedAnnotations() {
        return addedAnnotations;
    }

    public Map<String, RangeUpdateReport> getUpdatedRanges() {
        return updatedRanges;
    }
}