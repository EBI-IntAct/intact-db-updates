package uk.ac.ebi.intact.dbupdate.prot.errors;

/**
 * Interface for errors having an Intact accession
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>08/08/11</pre>
 */

public interface IntactUpdateError extends ProteinUpdateError{

    public String getProteinAc();
}
