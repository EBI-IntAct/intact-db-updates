package uk.ac.ebi.intact.util.protein.utils;

import uk.ac.ebi.intact.model.Alias;
import uk.ac.ebi.intact.model.InteractorAlias;
import uk.ac.ebi.intact.model.Protein;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Contains a summary of the aliases update
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>29/06/11</pre>
 */

public class AliasUpdateReport {

    private String protein;
    private Collection<String> addedAliases = new ArrayList<String>();
    private Collection<String> removedAliases = new ArrayList<String>();

    public AliasUpdateReport(Protein protein, Collection<InteractorAlias> addedAliases, Collection<InteractorAlias> removedAliases){
        this.protein = protein.getAc();

        for (Alias alias : addedAliases){
            String aliasType = "";
            if (alias.getCvAliasType() != null && alias.getCvAliasType().getShortLabel() != null){
                aliasType = alias.getCvAliasType().getShortLabel();
            }

            this.addedAliases.add(aliasType+ " : " +alias.getName());
        }
        for (Alias alias : removedAliases){
            String aliasType = "";
            if (alias.getCvAliasType() != null && alias.getCvAliasType().getShortLabel() != null){
                aliasType = alias.getCvAliasType().getShortLabel();
            }

            this.removedAliases.add(aliasType+ " : " +alias.getName());
        }
    }

    public String getProtein() {
        return protein;
    }

    public Collection<String> getAddedAliases() {
        return addedAliases;
    }

    public Collection<String> getRemovedAliases() {
        return removedAliases;
    }
}
