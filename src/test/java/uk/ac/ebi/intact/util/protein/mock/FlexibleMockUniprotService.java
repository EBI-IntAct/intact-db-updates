/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.protein.mock;

import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.service.AbstractUniprotService;

import java.util.*;

/**
 * Mock service serving mock proteins.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 1.1.2
 */
public class FlexibleMockUniprotService extends AbstractUniprotService {

    private Map<String, Collection<UniprotProtein>> proteins = new HashMap<String, Collection<UniprotProtein>>( );

    ////////////////////////////////////////////////
    // Control of proteins served by the service.

    public void add( String ac, Collection<UniprotProtein> myProteins ) {
        proteins.put( ac, myProteins );
    }

    public void add( String ac, UniprotProtein protein ) {
        add( ac, Arrays.asList( protein ) );
    }

    public void clear() {
        proteins.clear();
    }

    ////////////////////////////
    // AbstractUniprotService

    public Collection<UniprotProtein> retrieve( String ac ) {
        Collection<UniprotProtein> myProteins = new ArrayList<UniprotProtein>( 2 );
        if(proteins.containsKey(ac)){
            myProteins.addAll( proteins.get( ac ) );
        }
        return myProteins;
    }

    @Deprecated
    public Collection<UniprotProtein> retreive(String s) {
        return retrieve(s);
    }

    public Map<String, Collection<UniprotProtein>> retrieve( Collection<String> acs ) {
        Map<String, Collection<UniprotProtein>> results = new HashMap<String, Collection<UniprotProtein>>( acs.size() );
        for ( String ac : acs ) {
            results.put( ac, retrieve( ac ) );
        }
        return results;
    }

    @Deprecated
    public Map<String, Collection<UniprotProtein>> retreive(Collection<String> strings){
        return retrieve(strings);
    }
}