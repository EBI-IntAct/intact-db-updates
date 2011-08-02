package uk.ac.ebi.intact.dbupdate.prot.actions;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.model.Protein;

import java.util.List;

/**
 * This interface can be implemented by classes checking and updating intact parent cross references
 * (isoforms and feature chains having isoform-parent and chain-parent xrefs)
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>13-Dec-2010</pre>
 */

public interface IntactTranscriptParentUpdater {

    /**
     *
     * @param evt : contains the protein transcript to check
     * @param transcripts : list of transcripts which need to be reviewed because doesn't have valid parent xref
     * @return true if the protein transcript has a single parent xref (or no parent xrefs because can be updated later),
     * false if the protein transcript has several parent xrefs or has a parent xref which is not pointing to any proteins in the database
     */
    public boolean checkConsistencyProteinTranscript(ProteinEvent evt, List<Protein> transcripts);

    /**
     *
     * @param evt : contains the proteins matching a single uniprot entry
     * @return  the list of protein transcripts without intact parent xrefs.
     * Update the parent xrefs when it is necessary
     */
    public List<Protein> checkConsistencyOfAllTranscripts(UpdateCaseEvent evt);

    /**
     * Create an intact parent xref for a list of protein transcripts matching a single uniprot entry. Remove all out of date parent xRef
     * @param transcripts
     * @param masterProtein
     * @param uniprotAc ac
     * @param context
     * @param processor
     */
    public void createParentXRefs(List<Protein> transcripts, Protein masterProtein, String uniprotAc, DataContext context, ProteinUpdateProcessor processor);
}
