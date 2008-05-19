package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.context.DataContext;

import java.io.Serializable;

/**
 * TODO comment this
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public interface ProteinProcessorEvent extends Serializable {
    void requestFinalization();

    boolean isFinalizationRequested();

    DataContext getDataContext();
}
