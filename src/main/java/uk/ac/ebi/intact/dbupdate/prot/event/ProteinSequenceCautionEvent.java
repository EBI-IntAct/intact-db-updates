package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.model.Protein;

/**
 * Event when protein sequence has been dramatically changed
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28/02/11</pre>
 */

public class ProteinSequenceCautionEvent  extends ProteinEvent{

     /**
     * previous sequence of the protein
     */
    private String oldSequence;

    private String newSequence;

    double relativeConservation;

    /**
     * A protein update event
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public ProteinSequenceCautionEvent(Object source, DataContext dataContext, Protein protein, String oldSequence, String newSequence, double relativeConservation ) {
        super(source, dataContext, protein);
        this.oldSequence = oldSequence;
        this.newSequence = newSequence;
        this.relativeConservation = relativeConservation;
    }

    public String getOldSequence() {
        return oldSequence;
    }

    public String getNewSequence() {
        return newSequence;
    }

    public double getRelativeConservation() {
        return relativeConservation;
    }
}
