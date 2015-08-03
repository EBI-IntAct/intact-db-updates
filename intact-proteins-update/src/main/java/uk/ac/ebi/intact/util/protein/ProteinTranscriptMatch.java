/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.protein;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;

/**
 * Representation of the association between an intact protein and uniprot splice variant.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 1.1.2
 */
@Deprecated
public class ProteinTranscriptMatch {

    private Protein intactProtein;

    private UniprotProteinTranscript uniprotTranscript;


    public ProteinTranscriptMatch( Protein intactProtein, UniprotProteinTranscript uniprotProteinTranscript ) {

        if( intactProtein== null && uniprotProteinTranscript == null ) {
            throw new IllegalArgumentException( "Either intact or uniprot protein must not be null." );
        }

        this.intactProtein = intactProtein;
        this.uniprotTranscript = uniprotProteinTranscript;
    }

    ////////////////////////
    // Getters

    public Protein getIntactProtein() {
        return intactProtein;
    }

    public UniprotProteinTranscript getUniprotTranscript() {
        return uniprotTranscript;
    }

    ///////////////////////////
    // Questions to a match

    public boolean isSuccessful() {
        return ( uniprotTranscript != null && intactProtein != null );
    }

    public boolean hasNoIntact() {
        return ( uniprotTranscript != null && intactProtein == null );
    }

    public boolean hasNoUniprot() {
        return ( uniprotTranscript == null && intactProtein != null );
    }
}