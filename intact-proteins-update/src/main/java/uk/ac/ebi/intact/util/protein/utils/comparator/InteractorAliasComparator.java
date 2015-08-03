package uk.ac.ebi.intact.util.protein.utils.comparator;

import uk.ac.ebi.intact.model.CvAliasType;
import uk.ac.ebi.intact.model.InteractorAlias;

import java.util.Comparator;

/**
 * Comparator for interactor aliases
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>22/11/11</pre>
 */

public class InteractorAliasComparator implements Comparator<InteractorAlias>{
    @Override
    public int compare(InteractorAlias o1, InteractorAlias o2) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        if (o1.getCvAliasType() == null && o2.getCvAliasType() != null){
            return AFTER;
        }
        else if (o1.getCvAliasType() != null && o2.getCvAliasType() == null){
            return BEFORE;
        }
        else if (o1.getCvAliasType() == null && o2.getCvAliasType() == null){
            if (o1.getName() == null && o2.getName() != null){
                return AFTER;
            }
            else if (o1.getName() != null && o2.getName() == null){
                return BEFORE;
            }
            else if (o1.getName() == null && o2.getName() == null){
                return EQUAL;
            }
            else {
                return o1.getName().compareTo(o2.getName());
            }
        }
        else {
            String identifier1 = o1.getCvAliasType().getIdentifier();
            String identifier2 = o2.getCvAliasType().getIdentifier();

            if (identifier1.equalsIgnoreCase(identifier2)){
                if (o1.getName() == null && o2.getName() != null){
                    return AFTER;
                }
                else if (o1.getName() != null && o2.getName() == null){
                    return BEFORE;
                }
                else if (o1.getName() == null && o2.getName() == null){
                    return EQUAL;
                }
                else {
                    return o1.getName().compareTo(o2.getName());
                }
            }
            else if (CvAliasType.GENE_NAME_MI_REF.equalsIgnoreCase(identifier1)){
                return BEFORE;
            }
            else if (CvAliasType.GENE_NAME_MI_REF.equalsIgnoreCase(identifier2)){
                return AFTER;
            }
            else if (CvAliasType.GENE_NAME_SYNONYM_MI_REF.equalsIgnoreCase(identifier1)){
                return BEFORE;
            }
            else if (CvAliasType.GENE_NAME_SYNONYM_MI_REF.equalsIgnoreCase(identifier2)){
                return AFTER;
            }
            else if (CvAliasType.ORF_NAME_MI_REF.equalsIgnoreCase(identifier1)){
                return BEFORE;
            }
            else if (CvAliasType.ORF_NAME_MI_REF.equalsIgnoreCase(identifier2)){
                return AFTER;
            }
            else if (CvAliasType.LOCUS_NAME_MI_REF.equalsIgnoreCase(identifier1)){
                return BEFORE;
            }
            else if (CvAliasType.LOCUS_NAME_MI_REF.equalsIgnoreCase(identifier2)){
                return AFTER;
            }
            else if (CvAliasType.ISOFORM_SYNONYM_MI_REF.equalsIgnoreCase(identifier1)){
                return BEFORE;
            }
            else if (CvAliasType.ISOFORM_SYNONYM_MI_REF.equalsIgnoreCase(identifier2)){
                return AFTER;
            }
            else {
                return identifier1.compareTo(identifier2);
            }
        }
    }
}
