/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.protein;

/**
 * Exception thrown by the protein service.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 1.5.0-rc2
 */
public class ProteinServiceException extends Exception {

    public ProteinServiceException() {
    }

    public ProteinServiceException( Throwable cause ) {
        super( cause );
    }

    public ProteinServiceException( String message ) {
        super( message );
    }

    public ProteinServiceException( String message, Throwable cause ) {
        super( message, cause );
    }
}