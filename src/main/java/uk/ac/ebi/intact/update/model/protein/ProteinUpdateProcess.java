package uk.ac.ebi.intact.update.model.protein;

import org.apache.commons.collections.CollectionUtils;
import uk.ac.ebi.intact.update.model.UpdateProcessImpl;
import uk.ac.ebi.intact.update.model.protein.update.events.PersistentProteinEvent;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * Represents the update steps applied to proteins in IntAct
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>14/03/11</pre>
 */
@Entity
@Table(name = "ia_protein_update_process")
public class ProteinUpdateProcess extends UpdateProcessImpl<PersistentProteinEvent>{

    Collection<IntactUpdateError> updateErrors = new ArrayList<IntactUpdateError>();

    public ProteinUpdateProcess(){
        super();
        setDate(new Date(System.currentTimeMillis()));
        setUserStamp("PROTEIN_UPDATE_RUNNER");
    }

    public ProteinUpdateProcess(Date date, String userStamp){
        super(date, userStamp);
    }

    @OneToMany(mappedBy="parent", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
    public Collection<IntactUpdateError> getUpdateErrors() {
        return updateErrors;
    }

    public void setUpdateErrors(Collection<IntactUpdateError> updateErrors) {
        this.updateErrors = updateErrors;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final ProteinUpdateProcess process = ( ProteinUpdateProcess ) o;

        if (!CollectionUtils.isEqualCollection(updateErrors, process.getUpdateErrors())){
            return false;
        }

        return true;
    }
}
