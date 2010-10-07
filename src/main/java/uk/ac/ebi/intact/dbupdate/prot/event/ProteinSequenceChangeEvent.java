package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.model.Protein;

/**
 * Event fired when a protein sequence is changed
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class ProteinSequenceChangeEvent extends ProteinEvent {

    /**
     * previous sequence of the protein
     */
    private String oldSequence;

    /**
     * A protein update event
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public ProteinSequenceChangeEvent(Object source, DataContext dataContext, Protein protein, String oldSequence) {
        super(source, dataContext, protein);
        this.oldSequence = oldSequence;
    }

    public String getOldSequence() {
        return oldSequence;
    }
}
