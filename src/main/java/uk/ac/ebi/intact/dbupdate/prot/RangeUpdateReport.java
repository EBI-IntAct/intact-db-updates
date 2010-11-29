package uk.ac.ebi.intact.dbupdate.prot;

import uk.ac.ebi.intact.dbupdate.prot.rangefix.InvalidRange;
import uk.ac.ebi.intact.model.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>29-Nov-2010</pre>
 */

public class RangeUpdateReport {

    private Map<Component, Collection<InvalidRange>> invalidComponents;

    public RangeUpdateReport (){
         invalidComponents = new HashMap<Component, Collection<InvalidRange>>();
    }

    public Map<Component, Collection<InvalidRange>> getInvalidComponents() {
        return invalidComponents;
    }
}
