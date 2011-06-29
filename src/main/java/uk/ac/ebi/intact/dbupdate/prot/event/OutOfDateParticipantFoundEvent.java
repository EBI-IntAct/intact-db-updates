package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.model.Component;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;

import java.util.ArrayList;
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
    private Collection<Component> componentsToFix = new ArrayList<Component>();
    private Protein proteinWithConflicts;
    String validParentAc;

    public OutOfDateParticipantFoundEvent(Object source, DataContext dataContext, Collection<Component> components, Protein protein, UniprotProtein uniprotProtein, Collection<ProteinTranscript> primaryIsoforms, Collection<ProteinTranscript> secondaryIsoforms, Collection<ProteinTranscript> primaryFeatureChains, String validParentAc) {
        super(source, dataContext, uniprotProtein, Collections.EMPTY_LIST, Collections.EMPTY_LIST, primaryIsoforms, secondaryIsoforms, primaryFeatureChains, null);
        this.componentsToFix = components;
        this.proteinWithConflicts = protein;
        this.validParentAc = validParentAc;
    }

    public OutOfDateParticipantFoundEvent(Object source, DataContext dataContext, Protein protein, UniprotProtein uniprotProtein, Collection<ProteinTranscript> primaryIsoforms, Collection<ProteinTranscript> secondaryIsoforms, Collection<ProteinTranscript> primaryFeatureChains, String validParentAc) {
        super(source, dataContext, uniprotProtein, Collections.EMPTY_LIST, Collections.EMPTY_LIST, primaryIsoforms, secondaryIsoforms, primaryFeatureChains, null);
        this.proteinWithConflicts = protein;
        this.validParentAc = validParentAc;
    }

    public Collection<Component> getComponentsToFix() {
        return componentsToFix;
    }

    public void addComponentToFix(Component component){
        this.componentsToFix.add(component);
    }

    public Protein getProteinWithConflicts() {
        return proteinWithConflicts;
    }

    public String getValidParentAc() {
        return validParentAc;
    }
}
