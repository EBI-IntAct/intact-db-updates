package uk.ac.ebi.intact.update.model.protein.update.events;

import org.apache.commons.collections.CollectionUtils;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.update.model.UpdateEvent;
import uk.ac.ebi.intact.update.model.UpdateProcess;
import uk.ac.ebi.intact.update.model.UpdateStatus;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateAnnotation;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;
import uk.ac.ebi.intact.update.model.protein.UpdatedAlias;
import uk.ac.ebi.intact.update.model.protein.UpdatedCrossReference;
import uk.ac.ebi.intact.update.model.protein.update.ProteinEventName;

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
@DiscriminatorValue("PersistentProteinEvent")
@Table(name = "ia_protein_event")
public class PersistentProteinEvent extends UpdateEvent {

    ProteinEventName proteinEventName;

    String uniprotAc;

    String message;

    String proteinAc;

    protected Collection<UpdatedCrossReference> updatedReferences = new ArrayList<UpdatedCrossReference>();
    protected Collection<ProteinUpdateAnnotation> updatedAnnotations = new ArrayList<ProteinUpdateAnnotation>();
    protected Collection<UpdatedAlias> updatedAliases = new ArrayList<UpdatedAlias>();

    public PersistentProteinEvent(){
        super();
        this.proteinEventName = null;
        this.uniprotAc = null;
        this.message = null;
    }

    public PersistentProteinEvent(ProteinUpdateProcess process, ProteinEventName name, Protein protein){
        super(process, name.toString());
        this.proteinEventName = name;
        this.proteinAc = protein != null ? protein.getAc() : null;

        InteractorXref uniprotXref = ProteinUtils.getUniprotXref(protein);
        if (uniprotXref != null){
            this.uniprotAc = uniprotXref.getPrimaryId();
        }
        else {
            this.uniprotAc = null;
        }
        this.message = null;
    }

    public PersistentProteinEvent(ProteinUpdateProcess process, ProteinEventName name, Protein protein, String uniprotAc){
        super(process, name.toString());
        this.proteinEventName = name;
        this.proteinAc = protein != null ? protein.getAc() : null;

        this.uniprotAc = uniprotAc;
        this.message = null;
    }

    public PersistentProteinEvent(ProteinUpdateProcess process, ProteinEventName name, String proteinAc){
        super(process, name.toString());
        this.proteinEventName = name;
        this.proteinAc = proteinAc;

        this.uniprotAc = null;
        this.message = null;
    }

    public PersistentProteinEvent(ProteinUpdateProcess process, ProteinEventName name, String proteinAc, String uniprotAc){
        super(process, name.toString());
        this.proteinEventName = name;
        this.proteinAc = proteinAc;

        this.uniprotAc = uniprotAc;
        this.message = null;
    }

    @OneToMany(mappedBy = "parent", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
    public Collection<UpdatedCrossReference> getUpdatedReferences() {
        return this.updatedReferences;
    }

    public void addUpdatedReferencesFromXref(Collection<Xref> updatedRef, UpdateStatus status){
        for (Xref ref : updatedRef){

            UpdatedCrossReference reference = new UpdatedCrossReference(ref, status);
            if (this.updatedReferences.add(reference)){
                reference.setParent(this);
            }
        }
    }

    public void setUpdatedAnnotations(Collection<ProteinUpdateAnnotation> updatedAnnotations) {
        if (updatedAnnotations != null){
            this.updatedAnnotations = updatedAnnotations;
        }
    }

    public void setUpdatedAliases(Collection<UpdatedAlias> updatedAliases) {
        if (updatedAliases != null){
            this.updatedAliases = updatedAliases;
        }
    }

    public void setUpdatedReferences(Collection<UpdatedCrossReference> updatedReferences) {
        if (updatedReferences != null){
            this.updatedReferences = updatedReferences;
        }
    }

    @OneToMany(mappedBy = "parent", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
    public Collection<ProteinUpdateAnnotation> getUpdatedAnnotations(){
        return this.updatedAnnotations;
    }

    public void addUpdatedAnnotationFromAnnotation(Collection<Annotation> updatedAnn, UpdateStatus status){
        for (uk.ac.ebi.intact.model.Annotation a : updatedAnn){

            ProteinUpdateAnnotation annotation = new ProteinUpdateAnnotation(a, status);
            if(this.updatedAnnotations.add(annotation)){
                annotation.setParent(this);
            }
        }
    }

    @OneToMany(mappedBy = "parent", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
    public Collection<UpdatedAlias> getUpdatedAliases(){
        return this.updatedAliases;
    }

    public void addUpdatedAliasesFromAlias(Collection<Alias> updatedAlias, UpdateStatus status){
        for (Alias a : updatedAlias){

            UpdatedAlias alias = new UpdatedAlias(a, status);

            if (this.updatedAliases.add(alias)){
                alias.setParent(this);
            }
        }
    }

    public boolean addUpdatedXRef(UpdatedCrossReference xref){
        if (this.updatedReferences.add(xref)){
            xref.setParent(this);
            return true;
        }

        return false;
    }

    public boolean removeUpdatedXRef(UpdatedCrossReference xref){
        if (this.updatedReferences.remove(xref)){
            xref.setParent(null);
            return true;
        }

        return false;
    }

    public boolean addUpdatedAnnotation(ProteinUpdateAnnotation ann){
        if (this.updatedAnnotations.add(ann)){
            ann.setParent(this);
            return true;
        }

        return false;
    }

    public boolean removeUpdatedAnnotation(ProteinUpdateAnnotation ann){
        if (this.updatedAnnotations.remove(ann)){
            ann.setParent(null);
            return true;
        }

        return false;
    }

    public boolean addUpdatedAlias(UpdatedAlias alias){
        if (this.updatedAliases.add(alias)){
            alias.setParent(this);
            return true;
        }

        return false;
    }

    public boolean removeAlias(UpdatedAlias alias){
        if (this.updatedAliases.remove(alias)){
            alias.setParent(null);
            return true;
        }

        return false;
    }

    @Transient
    public ProteinEventName getProteinEventName() {

        if (proteinEventName == null && getName() != null){
            try{
                this.proteinEventName = ProteinEventName.valueOf(getName().toLowerCase());
            }
            catch (Exception e) {
                this.proteinEventName = ProteinEventName.none;
            }
        }

        return proteinEventName;
    }

    public void setProteinEventName(ProteinEventName proteinEventName) {
        this.proteinEventName = proteinEventName;
        setName(proteinEventName.toString());
    }

    @Column(name="protein_ac", nullable = false)
    public String getProteinAc() {
        return proteinAc;
    }

    public void setProteinAc(String proteinAc) {
        this.proteinAc = proteinAc;
    }

    @Override
    @ManyToOne( targetEntity = ProteinUpdateProcess.class )
    @JoinColumn( name = "parent_ac" )
    public UpdateProcess<PersistentProteinEvent> getParent(){
        return super.getParent();
    }

    @Column(name = "message")
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Column(name = "uniprot_ac")
    public String getUniprotAc() {
        return uniprotAc;
    }

    public void setUniprotAc(String uniprotAc) {
        this.uniprotAc = uniprotAc;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final PersistentProteinEvent event = (PersistentProteinEvent) o;

        if ( proteinEventName != null ) {
            if (!proteinEventName.equals( event.getProteinEventName())){
                return false;
            }
        }
        else if (event.getProteinEventName()!= null){
            return false;
        }

        if ( message != null ) {
            if (!message.equals( event.getMessage())){
                return false;
            }
        }
        else if (event.getMessage()!= null){
            return false;
        }

        if ( uniprotAc != null ) {
            if (!uniprotAc.equals( event.getUniprotAc())){
                return false;
            }
        }
        else if (event.getUniprotAc()!= null){
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

        if ( proteinEventName != null ) {
            code = 29 * code + proteinEventName.hashCode();
        }

        if ( message != null ) {
            code = 29 * code + message.hashCode();
        }

        if ( uniprotAc != null ) {
            code = 29 * code + uniprotAc.hashCode();
        }

        if ( proteinAc != null ) {
            code = 29 * code + proteinAc.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final PersistentProteinEvent event = (PersistentProteinEvent) o;

        if ( proteinEventName != null ) {
            if (!proteinEventName.equals( event.getProteinEventName())){
                return false;
            }
        }
        else if (event.getProteinEventName()!= null){
            return false;
        }

        if ( message != null ) {
            if (!message.equals( event.getMessage())){
                return false;
            }
        }
        else if (event.getMessage()!= null){
            return false;
        }

        if ( uniprotAc != null ) {
            if (!uniprotAc.equals( event.getUniprotAc())){
                return false;
            }
        }
        else if (event.getUniprotAc()!= null){
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
        buffer.append("Uniprot ac : " + uniprotAc != null ? uniprotAc : "none");
        buffer.append("Message : " + message != null ? message : "none");
        buffer.append(" \n");

        return buffer.toString();
    }
}
