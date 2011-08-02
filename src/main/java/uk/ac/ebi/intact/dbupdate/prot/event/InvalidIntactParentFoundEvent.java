package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.model.Protein;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>06-Dec-2010</pre>
 */

public class InvalidIntactParentFoundEvent extends ProteinEvent{

    private String newParentsAc;
    private String oldParentAc;

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public InvalidIntactParentFoundEvent(Object source, DataContext context, Protein protein, String uniprot, String oldParentsAcs, String parents) {
        super(source, context, protein);
        setUniprotIdentity(uniprot);
        this.newParentsAc = parents;
        this.oldParentAc = oldParentsAcs;
    }


    public String getNewParentAc() {
        return newParentsAc;
    }

    public String getOldParentAc() {
        return oldParentAc;
    }
}
