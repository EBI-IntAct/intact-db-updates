package uk.ac.ebi.intact.dbupdate.prot.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateError;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateErrorEvent;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.util.ProteinUtils;

import java.util.Collection;
import java.util.Set;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03-Nov-2010</pre>
 */

public class UniprotUpdateFilterListener extends AbstractProteinUpdateProcessorListener {

    private static final Log log = LogFactory.getLog( UniprotUpdateFilterListener.class );

    @Override
    public void onPreProcess(ProteinEvent evt) throws ProcessorException {

        Protein protein = evt.getProtein();

        // filter 'no-uniprot-update' proteins
        if (!ProteinUtils.isFromUniprot(protein)) {

            if (evt.getSource() instanceof ProteinUpdateProcessor) {
                final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                updateProcessor.fireNonUniprotProteinFound(evt);
            }

            if (log.isTraceEnabled()) log.debug("Request finalization, as this protein cannot be updated using UniProt (no-uniprot-update)");
            ((ProteinProcessor)evt.getSource()).finalizeAfterCurrentPhase();
        }
        else {
            Set<InteractorXref> uniprotIdentities = ProteinTools.getDistinctUniprotIdentities(protein);

            // no uniprot identity, it is a protein not from uniprot
            if (uniprotIdentities.size() == 0){
                if (evt.getSource() instanceof ProteinUpdateProcessor) {
                    final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                    updateProcessor.fireNonUniprotProteinFound(evt);
                }

                if (log.isTraceEnabled()) log.debug("Request finalization, as this protein cannot be updated using UniProt (no uniprot identity)");
                ((ProteinProcessor)evt.getSource()).finalizeAfterCurrentPhase();
            }
            else if (uniprotIdentities.size() > 1){
                if (evt.getSource() instanceof ProteinUpdateProcessor) {
                    final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                    updateProcessor.fireonProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), xRefToString(uniprotIdentities), UpdateError.multi_uniprot_identities));
                }

                if (log.isTraceEnabled()) log.debug("Request finalization, as this protein cannot be updated using UniProt (several distinct uniprot identities)");
                ((ProteinProcessor)evt.getSource()).finalizeAfterCurrentPhase();
            }
            else {
                InteractorXref uniprotIdentity = uniprotIdentities.iterator().next();

                // the primaryId is never null in intact and is never empty
                String uniprotAc = uniprotIdentity.getPrimaryId().trim();
                evt.setUniprotIdentity(uniprotAc);
            }
        }
    }

    private String xRefToString(Collection<InteractorXref> refs){
        StringBuilder sb = new StringBuilder();

        for (InteractorXref ref : refs) {

            String qual = (ref.getCvXrefQualifier() != null)? "("+ref.getCvXrefQualifier().getShortLabel()+")" : "";

            sb.append(ref.getCvDatabase().getShortLabel()+":"+ref.getPrimaryId()+qual);
            sb.append(", ");
        }

        return sb.toString();
    }
}
