/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.protein.mock;

import uk.ac.ebi.intact.uniprot.model.Organism;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinType;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

/**
 * TODO comment this
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since TODO artifact version here
 */
public class MockUniprotProtein {
    public static final String CANFA_ID = "CDC42_CANFA";
    public static final String CANFA_PRIMARY_AC = "P60952";
    public static final String CANFA_SECONDARY_AC_1 = "P21181";
    public static final String CANFA_SECONDARY_AC_2 = "P25763";
    public static final int CANFA_ORGA_TAXID = 9615;
    public static final String CANFA_ORGA_NAME = "Dog";
    public static final String CANFA_DESCRIPTION = "Cell division control protein 42 homolog precursor (G25K GTP-binding protein)";
    public static final String CANFA_GENE = "CDC42";
    public static final String CANFA_SEQ_UPDATE = "13-APR-2004";
    public static final String CANFA_ANNOT_UPDATE =  "20-FEB-2007";
    public static final String CANFA_SEQUENCE =  "MQTIKCVVVGDGAVGKTCLLISYTTNKFPSEYVPTVFDNYAVTVMIGGEPYTLGLFDTAG" +
                              "QEDYDRLRPLSYPQTDVFLVCFSVVSPSSFENVKEKWVPEITHHCPKTPFLLVGTQIDLR" +
                              "DDPSTIEKLAKNKQKPITPETAEKLARDLKAVKYVECSALTQRGLKNVFDEAILAALEPP" +
                              "ETQPKRKCCIF";


    public static final SimpleDateFormat sdf = new SimpleDateFormat( "dd-MMM-yyyy" );

    public static final Collection<String> NONE = new ArrayList<>(0);

    private static Date buildDate( String d ) {
        try {
            return sdf.parse( d );
        } catch ( ParseException e ) {
            throw new RuntimeException( "Could not parse date: " + d );
        }
    }

    private MockUniprotProtein() {
    }

    //////////////////////////////
    // Mock proteins

    public static UniprotProtein build_CDC42_CANFA() {
        return new UniprotProteinBuilder()
                .setSource( UniprotProteinType.SWISSPROT )
                .setId( CANFA_ID )
                .setPrimaryAc( CANFA_PRIMARY_AC )
                .setSecondaryAcs( Arrays.asList( CANFA_SECONDARY_AC_1, CANFA_SECONDARY_AC_2 ) )
                .setOrganism( new Organism( CANFA_ORGA_TAXID, CANFA_ORGA_NAME ) )
                .setDescription( CANFA_DESCRIPTION )
                .setGenes( Arrays.asList( CANFA_GENE ) )
                .setSynomyms( NONE )
                .setOrfs( NONE )
                .setLocuses( NONE )
                .setLastSequenceUpdate( buildDate( CANFA_SEQ_UPDATE ) )
                .setLastAnnotationUpdate( buildDate( CANFA_ANNOT_UPDATE ) )
                .setDiseases( NONE )
                .setFunctions( NONE )
                .setKeywords( NONE )
                .setCrossReferences(
                        new UniprotProteinXrefBuilder()
                                .add( "IPR003578", "InterPro", "GTPase_Rho" )
                                .add( "IPR013753", "InterPro", "Ras" )
                                .add( "IPR001806", "InterPro", "Ras_trnsfrmng" )
                                .add( "IPR005225", "InterPro", "Small_GTP_bd" )
                                /*  ENSCAFT00000023350; ENSCAFP00000021683; ENSCAFG00000014707 [P60952-2]
                                    ENSCAFT00030003966; ENSCAFP00030003518; ENSCAFG00030002163 [P60952-2]
                                    ENSCAFT00040003456; ENSCAFP00040002961; ENSCAFG00040001840 [P60952-2]
                                    ENSCAFT00040004651; ENSCAFP00040003996; ENSCAFG00040002443 [P60952-2]*/
                                .add( "ENSCAFG00000014707", "Ensembl", "gene", "P60952-2" )
                                .add( "ENSCAFG00030002163", "Ensembl", "gene", "P60952-2" )
                                .add( "ENSCAFG00040001840", "Ensembl", "gene", "P60952-2" )
                                .add( "ENSCAFG00040002443", "Ensembl", "gene", "P60952-2" )
                                .add( "ENSCAFT00000023350", "Ensembl", "transcript" ,"P60952-2")
                                .add( "ENSCAFT00030003966", "Ensembl", "transcript" ,"P60952-2")
                                .add( "ENSCAFT00040003456", "Ensembl", "transcript" ,"P60952-2")
                                .add( "ENSCAFT00040004651", "Ensembl", "transcript" ,"P60952-2")
                                .add( "ENSCAFP00000021683", "Ensembl", "protein" ,"P60952-2")
                                .add( "ENSCAFP00030003518", "Ensembl", "protein" ,"P60952-2")
                                .add( "ENSCAFP00040002961", "Ensembl", "protein" ,"P60952-2")
                                .add( "ENSCAFP00040003996", "Ensembl", "protein" ,"P60952-2")
                                .build()
                )
                .setCrc64( "34B44F9225EC106B" )
                .setSequence( CANFA_SEQUENCE )
                .setFeatureChains( null )
                .setSpliceVariants( Arrays.asList(
                        new UniprotSpliceVariantBuilder()
                                .setPrimaryAc( "P60952-1" )
                                .setSecondaryAcs( Arrays.asList( "P21181-1" ) )
                                .setOrganism( new Organism( 9615, "Dog" ) )
                                .setSynomyms( Arrays.asList( "Brain" ) )
                                .setNote( "Has not been isolated in dog so far" )
                                .setSequence( "MQTIKCVVVGDGAVGKTCLLISYTTNKFPSEYVPTVFDNYAVTVMIGGEPYTLGLFDTAG" +
                                              "QEDYDRLRPLSYPQTDVFLVCFSVVSPSSFENVKEKWVPEITHHCPKTPFLLVGTQIDLR" +
                                              "DDPSTIEKLAKNKQKPITPETAEKLARDLKAVKYVECSALTQRGLKNVFDEAILAALEPP" +
                                              "ETQPKRKCCIF" )
                                .setCrossReferences(
                                        new UniprotProteinXrefBuilder()
                                                /* ENSCAFT00000023351; ENSCAFP00000021684; ENSCAFG00000014707 [P60952-1]  */
                                                .add( "ENSCAFG00000014707", "Ensembl", "gene", "P60952-1" )
                                                .add( "ENSCAFT00000023351", "Ensembl", "transcript" ,"P60952-1")
                                                .add( "ENSCAFP00000021684", "Ensembl", "protein" ,"P60952-1")
                                                .build())
                                .build(),
                        new UniprotSpliceVariantBuilder()
                                .setPrimaryAc( "P60952-2" )
                                .setSecondaryAcs( Arrays.asList( "P21181-4" ) )
                                .setOrganism( new Organism( 9615, "Dog" ) )
                                .setSynomyms( Arrays.asList( "Placental" ) )
                                .setSequence( "MQTIKCVKRKCCIF" ) /* Fake sequence */
                                .setCrossReferences(
                                        new UniprotProteinXrefBuilder()
                                                /*  ENSCAFT00000023350; ENSCAFP00000021683; ENSCAFG00000014707 [P60952-2]
                                                    ENSCAFT00030003966; ENSCAFP00030003518; ENSCAFG00030002163 [P60952-2]
                                                    ENSCAFT00040003456; ENSCAFP00040002961; ENSCAFG00040001840 [P60952-2]
                                                    ENSCAFT00040004651; ENSCAFP00040003996; ENSCAFG00040002443 [P60952-2]*/
                                                .add( "ENSCAFG00000014707", "Ensembl", "gene", "P60952-2" )
                                                .add( "ENSCAFG00030002163", "Ensembl", "gene", "P60952-2" )
                                                .add( "ENSCAFG00040001840", "Ensembl", "gene", "P60952-2" )
                                                .add( "ENSCAFG00040002443", "Ensembl", "gene", "P60952-2" )
                                                .add( "ENSCAFT00000023350", "Ensembl", "transcript" ,"P60952-2")
                                                .add( "ENSCAFT00030003966", "Ensembl", "transcript" ,"P60952-2")
                                                .add( "ENSCAFT00040003456", "Ensembl", "transcript" ,"P60952-2")
                                                .add( "ENSCAFT00040004651", "Ensembl", "transcript" ,"P60952-2")
                                                .add( "ENSCAFP00000021683", "Ensembl", "protein" ,"P60952-2")
                                                .add( "ENSCAFP00030003518", "Ensembl", "protein" ,"P60952-2")
                                                .add( "ENSCAFP00040002961", "Ensembl", "protein" ,"P60952-2")
                                                .add( "ENSCAFP00040003996", "Ensembl", "protein" ,"P60952-2")
                                                .build())
                                .isCanonical(true)
                                .build()
                ) )
                .build();
    }

    public static UniprotProtein build_CDC42_HUMAN() {
        return new UniprotProteinBuilder()
                .setSource( UniprotProteinType.SWISSPROT )
                .setId( "CDC42_HUMAN" )
                .setPrimaryAc( "P60953" )
                .setSecondaryAcs( Arrays.asList( "P21181", "P25763", "Q7L8R5" ) )
                .setOrganism( new Organism( 9606, "Human" ) )
                .setDescription( "Cell division control protein 42 homolog precursor (G25K GTP-binding protein)" )
                .setGenes( Arrays.asList( "CDC42" ) )
                .setSynomyms( NONE )
                .setOrfs( NONE )
                .setLocuses( NONE )
                .setLastSequenceUpdate( buildDate( "13-APR-2004" ) )
                .setLastAnnotationUpdate( buildDate( "20-FEB-2007" ) )
                .setDiseases( NONE )
                .setFunctions( NONE )
                .setKeywords( Arrays.asList( "3D-structure", "Alternative splicing", "Direct protein sequencing" ) )
                .setCrossReferences(
                        new UniprotProteinXrefBuilder()
                                .add( "IPR003578", "InterPro", "GTPase_Rho" )
                                .add( "IPR013753", "InterPro", "Ras" )
                                .add( "IPR001806", "InterPro", "Ras_trnsfrmng" )
                                .add( "IPR005225", "InterPro", "Small_GTP_bd" )
                                .add( "GO:0005737", "Go", "" )
                                .add( "GO:0030175", "Go", "" )
                                .add( "GO:0005886", "Go", "" )
                                .add( "GO:0003924", "Go", "" )
                                 /* ENST00000344548; ENSP00000341072; ENSG00000070831 [P60953-2]
                                    ENST00000400259; ENSP00000383118; ENSG00000070831 [P60953-2]
                                    ENST00000648594; ENSP00000497733; ENSG00000070831 [P60953-2]
                                    ENST00000656825; ENSP00000499457; ENSG00000070831 [P60953-2]
                                    ENST00000662562; ENSP00000499612; ENSG00000070831 [P60953-2] */
                                .add( "ENSG00000070831", "Ensembl", "gene", "P60953-2" )
                                .add( "ENST00000344548", "Ensembl", "transcript" ,"P60953-2")
                                .add( "ENST00000400259", "Ensembl", "transcript" ,"P60953-2")
                                .add( "ENST00000648594", "Ensembl", "transcript" ,"P60953-2")
                                .add( "ENST00000656825", "Ensembl", "transcript" ,"P60953-2")
                                .add( "ENST00000662562", "Ensembl", "transcript" ,"P60953-2")
                                .add( "ENSP00000341072", "Ensembl", "protein" ,"P60953-2")
                                .add( "ENSP00000383118", "Ensembl", "protein" ,"P60953-2")
                                .add( "ENSP00000497733", "Ensembl", "protein" ,"P60953-2")
                                .add( "ENSP00000499457", "Ensembl", "protein" ,"P60953-2")
                                .add( "ENSP00000499612", "Ensembl", "protein" ,"P60953-2")
                                .add( "2NGR", "PDB", "" )
                                .build()
                )
                .setCrc64( "34B44F9225EC106B" )
                .setSequence( "MQTIKCVVVGDGAVGKTCLLISYTTNKFPSEYVPTVFDNYAVTVMIGGEPYTLGLFDTAG" +
                              "QEDYDRLRPLSYPQTDVFLVCFSVVSPSSFENVKEKWVPEITHHCPKTPFLLVGTQIDLR" +
                              "DDPSTIEKLAKNKQKPITPETAEKLARDLKAVKYVECSALTQRGLKNVFDEAILAALEPP" +
                              "ETQPKRKCCIF" )
                .setFeatureChains( null )
                .setSpliceVariants( Arrays.asList(
                        new UniprotSpliceVariantBuilder()
                                .setPrimaryAc( "P60953-1" )
                                .setSecondaryAcs( Arrays.asList( "P21181-1" ) )
                                .setOrganism( new Organism( 9606, "Human" ) )
                                .setSynomyms( Arrays.asList( "Brain" ) )
                                .setNote( "only present in brain" )
                                .setSequence( "MQTIKCVVVGDGAVGKTCLLISYTTNKFPSEYVPTVFDNYAVTVMIGGEPYTLGLFDTAG" +
                                              "QEDYDRLRPLSYPQTDVFLVCFSVVSPSSFENVKEKWVPEITHHCPKTPFLLVGTQIDLR" +
                                              "DDPSTIEKLAKNKQKPITPETAEKLARDLKAVKYVECSALTQRGLKNVFDEAILAALEPP" +
                                              "ETQPKRKCCIF" )
                                .setCrossReferences(
                                        new UniprotProteinXrefBuilder()
                                                 /* ENST00000315554; ENSP00000314458; ENSG00000070831 [P60953-1] */
                                                .add( "ENSG00000070831", "Ensembl", "gene", "P60953-1" )
                                                .add( "ENST00000315554", "Ensembl", "transcript" ,"P60953-1")
                                                .add( "ENSP00000314458", "Ensembl", "protein" ,"P60953-1")
                                                .build())
                                .isCanonical(false)
                                .build(),
                        new UniprotSpliceVariantBuilder()
                                .setPrimaryAc( "P60953-2" )
                                .setSecondaryAcs( Arrays.asList( "P21181-4", "P00012-2" ) )
                                .setOrganism( new Organism( 9606, "Human" ) )
                                .setSynomyms( Arrays.asList( "Placental" ) )
                                .setSequence( "SYPQTDVFLVCFSVVSPSSFENVKEKWVPEITHHHH" ) /* Fake sequence */
                                .setCrossReferences(
                                        new UniprotProteinXrefBuilder()
                                                 /* ENST00000344548; ENSP00000341072; ENSG00000070831 [P60953-2]
                                                    ENST00000400259; ENSP00000383118; ENSG00000070831 [P60953-2]
                                                    ENST00000648594; ENSP00000497733; ENSG00000070831 [P60953-2]
                                                    ENST00000656825; ENSP00000499457; ENSG00000070831 [P60953-2]
                                                    ENST00000662562; ENSP00000499612; ENSG00000070831 [P60953-2] */
                                                .add( "ENSG00000070831", "Ensembl", "gene", "P60953-2" )
                                                .add( "ENST00000344548", "Ensembl", "transcript" ,"P60953-2")
                                                .add( "ENST00000400259", "Ensembl", "transcript" ,"P60953-2")
                                                .add( "ENST00000648594", "Ensembl", "transcript" ,"P60953-2")
                                                .add( "ENST00000656825", "Ensembl", "transcript" ,"P60953-2")
                                                .add( "ENST00000662562", "Ensembl", "transcript" ,"P60953-2")
                                                .add( "ENSP00000341072", "Ensembl", "protein" ,"P60953-2")
                                                .add( "ENSP00000383118", "Ensembl", "protein" ,"P60953-2")
                                                .add( "ENSP00000497733", "Ensembl", "protein" ,"P60953-2")
                                                .add( "ENSP00000499457", "Ensembl", "protein" ,"P60953-2")
                                                .add( "ENSP00000499612", "Ensembl", "protein" ,"P60953-2")
                                                .build())
                                .isCanonical(true)
                                .build()
                ) )
                .build();
    }
}