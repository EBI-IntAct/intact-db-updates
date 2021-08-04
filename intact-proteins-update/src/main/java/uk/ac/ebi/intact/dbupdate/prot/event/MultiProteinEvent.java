package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.model.Protein;

import java.util.Collection;
import java.util.EventObject;

/**
 * Super class for the DuplicatesFoundEvent, contains a list of duplicated proteins
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class MultiProteinEvent extends EventObject implements ProteinProcessorEvent {

    private Collection<Protein> proteins;
    private DataContext dataContext;

    private Protein referenceProtein;

    /**
     * An event involving a list of proteins.
     */
    public MultiProteinEvent(Object source, DataContext dataContext, Collection<Protein> proteins) {
        super(source);
        this.proteins = proteins;
        this.dataContext = dataContext;
    }

    public DataContext getDataContext() {
        return dataContext;
    }

    public Collection<Protein> getProteins() {
        return proteins;
    }

    public Protein getReferenceProtein() {
        return referenceProtein;
    }

    public void setReferenceProtein(Protein referenceProtein) {
        this.referenceProtein = referenceProtein;
    }
}