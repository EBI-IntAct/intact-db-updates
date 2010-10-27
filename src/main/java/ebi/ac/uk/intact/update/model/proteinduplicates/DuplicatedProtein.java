package ebi.ac.uk.intact.update.model.proteinduplicates;

import ebi.ac.uk.intact.update.model.HibernatePersistentImpl;
import org.hibernate.annotations.CollectionOfElements;
import uk.ac.ebi.intact.model.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-Oct-2010</pre>
 */

public class DuplicatedProtein extends HibernatePersistentImpl{

    private String protein_ac;
    private Collection<String> crossReferences = new ArrayList<String>();
    private Collection<String> aliases = new ArrayList<String>();
    private Collection<String> annotations = new ArrayList<String>();
    private Collection<String> interactions = new ArrayList<String>();
    private String sequence;

    private Protein originalProtein;

    public DuplicatedProtein(){
        this.protein_ac = null;
    }

    @Column(name = "protein_ac", nullable = false)
    public String getProtein_ac() {
        return protein_ac;
    }

    public void setProtein_ac(String protein_ac) {
        this.protein_ac = protein_ac;
    }

    @CollectionOfElements
    @JoinTable(name = "ia_duplicated2xrefs", joinColumns = @JoinColumn(name="duplicated_id"))
    @Column(name = "protein_xref", nullable = false)
    public Collection<String> getCrossReferences() {
        return crossReferences;
    }

    public void setCrossReferences(Collection<String> crossReferences) {
        this.crossReferences = crossReferences;
    }

    public void setCrossReferencesFromInteractorRefs(Collection<InteractorXref> crossReferences) {
        if (crossReferences != null){
            for (InteractorXref ref : crossReferences){
                this.crossReferences.add(ref.getAc());
            }
        }
    }

    @CollectionOfElements
    @JoinTable(name = "ia_duplicated2aliases", joinColumns = @JoinColumn(name="duplicated_id"))
    @Column(name = "protein_aliases", nullable = false)
    public Collection<String> getAliases() {
        return aliases;
    }

    public void setAliases(Collection<String> aliases) {
        this.aliases = aliases;
    }

    public void setAliasesFromInteractorAliases(Collection<InteractorAlias> aliases) {
        if (aliases != null){
            for (InteractorAlias alias : aliases){
                this.aliases.add(alias.getAc());
            }
        }
    }

    @CollectionOfElements
    @JoinTable(name = "ia_duplicated2annotations", joinColumns = @JoinColumn(name="duplicated_id"))
    @Column(name = "protein_annotation", nullable = false)
    public Collection<String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Collection<String> annotations) {
        this.annotations = annotations;
    }

    public void setAnnotationsFromInteractorAnnotations(Collection<Annotation> annotations) {
        if (annotations != null){
            for (Annotation annotation : annotations){
                this.annotations.add(annotation.getAc());
            }
        }
    }

    @CollectionOfElements
    @JoinTable(name = "ia_duplicated2interactions", joinColumns = @JoinColumn(name="duplicated_id"))
    @Column(name = "protein_interactions", nullable = false)
    public Collection<String> getInteractions() {
        return interactions;
    }

    public void setInteractions(Collection<String> interactions) {
        this.interactions = interactions;
    }

    public void setInteractionsFromInteractorAliases(Collection<Interaction> interactions) {
        if (interactions != null){
            for (Interaction interaction : interactions){
                this.interactions.add(interaction.getAc());
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

    @ManyToOne
    @JoinColumn( name = "original_protein_ac" )
    public Protein getOriginalProtein() {
        return originalProtein;
    }

    public void setOriginalProtein(Protein originalProtein) {
        this.originalProtein = originalProtein;
    }
}
