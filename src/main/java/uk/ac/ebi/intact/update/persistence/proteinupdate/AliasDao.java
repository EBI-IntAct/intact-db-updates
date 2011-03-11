package uk.ac.ebi.intact.update.persistence.proteinupdate;

import uk.ac.ebi.intact.update.model.protein.update.protein.Alias;
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

public interface AliasDao<T extends Alias> extends UpdateBaseDao<T>, Serializable {

    public List<Alias> getAliasesByAliasTypeAc(String aliastTypeAc);
    public List<Alias> getAliasesByName(String name);
    public List<Alias> getAliasesByNameLike(String name);
    public List<Alias> getAliasByIntactAliasAc(String intactAliasAc);
    public List<Alias> getAliasesByProteinId(long proteinId);
}
