package uk.ac.ebi.intact.dbupdate.feature.model;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public enum AminoAcids {
    ALANINE('A', "ala"),
    ASPARAGINE_OR_ASPARTIC_ACID('B', "asx"),
    CYSTEINE('C', "cys"),
    ASPARTIC_ACID('D', "asp"),
    GLUTAMIC_ACID('E', "glu"),
    PHENYLALANINE('F', "phe"),
    GLYCINE('G', "gly"),
    HISTIDINE('H', "his"),
    ISOLEUCINE('I', "ile"),
    LYSINE('K', "lys"),
    LEUCINE('L', "leu"),
    METHIONINE('M', "met"),
    ASPARAGINE('N', "asn"),
    SELENOCYSTEINE('U', "sec"),
    PROLINE('P', "pro"),
    GLUTAMINE('Q', "gln"),
    ARGININE('R', "arg"),
    SERINE('S', "ser"),
    THREONINE('T', "thr"),
    PYRROLYSINE('O', "pyl"),
    VALINE('V', "val"),
    TRYPTOPHAN('W', "trp"),
    ANY_CODON('X', "x"),
    TYROSINE('Y', "tyr"),
    GLUTAMINE_OR_GLUTAMIC_ACID('Z', "glx"),
    STOP_CODON('*', "*"),
    DELETION('.', "del");

    private Character oneLetterCode;
    private String threeLetterCode;

    AminoAcids(Character oneLetterCode, String threeLetterCode) {
        this.oneLetterCode = oneLetterCode;
        this.threeLetterCode = threeLetterCode;
    }

    public Character getOneLetterCode() {
        return oneLetterCode;
    }

    public String getThreeLetterCode() {
        return threeLetterCode;
    }

    public static String getThreeLetterCodeByOneLetterCode(Character oneLetterCode) {
        for (AminoAcids aminoAcids : values()) {
            if (aminoAcids.getOneLetterCode() == oneLetterCode) {
                return aminoAcids.getThreeLetterCode();
            }
        }
        return null;
    }
}
