package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;

import java.util.Collection;

/**
 * Events for dead proteins
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28/06/11</pre>
 */

public class DeadUniprotEvent extends ProteinEvent {

    private Collection<InteractorXref> deletedXrefs;
    private InteractorXref uniprotIdentityXref;

    public DeadUniprotEvent(Object source, DataContext dataContext, Protein protein) {
        super(source, dataContext, protein);
    }

    public DeadUniprotEvent(Object source, DataContext dataContext, Protein protein, UniprotProtein uniprotProtein) {
        super(source, dataContext, protein, uniprotProtein);
    }

    public DeadUniprotEvent(Object source, DataContext dataContext, Protein protein, String message) {
        super(source, dataContext, protein, message);
    }

    public DeadUniprotEvent(Object source, DataContext dataContext, Protein protein, UniprotProtein uniprotProtein, String message) {
        super(source, dataContext, protein, uniprotProtein, message);
    }

    public InteractorXref getUniprotIdentityXref() {
        return uniprotIdentityXref;
    }

    public Collection<InteractorXref> getDeletedXrefs() {
        return deletedXrefs;
    }

    public void setDeletedXrefs(Collection<InteractorXref> deletedXrefs) {
        this.deletedXrefs = deletedXrefs;
    }

    public void setUniprotIdentityXref(InteractorXref uniprotIdentityXref) {
        this.uniprotIdentityXref = uniprotIdentityXref;
    }
}
