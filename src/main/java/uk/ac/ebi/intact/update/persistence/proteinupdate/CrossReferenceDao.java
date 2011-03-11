package uk.ac.ebi.intact.update.persistence.proteinupdate;

import uk.ac.ebi.intact.update.model.protein.update.protein.CrossReference;
import uk.ac.ebi.intact.update.persistence.UpdateBaseDao;

import java.io.Serializable;
import java.util.List;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02-Dec-2010</pre>
 */

public interface CrossReferenceDao<T extends CrossReference> extends UpdateBaseDao<T>, Serializable {

    public List<CrossReference> getCrossReferencesByIntactXrefAc(String intactXRefAc);
    public List<CrossReference> getCrossReferencesByDatabaseAc(String databaseAc);
    public List<CrossReference> getCrossReferencesByQualifierAc(String qualifierAc);
    public List<CrossReference> getCrossReferencesByIdentifier(String primaryAc);
    public List<CrossReference> getCrossReferencesByProteinId(long proteinId);
}
