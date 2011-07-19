package uk.ac.ebi.intact.update.model;

import org.apache.commons.collections.CollectionUtils;
import uk.ac.ebi.intact.model.AnnotatedObject;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Abstract class for each updateEvent
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>18/07/11</pre>
 */
@MappedSuperclass
public abstract class UpdateEvent<T extends UpdatedAlias, X extends UpdatedCrossReference, A extends UpdatedAnnotation> extends HibernatePersistentImpl implements Serializable {

    String name;

    String intactObjectAc;
    UpdateProcess parent;

    protected Collection<X> updatedReferences = new ArrayList<X>();
    protected Collection<A> updatedAnnotations = new ArrayList<A>();
    protected Collection<T> updatedAliases = new ArrayList<T>();

    public UpdateEvent(){
        super();
        this.name = null;

        this.intactObjectAc = null;
    }

    public UpdateEvent(UpdateProcess process, String name, AnnotatedObject intactObject){
        super();
        this.name = name;
        this.intactObjectAc = intactObject != null ? intactObject.getAc() : null;
        this.parent = process;
    }

    public UpdateEvent(UpdateProcess process, String name, String intactObjectAc){
        super();
        this.name = name;
        this.intactObjectAc = intactObjectAc;
        this.parent = process;
    }

    @Transient
    public UpdateProcess getParent() {
        return this.parent;
    }

    public void setParent(UpdateProcess updateProcess) {
        this.parent = updateProcess;
    }

    @Column(name="intact_object_ac", nullable = false)
    public String getIntactObjectAc() {
        return intactObjectAc;
    }

    public void setIntactObjectAc(String intactObjectAc) {
        this.intactObjectAc = intactObjectAc;
    }

    @Transient
    public Collection<X> getUpdatedReferences() {
        return updatedReferences;
    }

    public void setUpdatedReferences(Collection<X> updatedReferences) {
        if (updatedReferences != null){
            this.updatedReferences = updatedReferences;
        }
    }

    public boolean addUpdatedXRef(X xref){
        if (this.updatedReferences.add(xref)){
            xref.setParent(this);
            return true;
        }

        return false;
    }

    public boolean removeUpdatedXRef(X xref){
        if (this.updatedReferences.remove(xref)){
            xref.setParent(null);
            return true;
        }

        return false;
    }

    @Transient
    public Collection<A> getUpdatedAnnotations() {
        return updatedAnnotations;
    }

    public void setUpdatedAnnotations(Collection<A> updatedAnnotations) {
        if (updatedAnnotations != null){
            this.updatedAnnotations = updatedAnnotations;
        }
    }

    public boolean addUpdatedAnnotation(A ann){
        return this.updatedAnnotations.add(ann);
    }

    public boolean removeUpdatedAnnotation(A ann){
        return this.updatedAnnotations.remove(ann);
    }

    @Transient
    public Collection<T> getUpdatedAliases() {
        return updatedAliases;
    }

    public void setUpdatedAliases(Collection<T> updatedAliases) {
        if (updatedAliases != null){
            this.updatedAliases = updatedAliases;
        }
    }

    public boolean addUpdatedAlias(T alias){
        if (this.updatedAliases.add(alias)){
            alias.setParent(this);
            return true;
        }

        return false;
    }

    public boolean removeAlias(T alias){
        if (this.updatedAliases.remove(alias)){
            alias.setParent(null);
            return true;
        }

        return false;
    }

    @Column(name = "name", nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final UpdateEvent event = (UpdateEvent) o;

        if ( name != null ) {
            if (!name.equals( event.getName())){
                return false;
            }
        }
        else if (event.getName()!= null){
            return false;
        }

        if ( intactObjectAc != null ) {
            if (!intactObjectAc.equals( event.getIntactObjectAc())){
                return false;
            }
        }
        else if (event.getIntactObjectAc()!= null){
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

        if ( intactObjectAc != null ) {
            code = 29 * code + intactObjectAc.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final UpdateEvent event = (UpdateEvent) o;

        if ( name != null ) {
            if (!name.equals( event.getName())){
                return false;
            }
        }
        else if (event.getName()!= null){
            return false;
        }

        if ( intactObjectAc != null ) {
            if (!intactObjectAc.equals( event.getIntactObjectAc())){
                return false;
            }
        }
        else if (event.getIntactObjectAc()!= null){
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

        buffer.append("Intact Object : " + intactObjectAc != null ? intactObjectAc : "none");
        buffer.append(" \n");

        buffer.append("Event : " + name != null ? name.toString() : "none");
        buffer.append(" \n");

        return buffer.toString();
    }
}
