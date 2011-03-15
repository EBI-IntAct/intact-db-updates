package uk.ac.ebi.intact.update.model.protein.update.events;

import uk.ac.ebi.intact.model.InteractorAlias;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.HibernatePersistentImpl;
import uk.ac.ebi.intact.update.model.protein.update.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * The super class which represents an event of a protein update
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="objClass", discriminatorType=DiscriminatorType.STRING)
@DiscriminatorValue("ProteinEvent")
@Table(name = "ia_update_event")
public class ProteinEvent extends HibernatePersistentImpl {

    EventName name;
    int index;

    String protein;
    UpdateProcess updateProcess;

    private Collection<UpdatedCrossReference> updatedReferences;
    private Collection<UpdatedAnnotation> updatedAnnotations;
    private Collection<UpdatedAlias> updatedAliases;

    public ProteinEvent(){
        super();
        this.name = EventName.uniprot_update;
        this.index = 0;

        this.protein = null;

        updatedReferences = new ArrayList<UpdatedCrossReference>();
        updatedAnnotations = new ArrayList<UpdatedAnnotation>();
        updatedAliases = new ArrayList<UpdatedAlias>();
    }

    public ProteinEvent(UpdateProcess process, EventName name, Protein protein, int index){
        super();
        this.name = name;
        this.index = index;
        this.protein = protein != null ? protein.getAc() : null;
        this.updateProcess = process;
        updatedReferences = new ArrayList<UpdatedCrossReference>();
        updatedAnnotations = new ArrayList<UpdatedAnnotation>();
        updatedAliases = new ArrayList<UpdatedAlias>();
    }

    @ManyToOne
    @JoinColumn(name="parent_id", nullable=false)
    public UpdateProcess getParent() {
        return this.updateProcess;
    }

    public void setParent(UpdateProcess updateProcess) {
        this.updateProcess = updateProcess;
    }

    @Column(name="protein_ac", nullable = false)
    public String getProtein() {
        return protein;
    }

    public void setProtein(String protein) {
        this.protein = protein;
    }

    @ManyToMany
    @JoinTable(
            name = "ia_event2updated_xref",
            joinColumns = {@JoinColumn( name = "protein_event_id" )},
            inverseJoinColumns = {@JoinColumn( name = "updated_xref_id" )}
    )
    public Collection<UpdatedCrossReference> getUpdatedReferences() {
        return updatedReferences;
    }

    public void setUpdatedReferences(Collection<UpdatedCrossReference> updatedReferences) {
        if (updatedReferences != null){
            this.updatedReferences = updatedReferences;
        }
    }

    public void addUpdatedReferencesFromInteractor(Collection<InteractorXref> updatedRef, UpdateStatus status){
        for (InteractorXref ref : updatedRef){
            String refAc = ref.getAc();

            UpdatedCrossReference reference = new UpdatedCrossReference(ref, status);
            this.updatedReferences.add(reference);
        }
    }

    @ManyToMany
    @JoinTable(
            name = "ia_event2updated_annotations",
            joinColumns = {@JoinColumn( name = "protein_event_id" )},
            inverseJoinColumns = {@JoinColumn( name = "updated_annotation_id" )}
    )
    public Collection<UpdatedAnnotation> getUpdatedAnnotations() {
        return updatedAnnotations;
    }

    public void setUpdatedAnnotations(Collection<UpdatedAnnotation> updatedAnnotations) {
        if (updatedAnnotations != null){
            this.updatedAnnotations = updatedAnnotations;
        }
    }

    public void addUpdatedAnnotationFromInteractor(Collection<uk.ac.ebi.intact.model.Annotation> updatedAnn, UpdateStatus status){
        for (uk.ac.ebi.intact.model.Annotation a : updatedAnn){

            UpdatedAnnotation annotation = new UpdatedAnnotation(a, status);
            this.updatedAnnotations.add(annotation);
        }
    }

    @ManyToMany
    @JoinTable(
            name = "ia_event2updated_aliases",
            joinColumns = {@JoinColumn( name = "protein_event_id" )},
            inverseJoinColumns = {@JoinColumn( name = "updated_alias_id" )}
    )
    public Collection<UpdatedAlias> getUpdatedAliases() {
        return updatedAliases;
    }

    public void setUpdatedAliases(Collection<UpdatedAlias> updatedAliases) {
        if (updatedAliases != null){
            this.updatedAliases = updatedAliases;
        }
    }

    public void addUpdatedAliasesFromInteractor(Collection<InteractorAlias> updatedAlias, UpdateStatus status){
        for (InteractorAlias a : updatedAlias){

            UpdatedAlias alias = new UpdatedAlias(a, status);
            this.updatedAliases.add(alias);
        }
    }

    @Column(name = "name", nullable = false)
    @Enumerated(EnumType.STRING)
    public EventName getName() {
        return name;
    }

    public void setName(EventName name) {
        this.name = name;
    }

    @Column(name = "index", nullable = false)
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
