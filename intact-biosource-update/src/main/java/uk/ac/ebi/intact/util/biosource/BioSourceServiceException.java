/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.biosource;

/**
 * Specific exception for this BioSourceService.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 1.0
 */
public class BioSourceServiceException extends Exception {

    public BioSourceServiceException() {
    }

    public BioSourceServiceException( Throwable cause ) {
        super( cause );
    }

    public BioSourceServiceException( String message ) {
        super( message );
    }

    public BioSourceServiceException( String message, Throwable cause ) {
        super( message, cause );
    }
}