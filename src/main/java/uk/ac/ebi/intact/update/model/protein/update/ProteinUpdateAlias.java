package uk.ac.ebi.intact.update.model.protein.update;

import uk.ac.ebi.intact.model.Alias;
import uk.ac.ebi.intact.update.model.UpdateEvent;
import uk.ac.ebi.intact.update.model.UpdateStatus;
import uk.ac.ebi.intact.update.model.UpdatedAlias;
import uk.ac.ebi.intact.update.model.protein.update.events.PersistentProteinEvent;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * ProteinUpdateAlias of a protein
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@Entity
@Table(name = "ia_protein_updated_alias")
public class ProteinUpdateAlias extends UpdatedAlias {


    public ProteinUpdateAlias(){
        super();
    }

    public ProteinUpdateAlias(String type, String name, UpdateStatus status){
        super(type, name, status);
    }

    public ProteinUpdateAlias(Alias alias, UpdateStatus status){
        super(alias, status);
    }

    @Override
    @ManyToOne( targetEntity = PersistentProteinEvent.class )
    @JoinColumn( name = "event_id" )
    public UpdateEvent getParent() {
        return super.getParent();
    }
}
