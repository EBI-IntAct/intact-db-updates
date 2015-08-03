package uk.ac.ebi.intact.dbupdate.prot;

import uk.ac.ebi.intact.dbupdate.prot.rangefix.InvalidRange;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.UpdatedRange;
import uk.ac.ebi.intact.model.Component;
import uk.ac.ebi.intact.util.protein.utils.AnnotationUpdateReport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>29-Nov-2010</pre>
 */

public class RangeUpdateReport {

    private Map<Component, Collection<InvalidRange>> invalidComponents = new HashMap<Component, Collection<InvalidRange>>();
    private Map<String, AnnotationUpdateReport> updatedFeatureAnnotations = new HashMap<String, AnnotationUpdateReport>();
    private Collection<UpdatedRange> shiftedRanges = new ArrayList<UpdatedRange>();

    public Map<Component, Collection<InvalidRange>> getInvalidComponents() {
        return invalidComponents;
    }

    public Map<String, AnnotationUpdateReport> getUpdatedFeatureAnnotations() {
        return updatedFeatureAnnotations;
    }

    public Collection<UpdatedRange> getShiftedRanges() {
        return shiftedRanges;
    }
}
