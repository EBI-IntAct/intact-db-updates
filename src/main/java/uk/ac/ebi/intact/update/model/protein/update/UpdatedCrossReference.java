package uk.ac.ebi.intact.update.model.protein.update;

import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.update.model.HibernatePersistentImpl;

import javax.persistence.*;

/**
 * Cross reference of a protein
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@Entity
@Table(name = "ia_updated_xref")
public class UpdatedCrossReference extends HibernatePersistentImpl{

    private String database;
    private String identifier;
    private String qualifier;

    private UpdateStatus status;

    public UpdatedCrossReference(){
        super();
        this.database = null;
        this.identifier = null;
        this.qualifier = null;
        this.status = UpdateStatus.none;
    }

    public UpdatedCrossReference(InteractorXref ref, UpdateStatus status){

        super();
        if (ref != null){

            this.database = ref.getCvDatabase() != null ? ref.getCvDatabase().getAc() : null;

            this.identifier = ref.getPrimaryId();

            this.qualifier = ref.getCvXrefQualifier() != null ? ref.getCvXrefQualifier().getAc() : null;
        }
        else {
            this.database = null;
            this.identifier = null;
            this.qualifier = null;
        }

        this.status = status != null ? status : UpdateStatus.none;
    }

    @Column(name="database_ac", nullable = false)
    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    @Column(name = "identifier", nullable = false)
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Column(name = "qualifier_ac", nullable = true)
    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
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
