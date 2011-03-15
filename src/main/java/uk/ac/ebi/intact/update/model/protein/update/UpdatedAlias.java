package uk.ac.ebi.intact.update.model.protein.update;

import uk.ac.ebi.intact.model.InteractorAlias;
import uk.ac.ebi.intact.update.model.HibernatePersistentImpl;

import javax.persistence.*;

/**
 * UpdatedAlias of a protein
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@Entity
@Table(name = "ia_protein_alias")
public class UpdatedAlias extends HibernatePersistentImpl {

    private String type;
    private String name;

    private UpdateStatus status;

    public UpdatedAlias(){
        super();
        this.name = null;
        this.type = null;
        this.status = UpdateStatus.none;
    }

    public UpdatedAlias(String type, String name, UpdateStatus status){
        super();
        this.name = name;
        this.type = type;
        this.status = status != null ? status : UpdateStatus.none;
    }

    public UpdatedAlias(InteractorAlias alias, UpdateStatus status){
        super();
        if (alias != null){
            type = alias.getCvAliasType() != null ? alias.getCvAliasType().getAc() : null;

            this.name = alias.getName();
        }
        else {
            this.type = null;
            this.name = null;
        }
        this.status = status != null ? status : UpdateStatus.none;
    }

    @Column(name="type_ac", nullable = false)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Column(name = "name", nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    public UpdateStatus getStatus() {
        return status;
    }

    public void setStatus(UpdateStatus status) {
        this.status = status;
    }
}
