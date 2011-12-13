package uk.ac.ebi.intact.dbupdate.cv.utils;

import uk.ac.ebi.intact.model.CvDagObject;
import uk.ac.ebi.intact.model.CvObjectXref;
import uk.ac.ebi.intact.model.util.XrefUtils;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TComparator for Cv Parents
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>13/12/11</pre>
 */

public class CvParentComparator implements Comparator<CvDagObject> {

    private String dbIdentifier;
    private Pattern dbPattern;

    @Override
    public int compare(CvDagObject o1, CvDagObject o2) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        String identityValue1 = null;
        String identityValue2 = null;
        // we want a simple comparison of identifiers
        if (dbIdentifier == null || dbPattern == null){
            identityValue1 = o1.getIdentifier();
            identityValue2 = o2.getIdentifier();
        }
        else {
            CvObjectXref identity1 = XrefUtils.getIdentityXref(o1, dbIdentifier);
            // this parent cannot be updated because is not from the same ontology
            if (identity1 == null){
                Matcher matcher = dbPattern.matcher(o1.getIdentifier());

                if (matcher.find() && matcher.group().equalsIgnoreCase(o1.getIdentifier())){
                    identityValue1 = o1.getIdentifier();
                }
            }
            else {
                identityValue1 = identity1.getPrimaryId();
            }

            CvObjectXref identity2 = XrefUtils.getIdentityXref(o2, dbIdentifier);
            // this parent cannot be updated because is not from the same ontology
            if (identity2 == null){
                Matcher matcher = dbPattern.matcher(o2.getIdentifier());

                if (matcher.find() && matcher.group().equalsIgnoreCase(o2.getIdentifier())){
                    identityValue2 = o2.getIdentifier();
                }
            }
            else {
                identityValue2 = identity2.getPrimaryId();
            }
        }

        if (identityValue1 != null && identityValue2 != null){
            return identityValue1.compareTo(identityValue2);
        }
        else if (identityValue1 == null && identityValue2 != null){
            return AFTER;
        }
        else if (identityValue1 != null && identityValue2 == null){
            return BEFORE;
        }
        else {
            return EQUAL;
        }
    }

    public String getDbIdentifier() {
        return dbIdentifier;
    }

    public void setDbIdentifier(String db) {
        this.dbIdentifier = db;
    }

    public Pattern getDbPattern() {
        return dbPattern;
    }

    public void setDbPattern(Pattern dbPattern) {
        this.dbPattern = dbPattern;
    }
}