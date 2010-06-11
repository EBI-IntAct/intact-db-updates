package uk.ac.ebi.intact.dbupdate.dataset.proteinselection;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02-Jun-2010</pre>
 */

public class ProteinSelectorException extends Exception{
    public ProteinSelectorException() {
        super();
    }

    public ProteinSelectorException(String message) {
        super(message);
    }

    public ProteinSelectorException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProteinSelectorException(Throwable cause) {
        super(cause);
    }
}

