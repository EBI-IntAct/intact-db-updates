/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.protein.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.context.IntactContext;
import uk.ac.ebi.intact.model.AnnotatedObject;
import uk.ac.ebi.intact.model.Annotation;

/**
 * Utilities for updating Annotations.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 1.1.2
 */
public class AnnotationUpdaterUtils {

    /**
     * Sets up a logger for that class.
     */
    public static final Log log = LogFactory.getLog( AnnotationUpdaterUtils.class );

    /**
     * Add an annotation to an annotated object. <br> We check if that annotation is not already existing, if so, we
     * don't record it.
     *
     * @param current    the annotated object to which we want to add an Annotation.
     * @param annotation the annotation to add the Annotated object
     */
    public static void addNewAnnotation( AnnotatedObject current, final Annotation annotation ) {

        if ( current.getAnnotations().contains( annotation ) ) {
            if ( log.isDebugEnabled() ) {
                log.debug( "SKIP " + annotation );
            }
            return; // already in, exit
        }

        // add the alias to the AnnotatedObject
        current.addAnnotation( annotation );

        IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getAnnotationDao().persist( annotation );
        // update current as well !
        log.debug( "ADDED " + annotation + " to: " + current.getShortLabel() );
    }
}