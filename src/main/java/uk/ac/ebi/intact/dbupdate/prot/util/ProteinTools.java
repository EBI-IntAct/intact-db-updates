package uk.ac.ebi.intact.dbupdate.prot.util;

import uk.ac.ebi.intact.model.Component;
import uk.ac.ebi.intact.model.Protein;

import java.util.Collection;

/**
 * TODO comment this
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class ProteinTools {

    private ProteinTools() {}

    public static void moveInteractionsBetweenProteins(Protein destinationProtein, Collection<? extends Protein> sourceProteins) {
        for (Protein sourceProtein : sourceProteins) {
            moveInteractionsBetweenProteins(destinationProtein, sourceProtein);
        }
    }

    public static void moveInteractionsBetweenProteins(Protein destinationProtein, Protein sourceProtein) {
        for (Component component : sourceProtein.getActiveInstances()) {
            component.setInteractor(destinationProtein);
        }
        destinationProtein.getActiveInstances().addAll(sourceProtein.getActiveInstances());
        sourceProtein.getActiveInstances().clear();
    }
}
