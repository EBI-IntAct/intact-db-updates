package uk.ac.ebi.intact.dbupdate.prot.actions;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.RangeUpdateReport;
import uk.ac.ebi.intact.dbupdate.prot.event.InvalidRangeEvent;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.RangeChecker;
import uk.ac.ebi.intact.model.Protein;

/**
 * Interface to implement for classes for updating and fixing ranges when the protein sequence has been updated
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>14-Dec-2010</pre>
 */

public interface RangeFixer {

    /**
     * Fix invalid ranges from the beginning
     * @param evt
     */
    public void fixInvalidRanges(InvalidRangeEvent evt, ProteinUpdateProcessor processor);

    /**
     * Fix valid ranges impossible to shift with the new uniprot sequence
     * @param evt
     */
    public void fixOutOfDateRanges(InvalidRangeEvent evt, ProteinUpdateProcessor processor);

    /**
     * Fix invalid ranges and out of date ranges
     * @param protein : protein having range conflicts
     * @param context
     * @param uniprotAc : the uniprot ac
     * @param oldSequence : the previous sequence
     * @param report : the range update report
     * @param fixedProtein : the proteins the ranges can be remapped to
     * @param processor
     * @param fixOutOfDateRanges : enable or not to fix the out of date ranges
     */
    public void processInvalidRanges(Protein protein, DataContext context, String uniprotAc, String oldSequence, RangeUpdateReport report, ProteinTranscript fixedProtein, ProteinUpdateProcessor processor, boolean fixOutOfDateRanges);

    /**
     * Update ranges attached to a protein
     * @param protein
     * @param uniprotSequence
     * @param processor
     * @param datacontext
     * @return
     */
    public RangeUpdateReport updateRanges(Protein protein, String uniprotSequence, ProteinUpdateProcessor processor, DataContext datacontext);

    public RangeUpdateReport updateOnlyInvalidRanges(Protein protein, ProteinUpdateProcessor processor, DataContext datacontext);

    public RangeChecker getChecker();

    public void setChecker(RangeChecker checker);
}
