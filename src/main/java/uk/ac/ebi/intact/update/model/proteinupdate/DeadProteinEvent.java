package uk.ac.ebi.intact.update.model.proteinupdate;

import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.update.model.proteinmapping.results.IdentificationResults;
import uk.ac.ebi.intact.update.model.proteinupdate.protein.Annotation;
import uk.ac.ebi.intact.update.model.proteinupdate.protein.CrossReference;
import uk.ac.ebi.intact.update.model.proteinupdate.protein.IntactProtein;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@Entity
@DiscriminatorValue("DeadProteinEvent")
public class DeadProteinEvent extends XRefUpdateEvent{

    private Collection<Annotation> addedAnnotations;

    private String uniprotReference;

    private IdentificationResults identificationResult;

    public DeadProteinEvent(){
        super();
        addedAnnotations = new ArrayList<Annotation>();
        this.uniprotReference = null;
        this.identificationResult = null;
    }

    public DeadProteinEvent(Collection<InteractorXref> deletedRefs, Collection<InteractorXref> addedRefs, Collection<uk.ac.ebi.intact.model.Annotation> addedAnnotations, InteractorXref uniprotRef, IntactProtein intactProtein, Date created, IdentificationResults result, int index){
        super(deletedRefs, addedRefs, intactProtein, EventName.dead_protein, created, index);
        setAddedAnnotationsFromInteractor(addedAnnotations);
        setUniprotReference(uniprotRef);
        this.identificationResult = result;
    }

    @OneToMany
    @JoinTable(
            name = "ia_deadevent2createdannot",
            joinColumns = {@JoinColumn( name = "dead_event_id" )},
            inverseJoinColumns = {@JoinColumn( name = "created_annotation_id" )}
    )
    public Collection<Annotation> getAddedAnnotations() {
        return addedAnnotations;
    }

    public void setAddedAnnotations(Collection<Annotation> addedAnnotations) {
        this.addedAnnotations = addedAnnotations;
    }

    public void setAddedAnnotationsFromInteractor(Collection<uk.ac.ebi.intact.model.Annotation> addedAnnotations){
        for (uk.ac.ebi.intact.model.Annotation a : addedAnnotations){
            Annotation annotation = new Annotation(a, getCreated());
            this.addedAnnotations.add(annotation);
        }
    }

    @Column(name = "uniprot_reference", nullable = false)
    public String getUniprotReference() {
        return uniprotReference;
    }

    public void setUniprotReference(String uniprotReference) {
        this.uniprotReference = uniprotReference;
    }

    public void setUniprotReference(InteractorXref xRef){
        this.uniprotReference = xRef.getPrimaryId();
    }

    @OneToOne
    @JoinColumn(name = "identification_result_id")
    public IdentificationResults getIdentificationResult() {
        return identificationResult;
    }

    public void setIdentificationResult(IdentificationResults identificationResult) {
        this.identificationResult = identificationResult;
    }
}
