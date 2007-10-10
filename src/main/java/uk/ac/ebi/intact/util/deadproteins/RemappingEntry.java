/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.deadproteins;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO comment this
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since <pre>21-Nov-2006</pre>
 */
public class RemappingEntry {

    /////////////////////////
    // Instance attributes

    private String originalId;
    private String upi;
    private List<String> identifiers = new ArrayList( 4 );
    private int sequenceLength = -1;

    ////////////////////
    // Constructor

    public RemappingEntry( String originalId, String upi ) {
        this.originalId = originalId;
        this.upi = upi;
    }

    ////////////////////////
    // Getters and Settes

    public String getOriginalId() {
        return originalId;
    }

    public void setOriginalId( String originalId ) {
        this.originalId = originalId;
    }

    public List<String> getIdentifiers() {
        return identifiers;
    }

    public void addIdentifier( String identifier ) {
        this.identifiers.add( identifier );
    }

    public void removeIdentifier( String identifier ) {
        this.identifiers.remove( identifier );
    }

    public int getSequenceLength() {
        return sequenceLength;
    }

    public void setSequenceLength( int sequenceLength ) {
        this.sequenceLength = sequenceLength;
    }

    public String getUpi() {
        return upi;
    }

    public void setUpi( String upi ) {
        this.upi = upi;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append( "RemappingEntry" );
        sb.append( "{identifiers=" ).append( identifiers );
        sb.append( ", originalId='" ).append( originalId ).append( '\'' );
        sb.append( ", upi='" ).append( upi ).append( '\'' );
        sb.append( ", sequenceLength=" ).append( sequenceLength );
        sb.append( '}' );
        return sb.toString();
    }
}