/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.deadproteins;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO comment this
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since <pre>21-Nov-2006</pre>
 */
public class MappingParser {

    public static Map<String, Collection<RemappingEntry>> parse( File file ) throws IOException {

        Map<String, Collection<RemappingEntry>> remapping = new HashMap<String, Collection<RemappingEntry>>();

        BufferedReader in = new BufferedReader( new FileReader( file ) );
        String line;

        while ( ( line = in.readLine() ) != null ) {
            // process line here

            // line 1 - UniProt ID
            String identifier = line;

            // line 2..n - proposed remapping
            while ( !( line = in.readLine() ).equals( "" ) ) {
                // we haven't hit a blank line yet
                String[] values = line.split( "\\t" );

                String upi = values[0];
                RemappingEntry entry = new RemappingEntry( identifier, upi );

                String[] ids = values[1].split( "\\s" );// split on space
                for ( int j = 0; j < ids.length; j++ ) {
                    String id = ids[j].trim();
                    entry.addIdentifier( id );
                }

                // keep the UPI without prefix
                entry.setUpi( values[2].trim() );

                // sequence length
                int sequenceLength = Integer.parseInt( values[3].trim() );
                entry.setSequenceLength( sequenceLength );

                if ( !remapping.containsKey( identifier ) ) {
                    remapping.put( identifier, new ArrayList<RemappingEntry>( 4 ) );
                }
                Collection<RemappingEntry> entries = remapping.get( identifier );
                entries.add( entry );
            } // while mapping lines

            // line n+1 - blank line => finish that entry and get ready for a potential next one.
        } // while
        in.close();

        return remapping;
    }
}