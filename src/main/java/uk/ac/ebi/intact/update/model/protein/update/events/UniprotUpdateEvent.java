package uk.ac.ebi.intact.update.model.protein.update.events;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.annotations.DiscriminatorFormula;
import uk.ac.ebi.intact.model.Annotation;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.UpdateStatus;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;
import uk.ac.ebi.intact.update.model.protein.feature.FeatureUpdatedAnnotation;
import uk.ac.ebi.intact.update.model.protein.range.PersistentUpdatedRange;
import uk.ac.ebi.intact.update.model.protein.update.ProteinEventName;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Event for the basic update of a uniprot proteinAc
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@Entity
@DiscriminatorFormula("objclass")
@DiscriminatorValue("UniprotUpdateEvent")
public class UniprotUpdateEvent extends PersistentProteinEvent {

    private String updatedShortLabel;
    private String updatedFullName;
    private String uniprotQuery;

    private Collection<FeatureUpdatedAnnotation> updatedFeatureAnnotations = new ArrayList<FeatureUpdatedAnnotation>();
    private Collection<PersistentUpdatedRange> updatedRanges = new ArrayList<PersistentUpdatedRange>();

    public UniprotUpdateEvent(){
        super();
        this.updatedShortLabel = null;
        this.updatedFullName = null;
    }

    public UniprotUpdateEvent(ProteinUpdateProcess updateProcess, Protein protein, String shortlabel, String fullname, String uniprotEntry){
        super(updateProcess, ProteinEventName.uniprot_update, protein);
        this.updatedShortLabel = shortlabel;
        this.updatedFullName = fullname;
        this.uniprotQuery = uniprotEntry;
    }

    public UniprotUpdateEvent(ProteinUpdateProcess updateProcess, Protein protein, String uniprotEntry){
        super(updateProcess, ProteinEventName.uniprot_update, protein);
        this.updatedShortLabel = null;
        this.updatedFullName = null;
        this.uniprotQuery = uniprotEntry;
    }

    @Column(name = "shortlabel")
    public String getUpdatedShortLabel() {
        return updatedShortLabel;
    }

    public void setUpdatedShortLabel(String updatedShortLabel) {
        this.updatedShortLabel = updatedShortLabel;
    }

    @Column(name = "fullname")
    public String getUpdatedFullName() {
        return updatedFullName;
    }

    public void setUpdatedFullName(String updatedFullName) {
        this.updatedFullName = updatedFullName;
    }

    @Column(name = "uniprot")
    public String getUniprotQuery() {
        return uniprotQuery;
    }

    public void setUniprotQuery(String uniprot) {
        this.uniprotQuery = uniprot;
    }

    @OneToMany(mappedBy = "parent", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
    public Collection<FeatureUpdatedAnnotation> getUpdatedFeatureAnnotations(){
        return updatedFeatureAnnotations;
    }

    public void addUpdatedFeatureAnnotationFromAnnotation(String featureAc, Collection<Annotation> updatedAnn, UpdateStatus status){
        for (uk.ac.ebi.intact.model.Annotation a : updatedAnn){

            FeatureUpdatedAnnotation annotation = new FeatureUpdatedAnnotation(featureAc, a, status);
            this.updatedFeatureAnnotations.add(annotation);
        }
    }

    public void setUpdatedFeatureAnnotations(Collection<FeatureUpdatedAnnotation> updatedFeatureAnnotations) {
        this.updatedFeatureAnnotations = updatedFeatureAnnotations;
    }

    @OneToMany(mappedBy = "parent", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
    public Collection<PersistentUpdatedRange> getUpdatedRanges(){
        return updatedRanges;
    }

    public boolean addRangeUpdate(PersistentUpdatedRange up){
        if (updatedRanges.add(up)){
            up.setParent(this);
            return true;
        }

        return false;
    }

    public boolean removeRangeUpdate(PersistentUpdatedRange up){
        if (updatedRanges.remove(up)){
            up.setParent(null);
            return true;
        }

        return false;
    }

    public void setUpdatedRanges(Collection<PersistentUpdatedRange> updatedRanges) {
        this.updatedRanges = updatedRanges;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final UniprotUpdateEvent event = ( UniprotUpdateEvent ) o;

        if ( updatedShortLabel != null ) {
            if (!updatedShortLabel.equals( event.getUpdatedShortLabel())){
                return false;
            }
        }
        else if (event.getUpdatedShortLabel()!= null){
            return false;
        }

        if ( updatedFullName != null ) {
            if (!updatedFullName.equals( event.getUpdatedFullName())){
                return false;
            }
        }
        else if (event.getUpdatedFullName()!= null){
            return false;
        }

        if ( uniprotQuery != null ) {
            if (!uniprotQuery.equals( event.getUniprotQuery())){
                return false;
            }
        }
        else if (event.getUniprotQuery()!= null){
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

        if ( updatedShortLabel != null ) {
            code = 29 * code + updatedShortLabel.hashCode();
        }

        if ( updatedFullName != null ) {
            code = 29 * code + updatedFullName.hashCode();
        }

        if ( uniprotQuery != null ) {
            code = 29 * code + uniprotQuery.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final UniprotUpdateEvent event = ( UniprotUpdateEvent ) o;

        if ( updatedShortLabel != null ) {
            if (!updatedShortLabel.equals( event.getUpdatedShortLabel())){
                return false;
            }
        }
        else if (event.getUpdatedShortLabel()!= null){
            return false;
        }

        if ( updatedFullName != null ) {
            if (!updatedFullName.equals( event.getUpdatedFullName())){
                return false;
            }
        }
        else if (event.getUpdatedFullName()!= null){
            return false;
        }

        if ( uniprotQuery != null ) {
            if (!uniprotQuery.equals( event.getUniprotQuery())){
                return false;
            }
        }
        else if (event.getUniprotQuery()!= null){
            return false;
        }

        if (!CollectionUtils.isEqualCollection(this.updatedRanges, event.getUpdatedRanges())){
            return false;
        }

        return CollectionUtils.isEqualCollection(updatedFeatureAnnotations, event.getUpdatedFeatureAnnotations());
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Uniprot update event : [ shortlabel = " + updatedShortLabel != null ? updatedShortLabel : "none" + ", fullname = " + updatedFullName != null ? updatedFullName : "none" + ", uniprot query = " + uniprotQuery != null ? uniprotQuery : "none");

        return buffer.toString();
    }

}
