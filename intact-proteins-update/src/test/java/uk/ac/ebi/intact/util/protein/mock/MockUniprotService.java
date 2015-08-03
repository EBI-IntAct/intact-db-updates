/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.protein.mock;

import uk.ac.ebi.intact.uniprot.model.UniprotFeatureChain;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;
import uk.ac.ebi.intact.uniprot.model.UniprotSpliceVariant;
import uk.ac.ebi.intact.uniprot.service.AbstractUniprotService;

import java.util.*;

/**
 * Mock service serving mock proteins.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 1.1.2
 */
public class MockUniprotService extends AbstractUniprotService {


    private static Map<String, Collection<UniprotProtein>> proteins = new HashMap<String, Collection<UniprotProtein>>( );

    static {

        // This static block initialize the mock proteins and how they can be searched on (ACs and variant ACs).

        UniprotProtein cdc42canfa = MockUniprotProtein.build_CDC42_CANFA();
        UniprotProtein cdc42human = MockUniprotProtein.build_CDC42_HUMAN();

        /*In uniprot, if you search for P60952 or it's splice variant (P60952-1, P60952-2), it will return the same entry.
        That's why P60952, P60952-1 and P60952-2 are associated to the same UniprotProtein cdc42canfa*/
        proteins.put( "P60952", Arrays.asList( cdc42canfa ) );
        proteins.put( "P60952-1", Arrays.asList( cdc42canfa ) );
        proteins.put( "P60952-2", Arrays.asList( cdc42canfa ) );

        proteins.put( "P60953", Arrays.asList( cdc42human ) );
        proteins.put( "Q7L8R5", Arrays.asList( cdc42human ) ); /* (!) secondary AC not shared by CANFA */
        proteins.put( "P60953-1", Arrays.asList( cdc42human ) );
        proteins.put( "P60953-2", Arrays.asList( cdc42human ) );

        proteins.put( "P21181", Arrays.asList( cdc42canfa, cdc42human ) );
        proteins.put( "P25763", Arrays.asList( cdc42canfa, cdc42human ) );
        proteins.put( "P21181-1", Arrays.asList( cdc42canfa, cdc42human ) );
        proteins.put( "P21181-4", Arrays.asList( cdc42canfa, cdc42human ) );

        proteins.put( "P00012", Arrays.asList( cdc42human, cdc42human ) );
        proteins.put( "P00012-1", Arrays.asList( cdc42human, cdc42human ) );
        proteins.put( "P00012-2", Arrays.asList( cdc42canfa, cdc42canfa ) );
    }

    ////////////////////////////
    // AbstractUniprotService

    public Collection<UniprotProtein> retrieve( String ac ) {
        Collection<UniprotProtein> myProteins = new ArrayList<UniprotProtein>( 2 );

        if (proteins.get( ac ) != null){
            myProteins.addAll( proteins.get( ac ) );            
        }
        return myProteins;
    }

    public Collection<UniprotProtein> retrieve( String ac, boolean processSpliceVars ) {
        throw new UnsupportedOperationException( );
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

    public Map<String, Collection<UniprotProtein>> retrieve( Collection<String> acs, boolean processSpliceVars ) {
        throw new UnsupportedOperationException( );
    }

    @Deprecated
    public Map<String, Collection<UniprotProtein>> retreive(Collection<String> strings){
        return retrieve(strings);
    }

    @Override
    public Collection<UniprotProteinTranscript> retrieveProteinTranscripts(String ac) {
        return null;
    }

    @Override
    public Collection<UniprotSpliceVariant> retrieveSpliceVariant(String ac) {
        return null;
    }

    @Override
    public Collection<UniprotFeatureChain> retrieveFeatureChain(String ac) {
        return null;
    }

    @Override
    public void close() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}