/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.protein.mock;

import uk.ac.ebi.intact.uniprot.model.UniprotXref;

import java.util.Collection;
import java.util.ArrayList;

/**
 * Xref list builder.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 1.1.2
 */
public class UniprotProteinXrefBuilder {

    Collection<UniprotXref> xrefs = new ArrayList<UniprotXref>( );

    public Collection<UniprotXref> build() {
        return xrefs;
    }

    public UniprotProteinXrefBuilder add(  String ac, String db, String desc ) {
        xrefs.add( new UniprotXref( ac, db, desc) );
        return this;
    }
}