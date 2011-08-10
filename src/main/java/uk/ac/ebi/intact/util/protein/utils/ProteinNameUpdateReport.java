package uk.ac.ebi.intact.util.protein.utils;

/**
 * This class contains a summary of what have been updated (shortlabel and.or fullname)
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>29/06/11</pre>
 */

public class ProteinNameUpdateReport {

    private String protein;
    private String shortLabel;
    private String fullName;

    public ProteinNameUpdateReport(String proteinAc, String shortLabel, String fullName){
         this.shortLabel = shortLabel;
        this.fullName = fullName;
        protein = proteinAc;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public String getFullName() {
        return fullName;
    }

    public String getProtein() {
        return protein;
    }
}
