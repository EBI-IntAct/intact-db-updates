package uk.ac.ebi.intact.update.persistence.proteinupdate;

import uk.ac.ebi.intact.update.model.protein.update.UpdatedCrossReference;
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

public interface CrossReferenceDao<T extends UpdatedCrossReference> extends UpdateBaseDao<T>, Serializable {

    public List<UpdatedCrossReference> getCrossReferencesByIntactXrefAc(String intactXRefAc);
    public List<UpdatedCrossReference> getCrossReferencesByDatabaseAc(String databaseAc);
    public List<UpdatedCrossReference> getCrossReferencesByQualifierAc(String qualifierAc);
    public List<UpdatedCrossReference> getCrossReferencesByIdentifier(String primaryAc);
    public List<UpdatedCrossReference> getCrossReferencesByProteinId(long proteinId);
}
