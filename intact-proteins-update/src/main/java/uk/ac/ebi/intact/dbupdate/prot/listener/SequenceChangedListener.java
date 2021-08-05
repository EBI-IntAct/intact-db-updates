package uk.ac.ebi.intact.dbupdate.prot.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinSequenceChangeEvent;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.ProteinImpl;

/**
 * Checks the protein sequence updates in order to assess the change and add additional
 * information about the change (e.g. Add cautions for proteins and interactions if the
 * sequence changes considerably).
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class SequenceChangedListener extends AbstractProteinUpdateProcessorListener {

    private static final Log log = LogFactory.getLog( SequenceChangedListener.class );

    private double conservationThreshold = 0.35;

    public SequenceChangedListener() {
    }

    public SequenceChangedListener(double conservationThreshold) {
        this.conservationThreshold = conservationThreshold;
    }

    @Override
    public void onProteinSequenceChanged(ProteinSequenceChangeEvent evt) throws ProcessorException {
        String oldSeq = evt.getOldSequence();
        Protein protein = evt.getProtein();

        double relativeConservation = 0;

        if (oldSeq != null){
            relativeConservation = evt.getRelativeConservation();

            // if the sequences are considerably different, create a caution for the protein and the interactions
            if ( relativeConservation <= conservationThreshold) {

                if (evt.getSource() instanceof ProteinUpdateProcessor){
                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                    processor.fireOnProteinSequenceCaution(evt);
                }
            }
            evt.getDataContext().getDaoFactory().getProteinDao().update((ProteinImpl) protein);
        }

        if (log.isDebugEnabled()) {
            log.debug("After sequence update, the relative sequence conservation is " + relativeConservation);
        }
    }
}