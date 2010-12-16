package uk.ac.ebi.intact.update.persistence.proteinupdate;

import uk.ac.ebi.intact.update.model.proteinupdate.protein.IntactProtein;
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

public interface IntactProteinDao<T extends IntactProtein> extends UpdateBaseDao<T>, Serializable {

    public List<IntactProtein> getProteinsByIntactProteinAc(String intactProteinAc);
    public List<IntactProtein> getProteinsBySequence(String sequence);
    public List<IntactProtein> getProteinsByUniprotAc(String uniprotAc);
    public List<IntactProtein> getProteinsWithRangeUpdatesFor();
}
