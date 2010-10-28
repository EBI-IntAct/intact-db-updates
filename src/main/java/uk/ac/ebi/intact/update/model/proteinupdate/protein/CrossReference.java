package uk.ac.ebi.intact.update.model.proteinupdate.protein;

import uk.ac.ebi.intact.update.model.HibernatePersistentImpl;
import uk.ac.ebi.intact.model.CvDatabase;
import uk.ac.ebi.intact.model.CvXrefQualifier;
import uk.ac.ebi.intact.model.InteractorXref;

import javax.persistence.*;
import java.util.Date;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@Entity
@Table(name = "ia_protein_xref")
public class CrossReference extends HibernatePersistentImpl{

    private String databaseAc;
    private String identifier;
    private String qualifierAc;
    private String xRefAc;

    private IntactProtein intactProtein;

    public CrossReference(){
        super();
        this.databaseAc = null;
        this.identifier = null;
        this.qualifierAc = null;
        this.intactProtein = null;
        this.xRefAc = null;
    }

    public CrossReference(InteractorXref ref, Date created){

        super();
        if (ref != null){
            this.xRefAc = ref.getAc();
            
            CvDatabase database = ref.getCvDatabase();
            if (database != null){
                this.databaseAc = database.getAc();
            }
            else {
                this.databaseAc = null;
            }

            this.identifier = ref.getPrimaryId();

            CvXrefQualifier qualifier = ref.getCvXrefQualifier();
            if (qualifier != null){
                this.qualifierAc = qualifier.getAc();
            }
            else {
                this.databaseAc = null;
            }
        }
        else {
            this.databaseAc = null;
            this.identifier = null;
            this.qualifierAc = null;
        }
        setCreated(created);
        this.intactProtein = null;
    }

    public CrossReference(String xRefAc, String database, String identifier, String qualifier){

        super();
        this.xRefAc = xRefAc;
        this.databaseAc = database;
        this.identifier = identifier;
        this.qualifierAc = qualifier;
        setCreated(new Date(System.currentTimeMillis()));
        this.intactProtein = null;
    }

    @Column(name = "database_ac", nullable = false)
    public String getDatabaseAc() {
        return databaseAc;
    }

    public void setDatabaseAc(String databaseAc) {
        this.databaseAc = databaseAc;
    }

    @Column(name = "identifier", nullable = false)
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Column(name = "qualifier_ac", nullable = true)
    public String getQualifierAc() {
        return qualifierAc;
    }

    public void setQualifierAc(String qualifierAc) {
        this.qualifierAc = qualifierAc;
    }

    @ManyToOne
    @JoinColumn( name = "protein_id", nullable = true)
    public IntactProtein getIntactProtein() {
        return intactProtein;
    }

    public void setIntactProtein(IntactProtein intactProtein) {
        this.intactProtein = intactProtein;
    }

    @Column(name = "xref_ac", nullable = false)
    public String getXRefAc() {
        return xRefAc;
    }

    public void setXRefAc(String xRefAc) {
        this.xRefAc = xRefAc;
    }
}
