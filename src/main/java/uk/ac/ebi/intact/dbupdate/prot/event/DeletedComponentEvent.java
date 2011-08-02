package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.model.Component;
import uk.ac.ebi.intact.model.Protein;

import java.util.Collection;
import java.util.Collections;

/**
 * Event for deleted components
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>01/03/11</pre>
 */

public class DeletedComponentEvent extends ProteinEvent{

    private Collection<Component> deletedComponents;

    public DeletedComponentEvent(Object source, DataContext dataContext, Protein protein, String uniprot, Collection<Component> deletedComponents) {
        super(source, dataContext, protein);
        this.deletedComponents = deletedComponents;
        setUniprotIdentity(uniprot);
    }

    public Collection<Component> getDeletedComponents() {
        if (deletedComponents == null){
            return Collections.EMPTY_LIST;
        }
        return deletedComponents;
    }
}
