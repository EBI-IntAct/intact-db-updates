/*
Copyright (c) 2002 The European Bioinformatics Institute, and others.
 All rights reserved. Please see the file LICENSE
in the root directory of this distribution.
*/
package uk.ac.ebi.intact.util.biosource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.bridges.taxonomy.TaxonomyService;
import uk.ac.ebi.intact.bridges.taxonomy.TaxonomyServiceException;
import uk.ac.ebi.intact.bridges.taxonomy.TaxonomyTerm;
import uk.ac.ebi.intact.business.IntactException;
import uk.ac.ebi.intact.business.IntactTransactionException;
import uk.ac.ebi.intact.context.IntactContext;
import uk.ac.ebi.intact.context.DataContext;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.persistence.dao.BioSourceDao;
import uk.ac.ebi.intact.persistence.dao.CvObjectDao;
import uk.ac.ebi.intact.persistence.dao.DaoFactory;

import java.util.Collection;
import java.util.Iterator;

/**
 * Implementation of the BioSourceService.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 1.0
 */
public class BioSourceServiceImpl implements BioSourceService {

    // TODO Make sure the IntAct transactions will not be an issue if they are started outside the scope of the
    // TODO BioSourceService. If so, we should reuse the existing one instead of creating a new one.

    /**
     * Sets up a logger for that class.
     */
    public static final Log log = LogFactory.getLog( BioSourceServiceImpl.class );

    /**
     * The institution to which we have to link all new BioSource
     */
    private Institution institution;

    /**
     * Adapter allowing to get access to the Taxonomy data.
     */
    private TaxonomyService taxonomyService;

    ///////////////////////////
    // Constructor

    public BioSourceServiceImpl( TaxonomyService taxonomyService ) {
        // set default institution
        this( taxonomyService, IntactContext.getCurrentInstance().getInstitution() );
    }

    public BioSourceServiceImpl( TaxonomyService taxonomyAdapter, Institution institution ) {
        setTaxonomyService( taxonomyAdapter );
        setInstitution( institution );
    }

    //////////////////////
    // Setters

    private void setTaxonomyService( TaxonomyService taxonomyService ) {
        if ( taxonomyService == null ) {
            throw new IllegalArgumentException();
        }
        this.taxonomyService = taxonomyService;
    }

    private void setInstitution( Institution institution ) {
        if ( institution == null ) {
            throw new IllegalArgumentException();
        }
        this.institution = institution;
    }

    ///////////////////////
    // Private methods

    /**
     * Search the IntAct database and retreive a BioSource having the given taxid and no CvCellType or CvTissue.
     *
     * @param taxid a non null taxid.
     *
     * @return a BioSource or null if none is found.
     */
    private BioSource searchIntactByTaxid( String taxid ) throws BioSourceServiceException {
        log.debug( "Searching in the database for BioSource(" + taxid + ")" );
        DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();
        DaoFactory daoFactory = dataContext.getDaoFactory();

        if ( !daoFactory.isTransactionActive() ) {
            log.debug( "There's no transaction active, start a local transaction." );
            daoFactory.beginTransaction();
        } else {
            log.debug( "There's already an ongoing transaction, we join it." );
        }


        BioSourceDao bsDao = daoFactory.getBioSourceDao();
        BioSource biosource = bsDao.getByTaxonIdUnique( taxid );

        try {
            dataContext.commitTransaction();
        } catch (IntactTransactionException e) {
            throw new BioSourceServiceException("Problem committing", e);
        }

        if ( log.isDebugEnabled() ) {
            if ( biosource == null ) {
                log.debug( "Could not find Biosource having taxid: " + taxid );
            } else {
                log.debug( "Found 1 biosource: " + biosource.getShortLabel() + " [" + biosource.getAc() + "]" );
            }
        }

        return biosource;
    }


    /**
     * Create a BioSource and save it in the database.
     *
     * @param shortlabel shortlabel of the Biosource.
     * @param fullname   fullname of the Biosource.
     * @param taxid      taxid of the Biosource.
     *
     * @return a persistent BioSource.
     */
    private BioSource createAndPersistBioSource( String shortlabel, String fullname, String taxid ) throws BioSourceServiceException {

        if( log.isDebugEnabled() ) {
            log.debug( "Persisting BioSource(" + taxid + ", " + shortlabel + ", " + fullname + ")" );
        }

        DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();
        DaoFactory daoFactory = dataContext.getDaoFactory();

        dataContext.beginTransaction();

        // Instanciate it
        BioSource bioSource = new BioSource( institution, shortlabel, taxid );
        bioSource.setFullName( fullname );

        // Add Newt Xref

        // get source database
        CvObjectDao<CvDatabase> dbDao = daoFactory.getCvObjectDao( CvDatabase.class );
        final String miRef = taxonomyService.getSourceDatabaseMiRef();
        CvDatabase db = dbDao.getByPsiMiRef( miRef );
        if ( db == null ) {
            final String name = taxonomyService.getClass().getSimpleName();
            throw new IllegalStateException( "Could not find a CvDatabase based on the MI reference given by the " +
                                             "TaxonomyService[" + name + "]: " + miRef );
        }

        // retrieve identity qualifier
        CvObjectDao<CvXrefQualifier> qDao = daoFactory.getCvObjectDao( CvXrefQualifier.class );
        CvXrefQualifier identity = qDao.getByPsiMiRef( CvXrefQualifier.IDENTITY_MI_REF );

        BioSourceXref xref = new BioSourceXref( institution, db, taxid, identity );
        bioSource.addXref( xref );

        // persist
        BioSourceDao sourceDao = daoFactory.getBioSourceDao();
        sourceDao.saveOrUpdate( bioSource );

        try {
            dataContext.commitTransaction();
        } catch (IntactTransactionException e) {
            throw new BioSourceServiceException("Problem committing", e);
        }

        return bioSource;
    }

    /**
     * Handles special BioSource that are not supported by taxonomy ontologies.
     *
     * @param taxid a non null taxid.
     *
     * @return a valid BioSource or null of the taxid is not supported.
     */
    private BioSource handleSpecialBiosource( String taxid ) throws BioSourceServiceException {

        int myTaxid = Integer.parseInt( taxid );
        BioSource bioSource = null;

        switch ( myTaxid ) {
            case 0:
                bioSource = searchIntactByTaxid( taxid );
                if ( bioSource == null ) {
                    bioSource = createAndPersistBioSource( "in vivo", null, taxid );
                }
                break;

            case-1:
                bioSource = searchIntactByTaxid( taxid );
                if ( bioSource == null ) {
                    bioSource = createAndPersistBioSource( "in vitro", null, taxid );
                }
                break;

            case-2:
                bioSource = searchIntactByTaxid( taxid );
                if ( bioSource == null ) {
                    bioSource = createAndPersistBioSource( "chemical synthesis", null, taxid );
                }
                break;

            case-3:
                bioSource = searchIntactByTaxid( taxid );
                if ( bioSource == null ) {
                    bioSource = createAndPersistBioSource( "unknown", null, taxid );
                }
                break;

            default:
                // this is not a supported special biosource.
        }

        return bioSource;
    }


    ////////////////////////////
    // BioSourceLoaderService

    public BioSource getBiosourceByTaxid( String taxid ) throws BioSourceServiceException {

        if ( taxid == null ) {
            throw new NullPointerException( "taxid must not be null." );
        }
        
        int taxidInt;
        try {
            taxidInt = Integer.parseInt( taxid );
        } catch ( NumberFormatException e ) {
            throw new BioSourceServiceException( "A taxid is expected to be an Integer value: " + taxid );
        }

        BioSource bs = searchIntactByTaxid( taxid );
        if ( bs == null ) {
            // it is not yet in IntAct
            bs = handleSpecialBiosource( taxid );

            if ( bs == null ) {
                // it is not a special BioSource, use the taxonomy service to retreive data.
                TaxonomyTerm taxTerm = null;
                try {
                    taxTerm = taxonomyService.getTaxonomyTerm( taxidInt );
                } catch ( TaxonomyServiceException e ) {
                    throw new BioSourceServiceException( "Error while retreiving Taxonomy term.", e );
                }

                if ( taxTerm == null ) {
                    String name = taxonomyService.getClass().getSimpleName();
                    throw new BioSourceServiceException( "The TaxonomyService[" + name + "] returned a null TaxonomyTerm" );
                }

                //Sometimes the common name is null in Newt, therefore we choose as shortlabel the taxid.
                String shortlabel = taxTerm.getCommonName();
                if(shortlabel == null || (shortlabel != null && shortlabel.trim().length() == 0)){
                    shortlabel = taxTerm.getTaxid() + "";
                }

                bs = createAndPersistBioSource( shortlabel,
                                                taxTerm.getScientificName(),
                                                String.valueOf( taxTerm.getTaxid() ) );
            }
        }

        if ( bs == null ) {
            throw new BioSourceServiceException( "Failed to create a valid BioSource(taxid:" + taxid + ")" );
        }

        return bs;
    }

    public String getUpToDateTaxid( String taxid ) throws BioSourceServiceException {

        if ( taxid == null ) {
            throw new NullPointerException( "taxid must not be null." );
        }

        int t;
        try {
            t = Integer.parseInt( taxid );
        } catch ( NumberFormatException e ) {
            throw new BioSourceServiceException( "A taxid is expected to be an Integer value: " + taxid );
        }

        TaxonomyTerm term = null;
        try {
            term = taxonomyService.getTaxonomyTerm( t );
        } catch ( TaxonomyServiceException e ) {
            throw new BioSourceServiceException( "Error while accessing the taxonomy service.", e );
        }

        return String.valueOf( term.getTaxid() );
    }
}
