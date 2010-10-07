package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;

import java.io.Serializable;

/**
 * The interface to implement for each Event involved in the protein update
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public interface ProteinProcessorEvent extends Serializable {

    DataContext getDataContext();
}
