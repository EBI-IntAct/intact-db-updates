package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.model.Component;
import uk.ac.ebi.intact.model.Protein;

import java.util.ArrayList;
import java.util.Collection;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>29-Oct-2010</pre>
 */

public class OutOfDateParticipantFoundEvent extends ProteinEvent{
    Collection<Component> componentsToFix = new ArrayList<Component>();

    public OutOfDateParticipantFoundEvent(Object source, DataContext dataContext, Protein protein, Collection<Component> components) {
        super(source, dataContext, protein);
        this.componentsToFix = components;
    }

    public OutOfDateParticipantFoundEvent(Object source, DataContext dataContext, Protein protein) {
        super(source, dataContext, protein);
    }

    public Collection<Component> getComponentsToFix() {
        return componentsToFix;
    }

    public void addComponentToFix(Component component){
        this.componentsToFix.add(component);
    }
}
