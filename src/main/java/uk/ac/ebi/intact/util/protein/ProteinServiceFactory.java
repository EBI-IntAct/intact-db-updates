/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.protein;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import uk.ac.ebi.intact.uniprot.service.UniprotService;

/**
 * Factory allowing to instanciate a ProteinLoaderService.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since <pre>09-Feb-2007</pre>
 */
public class ProteinServiceFactory {

    public static final String SPRING_CONFIG_FILE = "/protein-service.xml";

    //////////////////////
    // Singleton

    private static ProteinServiceFactory ourInstance = new ProteinServiceFactory();

    public static ProteinServiceFactory getInstance() {
        return ourInstance;
    }

    private ProteinServiceFactory() {
    }

    //////////////////////
    // Instance methods

    public ProteinService buildProteinService() {
        ClassPathResource resource = new ClassPathResource( SPRING_CONFIG_FILE );
        BeanFactory factory = new XmlBeanFactory( resource );
        ProteinService loaderService = ( ProteinService ) factory.getBean( "proteinLoaderBean" );
        return loaderService;
    }

    public ProteinService buildProteinService( UniprotService uniprotService ) {
        if ( uniprotService == null ) {
            throw new IllegalArgumentException( "You must give a non null implementation of UniprotService." );
        }
        return new ProteinServiceImpl( uniprotService );
    }
}