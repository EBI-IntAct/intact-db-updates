package uk.ac.ebi.intact.update.model.proteinupdate;

import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.update.model.proteinupdate.protein.*;

import javax.persistence.*;
import java.util.*;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */

public class UniprotUpdateEvent extends XRefUpdateEvent{

    private Collection<String> errors;
    int taxId;
    String shortLabel;
    String fullName;
    private Collection<Alias> deletedAlias;
    private Collection<Alias> createdAlias;

    public UniprotUpdateEvent(){
        super();
        this.taxId = 0;
        this.shortLabel = null;
        this.fullName = null;
        errors = new ArrayList<String>();
        deletedAlias = new ArrayList<Alias>();
        createdAlias = new ArrayList<Alias>();
    }

    public UniprotUpdateEvent(Collection<InteractorXref> deletedRefs, Collection<InteractorXref> addedRefs, Collection<Alias> deletedAlias,
                              Collection<Alias> createdAlias, IntactProtein intactProtein, Collection<String> errors,
                              int taxId, String shortLabel, String fullName, Date created, int index){
        super(deletedRefs, addedRefs, intactProtein, EventName.uniprot_update, created, index);
        this.taxId = taxId;
        this.shortLabel = shortLabel;
        this.fullName = fullName;
        setErrors(errors);
        this.createdAlias = createdAlias;
        this.deletedAlias = deletedAlias;
    }
    
    @ElementCollection
    @JoinTable(name = "ia_update2error", joinColumns = @JoinColumn(name="update_event_id"))
    @Column(name = "update_error", nullable = false)
    public Collection<String> getErrors() {
        return errors;
    }

    public void setErrors(Collection<String> errors) {
        this.errors = errors;
    }

    @Column(name = "taxid", nullable = false)
    public int getTaxId() {
        return taxId;
    }

    public void setTaxId(int taxId) {
        this.taxId = taxId;
    }

    @Column(name = "shortlabel", nullable = false)
    public String getShortLabel() {
        return shortLabel;
    }

    public void setShortLabel(String shortLabel) {
        this.shortLabel = shortLabel;
    }

    @Column(name = "fullname", nullable = true)
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @OneToMany
    @JoinTable(
            name = "ia_event2deletedalias",
            joinColumns = {@JoinColumn( name = "protein_event_id" )},
            inverseJoinColumns = {@JoinColumn( name = "deleted_alias_id" )}
    )
    public Collection<Alias> getDeletedAlias() {
        return deletedAlias;
    }

    public void setDeletedAlias(Collection<Alias> deletedAlias) {
        this.deletedAlias = deletedAlias;
    }

    @OneToMany
    @JoinTable(
            name = "ia_event2createdalias",
            joinColumns = {@JoinColumn( name = "protein_event_id" )},
            inverseJoinColumns = {@JoinColumn( name = "created_alias_id" )}
    )
    public Collection<Alias> getCreatedAlias() {
        return createdAlias;
    }

    public void setCreatedAlias(Collection<Alias> createdAlias) {
        this.createdAlias = createdAlias;
    }
}
