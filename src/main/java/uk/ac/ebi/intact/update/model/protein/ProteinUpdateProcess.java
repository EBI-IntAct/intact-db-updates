package uk.ac.ebi.intact.update.model.protein;

import uk.ac.ebi.intact.update.model.UpdateProcessImpl;
import uk.ac.ebi.intact.update.model.protein.update.events.PersistentProteinEvent;

import javax.persistence.Entity;
import javax.persistence.Table;
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

    public ProteinUpdateProcess(){
        super();
        setDate(new Date(System.currentTimeMillis()));
        setUserStamp("PROTEIN_UPDATE_RUNNER");
    }

    public ProteinUpdateProcess(Date date, String userStamp){
        super(date, userStamp);
    }
}
