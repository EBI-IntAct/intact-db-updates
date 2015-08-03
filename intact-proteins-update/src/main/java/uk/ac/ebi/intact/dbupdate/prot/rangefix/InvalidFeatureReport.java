package uk.ac.ebi.intact.dbupdate.prot.rangefix;

import uk.ac.ebi.intact.model.Annotation;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02-Dec-2010</pre>
 */

public class InvalidFeatureReport {

    private String rangePositions;
    private String uniprotAc;
    private int sequenceVersion;
    public static String rangeAcDelimiterEnd = "]";
    public static String rangeAcDelimiterStart = "[";
    private String rangeAc;

    public InvalidFeatureReport(){
        this.rangePositions = null;
        this.sequenceVersion = -1;
        this.rangeAc = null;
        this.uniprotAc = null;
    }

    public String getRangePositions() {
        return rangePositions;
    }

    public void setRangePositions(Annotation rangePositions) {
        if (rangePositions != null){
            String positions = rangePositions.getAnnotationText();

            if (positions != null){
                if (positions.contains(rangeAcDelimiterEnd)){
                    this.rangePositions = positions.substring(Math.min(positions.length(), positions.indexOf(rangeAcDelimiterEnd) + 1));
                }
            }
        }
    }

    public int getSequenceVersion() {
        return sequenceVersion;
    }

    public void setSequenceVersion(Annotation sequenceVersion) {
        if (sequenceVersion != null){
            String sequenceString = sequenceVersion.getAnnotationText();

            if (sequenceString != null){
                if (sequenceString.contains(rangeAcDelimiterEnd)){
                    String seqVersion = sequenceString.substring(Math.min(sequenceString.length() - 1, sequenceString.indexOf(rangeAcDelimiterEnd) + 1));

                    if (seqVersion != null && seqVersion.trim().length() > 0){
                        if (seqVersion.contains(",")){
                            int indexOfSeparator = seqVersion.indexOf(",");

                            this.uniprotAc = seqVersion.substring(0, indexOfSeparator);
                            this.sequenceVersion = Integer.parseInt(seqVersion.substring(indexOfSeparator + 1));
                        }
                    }
                }
            }
        }
    }

    public String getRangeAc() {
        return rangeAc;
    }

    public void setRangeAc(String rangeAc) {
        this.rangeAc = rangeAc;
    }

    public static String extractRangeAcFromAnnotation(Annotation a){

        if (a != null){
            if (a.getAnnotationText() != null){
                 if (a.getAnnotationText().contains(rangeAcDelimiterStart) && a.getAnnotationText().contains(rangeAcDelimiterEnd)){
                     String text = a.getAnnotationText();

                     if (text.indexOf(rangeAcDelimiterStart) < text.indexOf(rangeAcDelimiterEnd) && text.indexOf(rangeAcDelimiterEnd) <= text.length() - 1){
                           return text.substring(text.indexOf(rangeAcDelimiterStart) + 1, text.indexOf(rangeAcDelimiterEnd));
                     }
                 }
            }
        }

        return null;
    }

    public String getUniprotAc() {
        return uniprotAc;
    }

    public void setUniprotAc(String uniprotAc) {
        this.uniprotAc = uniprotAc;
    }
}
