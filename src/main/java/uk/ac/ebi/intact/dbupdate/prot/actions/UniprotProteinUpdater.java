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
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>14-Dec-2010</pre>
 */

public interface UniprotProteinUpdater {

     public void createOrUpdateProtein( UpdateCaseEvent evt) throws ProteinServiceException;

    public void createOrUpdateIsoform( UpdateCaseEvent caseEvent, Protein masterProtein) throws ProteinServiceException;

    public void createOrUpdateFeatureChain( UpdateCaseEvent caseEvent, Protein masterProtein) throws ProteinServiceException;

    public BioSourceService getBioSourceService();

    public void setBioSourceService(BioSourceService bioSourceService);

    public OutOfDateParticipantFixer getParticipantFixer();

    public void setParticipantFixer(OutOfDateParticipantFixer participantFixer);

    public RangeFixer getRangeFixer();

    public void setRangeFixer(RangeFixer rangeFixer);
}
