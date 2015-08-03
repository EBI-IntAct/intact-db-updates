package uk.ac.ebi.intact.util;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;
import uk.ac.ebi.intact.uniprot.service.UniprotService;
import uk.ac.ebi.intact.util.biosource.BioSourceService;
import uk.ac.ebi.intact.util.protein.ProteinServiceException;

import java.util.Collection;

/**
 * This service allows to create an intact protein up to date with uniprot protein
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>10/06/11</pre>
 */

public interface ProteinService {

    public BioSourceService getBiosourceService();

    public UniprotService getUniprotService();

    public Collection<Protein> getMasterProteinsByUniprotAc(String uniprotAc) throws ProteinServiceException;

    public Protein getMasterProteinFromUniprotEntry(UniprotProtein uniprot) throws ProteinServiceException;

    public Protein getUniqueMasterProteinForUniprotAc(String uniprotAc) throws ProteinServiceException;

    public Collection<Protein> getProteinTranscriptsByUniprotAc(String uniprotAc, String intactParentAc) throws ProteinServiceException;

    public Protein getProteinTranscriptFromUniprotEntry(UniprotProteinTranscript uniprot, String intactParentAc) throws ProteinServiceException;

    public Protein getUniqueProteinTranscriptForUniprotAc(String uniprotAc, String intactParentAc) throws ProteinServiceException;
}
