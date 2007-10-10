/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.protein.mock;

import uk.ac.ebi.intact.uniprot.model.Organism;
import uk.ac.ebi.intact.uniprot.model.UniprotSpliceVariant;

import java.util.Collection;
import java.util.List;

/**
 * Splice Variant builder.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 1.1.2
 */
public class UniprotSpliceVariantBuilder {

    /**
     * Accession number of the splice variant.
     */
    private String primaryAc;

    /**
     * Secondary accession number of the splice variant.
     */
    private List<String> secondaryAcs;

    /**
     * Collection of synonyms.
     */
    private Collection<String> synomyms;

    /**
     * Sequence of the splice variant.
     */
    private String sequence;

    /**
     * Organism of a splice variant.
     */
    private Organism organism;

    /**
     * Start range of the splice variant.
     */
    private Integer start;

    /**
     * End range of the splice variant.
     */
    private Integer end;

    /**
     * Additional note of the splice variant.
     */
    private String note;


    public UniprotSpliceVariantBuilder() {
    }


    public UniprotSpliceVariantBuilder setEnd( Integer end ) {
        this.end = end;
        return this;
    }

    public UniprotSpliceVariantBuilder setNote( String note ) {
        this.note = note;
        return this;
    }

    public UniprotSpliceVariantBuilder setOrganism( Organism organism ) {
        this.organism = organism;
        return this;
    }

    public UniprotSpliceVariantBuilder setPrimaryAc( String primaryAc ) {
        this.primaryAc = primaryAc;
        return this;
    }

    public UniprotSpliceVariantBuilder setSecondaryAcs( List<String> secondaryAcs ) {
        this.secondaryAcs = secondaryAcs;
        return this;
    }

    public UniprotSpliceVariantBuilder setSequence( String sequence ) {
        this.sequence = sequence;
        return this;
    }

    public UniprotSpliceVariantBuilder setStart( Integer start ) {
        this.start = start;
        return this;
    }

    public UniprotSpliceVariantBuilder setSynomyms( Collection<String> synomyms ) {
        this.synomyms = synomyms;
        return this;
    }

    public UniprotSpliceVariant build() {
        UniprotSpliceVariant variant = new UniprotSpliceVariant( primaryAc, organism, sequence );
        variant.setSecondaryAcs( secondaryAcs );
        variant.setSynomyms( synomyms );
        variant.setNote( note );
        variant.setStart( start );
        variant.setEnd( end );
        return variant;
    }
}