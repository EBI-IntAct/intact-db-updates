package uk.ac.ebi.intact.update.model.protein;

import org.apache.commons.collections.CollectionUtils;
import uk.ac.ebi.intact.update.model.UpdateName;
import uk.ac.ebi.intact.update.model.UpdateProcessImpl;
import uk.ac.ebi.intact.update.model.protein.update.events.PersistentProteinEvent;
import uk.ac.ebi.intact.update.model.protein.update.events.range.PersistentUpdatedRange;

import javax.persistence.*;
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
@Table(name = "ia_update_process")
public class ProteinUpdateProcess extends UpdateProcessImpl<PersistentProteinEvent>{

    private Collection<PersistentUpdatedRange> rangeUpdates = new ArrayList<PersistentUpdatedRange>();

    public ProteinUpdateProcess(){
        super();
        setDate(new Date(System.currentTimeMillis()));
        setUpdateName(UpdateName.protein_update);
        setUserStamp("PROTEIN_UPDATE_RUNNER");
    }

    public ProteinUpdateProcess(Date date, String userStamp){
        super(date, userStamp, UpdateName.protein_update);
    }

    @OneToMany(mappedBy="parent", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
    public Collection<PersistentUpdatedRange> getRangeUpdates() {
        return rangeUpdates;
    }

    public void setRangeUpdates(Collection<PersistentUpdatedRange> rangeUpdates) {
        this.rangeUpdates = rangeUpdates;
    }

    public boolean addRangeUpdate(PersistentUpdatedRange up){
        if (rangeUpdates.add(up)){
            up.setParent(this);
            return true;
        }

        return false;
    }

    public boolean removeRangeUpdate(PersistentUpdatedRange up){
        if (rangeUpdates.remove(up)){
            up.setParent(null);
            return true;
        }

        return false;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final ProteinUpdateProcess process = (ProteinUpdateProcess) o;

        return CollectionUtils.isEqualCollection(rangeUpdates, process.getRangeUpdates());
    }
}
