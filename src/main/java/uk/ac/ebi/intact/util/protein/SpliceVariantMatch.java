/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.protein;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.uniprot.model.UniprotSpliceVariant;

/**
 * Representation of the association between an intact protein and uniprot splice variant.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 1.1.2
 */
public class SpliceVariantMatch {

    private Protein intactProtein;

    private UniprotSpliceVariant uniprotSpliceVariant;


    public SpliceVariantMatch( Protein intactProtein, UniprotSpliceVariant uniprotSpliceVariant ) {

        if( intactProtein== null && uniprotSpliceVariant == null ) {
            throw new IllegalArgumentException( "Either intact or uniprot protein must not be null." );
        }

        this.intactProtein = intactProtein;
        this.uniprotSpliceVariant = uniprotSpliceVariant;
    }

    ////////////////////////
    // Getters

    public Protein getIntactProtein() {
        return intactProtein;
    }

    public UniprotSpliceVariant getUniprotSpliceVariant() {
        return uniprotSpliceVariant;
    }

    ///////////////////////////
    // Questions to a match

    public boolean isSuccessful() {
        return ( uniprotSpliceVariant != null && intactProtein != null );
    }

    public boolean hasNoIntact() {
        return ( uniprotSpliceVariant != null && intactProtein == null );
    }

    public boolean hasNoUniprot() {
        return ( uniprotSpliceVariant == null && intactProtein != null );
    }
}