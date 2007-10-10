/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.protein;

import uk.ac.ebi.intact.util.biosource.BioSourceService;
import uk.ac.ebi.intact.util.protein.utils.UniprotServiceResult;
import uk.ac.ebi.intact.uniprot.service.UniprotService;

import java.util.Collection;

/**
 * What the protein loader service van do.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since <pre>08-Feb-2007</pre>
 */
public interface ProteinService {

    // Requirements:
    // ------------
    //                insert by uniprot AC (single AC or List)
    //                filtering on taxid
    // [low priority] get stats of object created
    //                allow personalized configuration:
    //                      - DR line filter
    //                      - Shortlabel / Fullname
    //                      - Feature chain loading (true/false)
    //                      - Splice variant loading (true/false)

    // [low priority] cache AC -> UniprotProtein with ehcache

    ///////////////////////////
    // Handle single protein

    public UniprotServiceResult retrieve( String uniprotId );

    ////////////////////////////////////
    // Configuration

    public BioSourceService getBioSourceService();

    public void setBioSourceService( BioSourceService bioSourceService );

    public UniprotService getUniprotService();

    public void setUniprotService( UniprotService uniprotService );

//    public void addDbMapping( String databaseName, String miRef );
}