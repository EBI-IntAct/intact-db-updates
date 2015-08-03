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

    private String newSequence;

    private String uniprotCrc64;

    double relativeConservation;

    /**
     * A protein update event
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public ProteinSequenceChangeEvent(Object source, DataContext dataContext, Protein protein, String uniprot, String oldSequence, String newSequence, String uniprotCrc64, double relativeConservation) {
        super(source, dataContext, protein);
        this.oldSequence = oldSequence;
        this.newSequence = newSequence;
        this.uniprotCrc64 = uniprotCrc64;
        this.relativeConservation = relativeConservation;
        setUniprotIdentity(uniprot);
    }

    public String getOldSequence() {
        return oldSequence;
    }

    public String getNewSequence() {
        return newSequence;
    }

    public String getUniprotCrc64() {
        return uniprotCrc64;
    }

    public double getRelativeConservation() {
        return relativeConservation;
    }
}
