package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.model.Component;
import uk.ac.ebi.intact.model.Protein;

/**
 * Event for deleted components
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>01/03/11</pre>
 */

public class DeletedComponentEvent extends ProteinEvent{

    private Component component;

    public DeletedComponentEvent(Object source, DataContext dataContext, Protein protein, Component component) {
        super(source, dataContext, protein);
        this.component = component;
    }

    public Component getComponent() {
        return component;
    }
}
