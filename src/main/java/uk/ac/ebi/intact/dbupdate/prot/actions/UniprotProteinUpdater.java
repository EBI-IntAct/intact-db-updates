package uk.ac.ebi.intact.dbupdate.prot.actions;

import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.OutOfDateParticipantFixerImpl;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.RangeFixerImpl;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.util.biosource.BioSourceService;
import uk.ac.ebi.intact.util.protein.ProteinServiceException;

/**
 * The interface to implement for classes updating a protein against a single uniprot entry
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>14-Dec-2010</pre>
 */

public interface UniprotProteinUpdater {

    /**
     * Create of update the master proteins
     * @param evt : contains the uniprot entry and the list of proteins to update
     * @throws ProteinServiceException
     */
     public void createOrUpdateProtein( UpdateCaseEvent evt) throws ProteinServiceException;

    /**
     * Create of update the isoforms
     * @param caseEvent : contains the uniprot entry and the list of proteins to update
     * @param masterProtein : the master protein which is the parent of the isoforms
     * @throws ProteinServiceException
     */
    public void createOrUpdateIsoform( UpdateCaseEvent caseEvent, Protein masterProtein) ;

    /**
     * Create of update the feature chains
     * @param caseEvent : contains the uniprot entry and the list of proteins to update
     * @param masterProtein : the master protein which is the parent of the isoforms
     * @throws ProteinServiceException
     */
    public void createOrUpdateFeatureChain( UpdateCaseEvent caseEvent, Protein masterProtein);

    public BioSourceService getBioSourceService();

    public void setBioSourceService(BioSourceService bioSourceService);

    public OutOfDateParticipantFixer getParticipantFixer();

    public void setParticipantFixer(OutOfDateParticipantFixer participantFixer);

    public RangeFixer getRangeFixer();

    public void setRangeFixer(RangeFixer rangeFixer);
}
