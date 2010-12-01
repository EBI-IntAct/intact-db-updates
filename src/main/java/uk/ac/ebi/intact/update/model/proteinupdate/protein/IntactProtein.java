package uk.ac.ebi.intact.update.model.proteinupdate.protein;

import uk.ac.ebi.intact.update.model.HibernatePersistentImpl;
import uk.ac.ebi.intact.update.model.proteinupdate.ProteinEvent;
import uk.ac.ebi.intact.update.model.proteinupdate.range.UpdatedRange;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.ProteinUtils;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-Oct-2010</pre>
 */
@Entity
@Table(name = "ia_intact_protein")
public class IntactProtein extends HibernatePersistentImpl {

    private String protein_ac;
    private Collection<CrossReference> crossReferences = new ArrayList<CrossReference>();
    private Collection<Alias> aliases = new ArrayList<Alias>();
    private Collection<Annotation> annotations = new ArrayList<Annotation>();
    private Collection<String> interactions = new ArrayList<String>();
    private String sequence;
    private String uniprotAc;

    private Collection<ProteinEvent> events;
    private Collection<UpdatedRange> rangeUpdates;

    public IntactProtein(){
        this.protein_ac = null;
        this.sequence = null;
        this.uniprotAc = null;
        this.events = new ArrayList<ProteinEvent>();
        this.rangeUpdates = new ArrayList<UpdatedRange>();
    }

    public IntactProtein(Protein protein){
        setCreated(new Date(System.currentTimeMillis()));

        if (protein != null){
            this.protein_ac = protein.getAc();

            InteractorXref uniprot = ProteinUtils.getUniprotXref(protein);
            if (uniprot != null){
                this.uniprotAc = uniprot.getPrimaryId();
            }

            setCrossReferencesFromInteractorRefs(protein.getXrefs());
            setAliasesFromInteractorAliases(protein.getAliases());
            setInteractionsFromInteractor(protein.getActiveInstances());

            this.events = new ArrayList<ProteinEvent>();
            this.rangeUpdates = new ArrayList<UpdatedRange>();
        }
        else {
            this.protein_ac = null;
            this.sequence = null;
            this.uniprotAc = null;
            this.events = new ArrayList<ProteinEvent>();
            this.rangeUpdates = new ArrayList<UpdatedRange>();
        }
    }

    @Column(name = "protein_ac", nullable = false)
    public String getProtein_ac() {
        return protein_ac;
    }

    public void setProtein_ac(String protein_ac) {
        this.protein_ac = protein_ac;
    }

    @OneToMany( mappedBy = "intactProtein", cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE} )
    public Collection<CrossReference> getCrossReferences() {
        return crossReferences;
    }

    public void setCrossReferences(Collection<CrossReference> crossReferences) {
        this.crossReferences = crossReferences;

        for (CrossReference ref : crossReferences){
            ref.setIntactProtein(this);
        }
    }

    public void setCrossReferencesFromInteractorRefs(Collection<InteractorXref> crossReferences) {
        if (crossReferences != null){
            for (InteractorXref ref : crossReferences){
                CrossReference convertedXRef = new CrossReference(ref, getCreated());
                convertedXRef.setIntactProtein(this);
                this.crossReferences.add(convertedXRef);
            }
        }
    }

    @OneToMany( mappedBy = "intactProtein", cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE} )
    public Collection<Alias> getAliases() {
        return aliases;
    }

    public void setAliases(Collection<Alias> aliases) {
        this.aliases = aliases;

        for (Alias alias : aliases){
            alias.setIntactProtein(this);
        }
    }

    public void setAliasesFromInteractorAliases(Collection<InteractorAlias> aliases) {
        if (aliases != null){
            for (InteractorAlias alias : aliases){
                Alias convertedAlias = new Alias(alias, getCreated());
                convertedAlias.setIntactProtein(this);
                this.aliases.add(convertedAlias);
            }
        }
    }

    @OneToMany( mappedBy = "intactProtein", cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE} )
    public Collection<Annotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Collection<Annotation> annotations) {
        this.annotations = annotations;

        for (Annotation annotation : annotations){
            annotation.setIntactProtein(this);
        }
    }

    public void setAnnotationsFromInteractorAnnotations(Collection<uk.ac.ebi.intact.model.Annotation> annotations) {
        if (annotations != null){
            for (uk.ac.ebi.intact.model.Annotation annotation : annotations){
                Annotation convertedAnnotation = new Annotation(annotation, getCreated());
                convertedAnnotation.setIntactProtein(this);
                this.annotations.add(convertedAnnotation);
            }
        }
    }

    @ElementCollection
    @JoinTable(name = "ia_protein2interactions", joinColumns = @JoinColumn(name="intact_protein_id"))
    @Column(name = "protein_interaction", nullable = false)
    public Collection<String> getInteractions() {
        return interactions;
    }

    public void setInteractions(Collection<String> interactions) {
        this.interactions = interactions;
    }

    public void setInteractionsFromInteractor(Collection<Component> interactions) {
        if (interactions != null){
            for (Component interaction : interactions){
                this.interactions.add(interaction.getInteractionAc());
            }
        }
    }

    @Lob
    @Column(name = "protein_sequence", nullable = true)
    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    @ManyToMany( cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE} )

    @JoinTable(
            name = "ia_protein2event",
            joinColumns = {@JoinColumn( name = "intact_protein_id" )},
            inverseJoinColumns = {@JoinColumn( name = "protein_event_id" )}
    )
    public Collection<ProteinEvent> getEvents() {
        return events;
    }

    public void setEvents(Collection<ProteinEvent> events) {
        this.events = events;
    }

    @JoinTable(
            name = "ia_protein2range",
            joinColumns = {@JoinColumn( name = "intact_protein_id" )},
            inverseJoinColumns = {@JoinColumn( name = "range_update_id" )}
    )
    public Collection<UpdatedRange> getRangeUpdates() {
        return rangeUpdates;
    }

    public void setRangeUpdates(Collection<UpdatedRange> rangeUpdates) {
        this.rangeUpdates = rangeUpdates;
    }

    @Column(name = "uniprot_ac", nullable = true)
    public String getUniprotAc() {
        return uniprotAc;
    }

    public void setUniprotAc(String uniprotAc) {
        this.uniprotAc = uniprotAc;
    }
}
