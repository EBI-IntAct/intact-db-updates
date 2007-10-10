/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.biosource;

import uk.ac.ebi.intact.model.BioSource;

/**
 * Defines a BioSourceService.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 1.0
 */
public interface BioSourceService {

    /**
     * Gives a valid taxid.
     *
     * @param taxid the original taxid
     *
     * @return a valid taxid (can be different from the original in case of obsoletness).
     *
     * @throws BioSourceServiceException if an error occur during the processing.
     */
    public String getUpToDateTaxid( String taxid ) throws BioSourceServiceException;

    /**
     * Build and persist a BioSource based on the given taxid.
     *
     * @param taxid a non null valid taxid.
     *
     * @return a valid BioSource of which the taxid might be different from the one given if it happens to be obsolete in the
     *         taxonomy ontology in use. The BioSource returned has been persisted into the Database.
     *
     * @throws BioSourceServiceException if an error occur during the processing.
     */
    public BioSource getBiosourceByTaxid( String taxid ) throws BioSourceServiceException;
}