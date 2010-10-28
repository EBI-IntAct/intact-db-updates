package uk.ac.ebi.intact.update.model.proteinupdate.protein;

import uk.ac.ebi.intact.update.model.HibernatePersistentImpl;
import uk.ac.ebi.intact.model.CvAliasType;
import uk.ac.ebi.intact.model.InteractorAlias;

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
@Table(name = "ia_protein_alias")
public class Alias extends HibernatePersistentImpl {

    private String aliasAc;
    private String typeAc;
    private String name;

    private IntactProtein intactProtein;

    public Alias(){
        super();
        this.typeAc = null;
        this.name = null;
        this.intactProtein = null;
        this.aliasAc = null;
    }

    public Alias(String aliasAc, String type, String name){
        super();
        this.aliasAc = aliasAc;
        this.typeAc = type;
        this.name = name;
        this.intactProtein = null;
        setCreated(new Date(System.currentTimeMillis()));
    }

    public Alias(InteractorAlias alias, Date created){
        super();
        setCreated(created);
        if (alias != null){
            this.aliasAc = alias.getAc();
            CvAliasType type = alias.getCvAliasType();
            if (type != null){
                this.typeAc = type.getAc();
            }
            else {
                this.typeAc = null;
            }

            this.name = alias.getName();
        }
        else {
            this.typeAc = null;
            this.name = null;
            this.intactProtein = null;
        }
    }

    @Column(name = "type_ac", nullable = false)
    public String getTypeAc() {
        return typeAc;
    }

    public void setTypeAc(String typeAc) {
        this.typeAc = typeAc;
    }

    @Column(name = "name", nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ManyToOne
    @JoinColumn( name = "protein_id", nullable = true)
    public IntactProtein getIntactProtein() {
        return intactProtein;
    }

    public void setIntactProtein(IntactProtein intactProtein) {
        this.intactProtein = intactProtein;
    }

    @Column(name = "alias_ac", nullable = false)
    public String getAliasAc() {
        return aliasAc;
    }

    public void setAliasAc(String aliasAc) {
        this.aliasAc = aliasAc;
    }
}
