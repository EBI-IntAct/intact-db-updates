package uk.ac.ebi.intact.update.persistence.proteinupdate;

import uk.ac.ebi.intact.update.model.protein.update.UpdatedAlias;
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

public interface AliasDao<T extends UpdatedAlias> extends UpdateBaseDao<T>, Serializable {

    public List<UpdatedAlias> getAliasesByAliasTypeAc(String aliastTypeAc);
    public List<UpdatedAlias> getAliasesByName(String name);
    public List<UpdatedAlias> getAliasesByNameLike(String name);
    public List<UpdatedAlias> getAliasByIntactAliasAc(String intactAliasAc);
    public List<UpdatedAlias> getAliasesByProteinId(long proteinId);
}
