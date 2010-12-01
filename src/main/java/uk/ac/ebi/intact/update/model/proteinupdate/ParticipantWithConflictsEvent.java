package uk.ac.ebi.intact.update.model.proteinupdate;

import uk.ac.ebi.intact.model.Component;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>25-Nov-2010</pre>
 */
@Entity
@DiscriminatorValue("ParticipantWithConflictsEvent")
public class ParticipantWithConflictsEvent extends ProteinEvent{

    private Collection<String> componentsWithFeatureConflicts;
    private String remapped_protein_ac;

    public ParticipantWithConflictsEvent(){
        super();
        this.componentsWithFeatureConflicts = new ArrayList<String>();
        this.remapped_protein_ac = null;

    }

    public ParticipantWithConflictsEvent(Date created, String remappedProteinAc, Collection<String> componentsWithRangeConflicts, int index){
        super(EventName.participant_with_feature_conflicts, created, index);
        this.componentsWithFeatureConflicts = new ArrayList<String>();
        this.componentsWithFeatureConflicts.addAll(componentsWithRangeConflicts);
        this.remapped_protein_ac = remappedProteinAc;
    }

    public ParticipantWithConflictsEvent(Date created, String remappedProteinAc, int index){
        super(EventName.participant_with_feature_conflicts, created, index);
        this.componentsWithFeatureConflicts = new ArrayList<String>();
        this.remapped_protein_ac = remappedProteinAc;
    }

    public Collection<String> getComponentsWithFeatureConflicts() {
        return componentsWithFeatureConflicts;
    }

    public void setComponentsWithFeatureConflicts(Collection<String> componentsWithFeatureConflicts) {
        this.componentsWithFeatureConflicts = componentsWithFeatureConflicts;
    }

    public String getRemapped_protein_ac() {
        return remapped_protein_ac;
    }

    public void setRemapped_protein_ac(String remapped_protein_ac) {
        this.remapped_protein_ac = remapped_protein_ac;
    }
}
