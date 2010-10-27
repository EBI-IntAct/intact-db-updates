package ebi.ac.uk.intact.update.model.proteinsequence;

import ebi.ac.uk.intact.update.model.HibernatePersistentImpl;

import javax.persistence.*;
import java.util.Date;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-Oct-2010</pre>
 */
@Entity
@Table(name = "ia_updated_protein_sequence")
public class UpdatedProteinSequence extends HibernatePersistentImpl {

    private String oldSequence;
    private String newSequence;
    private String message;
    private String protein_ac;

    public UpdatedProteinSequence(){
        super();
        this.oldSequence = null;
        this.newSequence = null;
        this.protein_ac = null;
    }

    public UpdatedProteinSequence(String proteinAc, String oldSequence, String newSequence){
        super();
        this.protein_ac = proteinAc;
        this.oldSequence = oldSequence;
        this.newSequence = newSequence;
        setCreated(new Date(System.currentTimeMillis()));
    }

    @Lob
    @Column(name = "old_sequence", nullable = true)
    public String getOldSequence() {
        return oldSequence;
    }

    public void setOldSequence(String oldSequence) {
        this.oldSequence = oldSequence;
    }

    @Lob
    @Column(name = "new_sequence", nullable = false)
    public String getNewSequence() {
        return newSequence;
    }

    public void setNewSequence(String newSequence) {
        this.newSequence = newSequence;
    }

    @Column(name = "protein_ac", nullable = false)
    public String getProtein_ac() {
        return protein_ac;
    }

    public void setProtein_ac(String protein_ac) {
        this.protein_ac = protein_ac;
    }

    @Column(name = "message", nullable = true)
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
