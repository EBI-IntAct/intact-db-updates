package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.RangeUpdateReport;
import uk.ac.ebi.intact.model.Component;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;

import java.util.Collection;
import java.util.Collections;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>29-Oct-2010</pre>
 */

public class OutOfDateParticipantFoundEvent extends UpdateCaseEvent{
    private RangeUpdateReport invalidRangeReport;
    private Protein proteinWithConflicts;
    String validParentAc;
    String remappedProteinAc;

    public OutOfDateParticipantFoundEvent(Object source, DataContext dataContext, Protein protein, UniprotProtein uniprotProtein, RangeUpdateReport invalidReport, Collection<ProteinTranscript> primaryIsoforms, Collection<ProteinTranscript> secondaryIsoforms, Collection<ProteinTranscript> primaryFeatureChains, String validParentAc) {
        super(source, dataContext, uniprotProtein, Collections.EMPTY_LIST, Collections.EMPTY_LIST, primaryIsoforms, secondaryIsoforms, primaryFeatureChains, null);
        this.invalidRangeReport = invalidReport;
        this.proteinWithConflicts = protein;
        this.validParentAc = validParentAc;
        this.remappedProteinAc = null;
    }

    public Collection<Component> getComponentsToFix() {
        if (this.invalidRangeReport != null){
           return this.invalidRangeReport.getInvalidComponents().keySet();
        }
        return Collections.EMPTY_LIST;
    }

    public Protein getProteinWithConflicts() {
        return proteinWithConflicts;
    }

    public String getValidParentAc() {
        return validParentAc;
    }

    public RangeUpdateReport getInvalidRangeReport() {
        return invalidRangeReport;
    }

    public String getRemappedProteinAc() {
        return remappedProteinAc;
    }

    public void setRemappedProteinAc(String remappedProteinAc) {
        this.remappedProteinAc = remappedProteinAc;
    }
}
