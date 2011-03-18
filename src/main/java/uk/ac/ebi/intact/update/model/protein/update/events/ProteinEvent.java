package uk.ac.ebi.intact.update.model.protein.update.events;

import org.apache.commons.collections.CollectionUtils;
import uk.ac.ebi.intact.model.InteractorAlias;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.HibernatePersistentImpl;
import uk.ac.ebi.intact.update.model.protein.update.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * The super class which represents an event of a proteinAc update
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="objclass", discriminatorType=DiscriminatorType.STRING)
@DiscriminatorValue("ProteinEvent")
@Table(name = "ia_update_event")
public class ProteinEvent extends HibernatePersistentImpl {

    EventName name;
    int index;

    String proteinAc;
    UpdateProcess parent;

    private Collection<UpdatedCrossReference> updatedReferences;
    private Collection<UpdatedAnnotation> updatedAnnotations;
    private Collection<UpdatedAlias> updatedAliases;

    public ProteinEvent(){
        super();
        this.name = EventName.uniprot_update;
        this.index = 0;

        this.proteinAc = null;

        updatedReferences = new ArrayList<UpdatedCrossReference>();
        updatedAnnotations = new ArrayList<UpdatedAnnotation>();
        updatedAliases = new ArrayList<UpdatedAlias>();
    }

    public ProteinEvent(UpdateProcess process, EventName name, Protein protein, int index){
        super();
        this.name = name;
        this.index = index;
        this.proteinAc = protein != null ? protein.getAc() : null;
        this.parent = process;
        updatedReferences = new ArrayList<UpdatedCrossReference>();
        updatedAnnotations = new ArrayList<UpdatedAnnotation>();
        updatedAliases = new ArrayList<UpdatedAlias>();
    }

    @ManyToOne
    @JoinColumn(name="parent_id", nullable=false)
    public UpdateProcess getParent() {
        return this.parent;
    }

    public void setParent(UpdateProcess updateProcess) {
        this.parent = updateProcess;
    }

    @Column(name="protein_ac", nullable = false)
    public String getProteinAc() {
        return proteinAc;
    }

    public void setProteinAc(String proteinAc) {
        this.proteinAc = proteinAc;
    }

    @OneToMany(mappedBy = "parent", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
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

            UpdatedCrossReference reference = new UpdatedCrossReference(ref, status);
            reference.setParent(this);
            this.updatedReferences.add(reference);
        }
    }

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH})
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

    @OneToMany(mappedBy = "parent", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
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
            alias.setParent(this);
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

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final ProteinEvent event = ( ProteinEvent ) o;

        if ( name != null ) {
            if (!name.equals( event.getName())){
                return false;
            }
        }
        else if (event.getName()!= null){
            return false;
        }

        if ( proteinAc != null ) {
            if (!proteinAc.equals( event.getProteinAc())){
                return false;
            }
        }
        else if (event.getProteinAc()!= null){
            return false;
        }

        if (index != event.getIndex()){
            return false;
        }

        return true;
    }

    /**
     * This class overwrites equals. To ensure proper functioning of HashTable,
     * hashCode must be overwritten, too.
     *
     * @return hash code of the object.
     */
    @Override
    public int hashCode() {

        int code = 29;

        code = 29 * code + super.hashCode();

        if ( name != null ) {
            code = 29 * code + name.hashCode();
        }

        if ( proteinAc != null ) {
            code = 29 * code + proteinAc.hashCode();
        }

        code = 29 * code + Integer.toString(index).hashCode();


        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final ProteinEvent event = ( ProteinEvent ) o;

        if ( name != null ) {
            if (!name.equals( event.getName())){
                return false;
            }
        }
        else if (event.getName()!= null){
            return false;
        }

        if ( proteinAc != null ) {
            if (!proteinAc.equals( event.getProteinAc())){
                return false;
            }
        }
        else if (event.getProteinAc()!= null){
            return false;
        }

        if (index != event.getIndex()){
            return false;
        }

        if (!CollectionUtils.isEqualCollection(updatedAliases, event.getUpdatedAliases())){
            return false;
        }

        if (!CollectionUtils.isEqualCollection(updatedAnnotations, event.getUpdatedAnnotations())){
            return false;
        }

        return CollectionUtils.isEqualCollection(updatedReferences, event.getUpdatedReferences());
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Protein : " + proteinAc != null ? proteinAc : "none");
        buffer.append(" \n");

        buffer.append("Event : " + name != null ? name.toString() : "none");
        buffer.append(" \n");

        buffer.append("Index : " + index);
        buffer.append(" \n");

        if (!updatedAliases.isEmpty()){
            buffer.append("Updated aliases : ");

            for (UpdatedAlias p : updatedAliases){
                buffer.append(p.toString() + ", ");
            }

            buffer.append("\n");
        }

        if (!updatedAnnotations.isEmpty()){
            buffer.append("Updated annotations : ");

            for (UpdatedAnnotation p : updatedAnnotations){
                buffer.append(p.toString() + ", ");
            }

            buffer.append("\n");
        }

        if (!updatedReferences.isEmpty()){
            buffer.append("Updated references : ");

            for (UpdatedCrossReference p : updatedReferences){
                buffer.append(p.toString() + ", ");
            }

            buffer.append("\n");
        }

        return buffer.toString();
    }
}
