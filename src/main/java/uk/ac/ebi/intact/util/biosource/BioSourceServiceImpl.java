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
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.persistence.dao.BioSourceDao;
import uk.ac.ebi.intact.persistence.dao.CvObjectDao;
import uk.ac.ebi.intact.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.persistence.dao.IntactTransaction;

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
    // Utility methods

    /**
     * Returns CvDatabase(newt)
     *
     * @return
     *
     * @throws IntactException
     */
    private CvDatabase getNewt() throws IntactException {
        CvDatabase newt = IntactContext.getCurrentInstance().getCvContext().getByMiRef( CvDatabase.class, CvDatabase.NEWT_MI_REF );
        if ( newt == null ) {
            throw new IllegalStateException( "Could not find newt in the Database. Please update your controlled vocabularies." );
        }
        return newt;
    }

    private CvXrefQualifier getIdentity() throws IntactException {
        CvXrefQualifier identity = IntactContext.getCurrentInstance().getCvContext().getByMiRef( CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF );
        if ( identity == null ) {
            throw new IllegalStateException( "Could not find the qualifier(identity) in the Database. Please update your controlled vocabularies." );
        }
        return identity;
    }

    ///////////////////////
    // Private methods

    /**
     * Select a BioSource that has neither CvCellType nor CvTissue.
     *
     * @param bioSources the Collection of BioSource that potentially contains some having CvCellType or CvTissue.
     *
     * @return the unique BioSource that has neither CvCellType nor CvTissue
     *
     * @throws BioSourceServiceException if several of such BioSource are found in the database.
     */
    private BioSource getOriginalBioSource( Collection<BioSource> bioSources ) throws BioSourceServiceException {
        BioSource original = null;

        for ( BioSource bioSource : bioSources ) {
            if ( bioSource.getCvTissue() == null &&
                 bioSource.getCvCellType() == null ) {
                if ( original == null ) {
                    // first one is found
                    original = bioSource;
                } else {
                    // multiple bioSource found
                    String msg = "More than one BioSource with this taxId " + original.getTaxId() +
                                 "and CvTissue/CvCellType were found: " + original.getAc() + " and " +
                                 "" + bioSource.getAc();
                    log.error( msg );
                    throw new BioSourceServiceException( msg );
                }
            }
        }

        return original;
    }

    /**
     * Update the given BioSource with data taken from the provided TaxonomyService.
     * <p/>
     * It assumes that the taxid is existing in the given BioSource.
     *
     * @param taxid the taxid from which we want to get a Biosource
     *
     * @return an updated BioSource or null
     */
    private BioSource getBiosourceFromTaxonomyService( String taxid ) throws BioSourceServiceException {

        if ( taxid == null ) {
            throw new NullPointerException( "taxid must not be null." );
        }

        log.info( "Try to get BioSource data from Newt" );

        TaxonomyTerm term;
        try {
            term = taxonomyService.getTaxonomyTerm( Integer.parseInt( taxid ) );
        } catch ( TaxonomyServiceException e ) {
            throw new BioSourceServiceException( e );
        }

        BioSource bioSource = new BioSource( institution, term.getCommonName(), String.valueOf( term.getTaxid() ) );
        bioSource.setFullName( term.getScientificName() );

        // add xref to it !

        // retreive required objects
        CvDatabase newt = getNewt();
        CvXrefQualifier identity = getIdentity();

        // create identity Newt Xref
        BioSourceXref xref = new BioSourceXref( institution, newt, "" + taxid, null, null, identity );
        bioSource.addXref( xref );

        // Note: We do not persist that Xref as it will be used for checking against IntAct data.

        return bioSource;
    }

    /**
     * Search the IntAct database and retreive a BioSource having the given taxid and no CvCellType or CvTissue.
     *
     * @param taxid a non null taxid.
     *
     * @return a BioSource or null if none is found.
     */
    private BioSource searchIntactByTaxid( String taxid ) throws BioSourceServiceException {
        log.debug( "Searching in the database for BioSource(" + taxid + ")" );
        DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();

        IntactTransaction transaction = null;
        if ( !daoFactory.isTransactionActive() ) {
            log.debug( "There's no transaction active, start a local transaction." );
            transaction = daoFactory.beginTransaction();
        } else {
            log.debug( "There's already an ongoing transaction, we join it." );
        }


        BioSourceDao bsDao = daoFactory.getBioSourceDao();
        BioSource biosource = bsDao.getByTaxonIdUnique( taxid );

        if ( transaction != null ) {
            log.debug( "Commiting local transaction..." );
            try {
                transaction.commit();
            } catch ( IntactTransactionException e ) {
                throw new BioSourceServiceException( "Error while commiting transaction", e );
            }
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

//    private CvDatabase getDatabase( String dbMiRef ) {
//        if ( dbMiRef == null ) {
//            throw new NullPointerException( "dbMiRef must not be null." );
//        }
//
//        DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
//        CvObjectDao<CvDatabase> cvObjectDao = daoFactory.getCvObjectDao( CvDatabase.class );
//        CvDatabase db = cvObjectDao.getByPsiMiRef( dbMiRef );
//
//        return db;
//    }

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

        DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();

        // check if there is a transaction active, if so don't touch it, if not open a new one and close it when finished.
        IntactTransaction transaction = null;
        if ( !daoFactory.isTransactionActive() ) {
            log.debug( "There's no transaction active, start a local transaction." );
            transaction = daoFactory.beginTransaction();
        } else {
            log.debug( "There's already an ongoing transaction, we join it." );
        }

        // Instanciate it
        BioSource bioSource = new BioSource( institution, shortlabel, taxid );
        bioSource.setFullName( fullname );

        // Add Newt Xref

        // get source database
        CvObjectDao<CvDatabase> dbDao = daoFactory.getCvObjectDao( CvDatabase.class );
        String miRef = taxonomyService.getSourceDatabaseMiRef();
        CvDatabase db = dbDao.getByPsiMiRef( miRef );
        if ( db == null ) {
            String name = taxonomyService.getClass().getSimpleName();
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

        if ( transaction != null ) {
            log.debug( "Commiting local transaction..." );
            try {
                transaction.commit();
            } catch ( IntactTransactionException e ) {
                throw new BioSourceServiceException( "Error while commiting transaction", e );
            }
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

    /**
     * Create or update a BioSource object from a taxid.
     *
     * @param aTaxId The tax id to create/update a biosource for
     *
     * @return a valid, persistent BioSource
     */
//    private BioSource getValidBioSource( String aTaxId ) throws IntactException, BioSourceServiceException {
//
//        BioSourceDao bioSourceDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getBioSourceDao();
//
//        int taxid = 0;
//        try {
//            taxid = Integer.parseInt( aTaxId );
//        } catch ( NumberFormatException e ) {
//            throw new IntactException( "A taxid must be a integer value.", e );
//        }
//
//        // If a valid BioSource object already exists, return it.
////        if ( bioSourceCache.containsKey( aTaxId ) ) {
////            return ( BioSource ) bioSourceCache.get( aTaxId );
////        }
//
//        // Get all existing BioSources with aTaxId
//        Collection currentBioSources = bioSourceDao.getByTaxonId( aTaxId );
//
//        log.info( currentBioSources.size() + " BioSource found for " + aTaxId );
//
//        if ( null == currentBioSources ) {
//            throw new IntactException( "Search for a BioSource having the taxId: " + aTaxId + " failed." );
//        }
//
//        BioSource intactBioSource = null;
//        if ( currentBioSources.size() > 0 ) {
//            intactBioSource = getOriginalBioSource( currentBioSources );
//        }
//
//        // The verified BioSource
//        BioSource newBioSource = null;
//
//        // Get a correct BioSource from Newt
//        // we could have created our own biosource like 'in vitro' ... in which case, newt is unaware of it !
//        BioSource validBioSource = null;
//        if ( taxid != -1 ) {
//            validBioSource = getBiosourceFromTaxonomyService( aTaxId );
//        }
//
//        if ( null == validBioSource && intactBioSource == null ) {
//
//            if ( taxid == -1 ) {
//                // special case: in vitro was defined as taxid -1.
//                BioSource inVitro = new BioSource( institution, "in-vitro", aTaxId );
//
//                bioSourceDao.persist( inVitro );
//
//                CvDatabase newt = getNewt();
//                CvXrefQualifier identity = getIdentity();
//                BioSourceXref xref = new BioSourceXref( institution, newt, aTaxId, identity );
//                inVitro.addXref( xref );
//
//                IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getXrefDao().persist( xref );
//
//                newBioSource = inVitro;
//
//            } else {
//
//                log.error( "The taxId is invalid: " + aTaxId );
//                throw new IntactException( "The taxId is invalid: " + aTaxId );
//            }
//
//        } else if ( null == validBioSource && intactBioSource != null ) {
//
//            // we have a biosource in intact that Newt doesn't know about, return it.
//            log.error( "The taxId " + aTaxId + " was found in IntAct but doesn't exists in Newt." );
//            newBioSource = intactBioSource;
//
//        } else if ( null != validBioSource && intactBioSource == null ) {
//
//            // newt biosource has been found and nothing in intact.
//            // chech if the given taxid was obsolete.
//            if ( validBioSource.getTaxId().equals( aTaxId ) ) {
//
//                // not in IntAct and found in Newt so make it persistent in IntAct
//                IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getBioSourceDao().persist( validBioSource );
//                newBioSource = validBioSource;
//
//            } else {
//                // the given taxid was obsolete, check if intact contains already a biosource for the new taxid.
//
//                // both were found but different taxid, ie. taxid was obsolete.
//                final String newTaxid = validBioSource.getTaxId();
//                Collection<BioSource> bioSources = bioSourceDao.getByTaxonId( aTaxId );
//
//                switch ( bioSources.size() ) {
//                    case 0:
//                        // doesn't exists, so create it.
//                        log.info( "Creating new bioSource(" + newTaxid + ")." );
//                        bioSourceDao.persist( validBioSource );
//                        newBioSource = validBioSource;
//
//                        // cache the new taxid as well.
////                        bioSourceCache.put( newTaxid, newBioSource );
//                        break;
//
//                    case 1:
//                        // it exists, try to update it.
//                        BioSource intactBs = ( BioSource ) bioSources.iterator().next();
//                        log.info( "Updating existing BioSource (" + newTaxid + ")" );
//                        newBioSource = updateBioSource( intactBs, validBioSource );
//
//                        // cache the new taxid as well.
////                        bioSourceCache.put( newTaxid, newBioSource );
//                        break;
//
//                    default:
//                        // more than one !
//                        log.error( "More than one BioSource with this taxId found: " + aTaxId +
//                                   ". Check for the original one." );
//
//                        newBioSource = getOriginalBioSource( bioSources ); // fail if more than one !
//                }
//            }
//
//        } else {
//
//            // BioSource found in IntAct AND in Newt.
//
//            if ( !intactBioSource.equals( validBioSource ) ) {
//                log.info( "Updating existing BioSource (" + validBioSource.getTaxId() + ")" );
//                if ( validBioSource.getTaxId().equals( aTaxId ) ) {
//
//                    // given taxid was ok
//                    newBioSource = updateBioSource( intactBioSource, validBioSource );
//
//                } else {
//
//                    // The given taxid was obsolete.
//                    // (!) It could be a problem if the taxid was obsolete, and there is already a biosource
//                    // in intact with the new taxid. In which case, we can't just update or two BioSources
//                    // will have the same taxid.
//                    final String newTaxid = validBioSource.getTaxId();
//                    Collection<BioSource> bioSources = bioSourceDao.getByTaxonId( aTaxId );
//
//                    switch ( bioSources.size() ) {
//                        case 0:
//                            // doesn't exists, so create it.
//                            log.info( "Creating new bioSource(" + newTaxid + ")." );
//                            bioSourceDao.persist( validBioSource );
//                            newBioSource = validBioSource;
//
//                            try {
//                                // retreive required objects
//                                CvDatabase newt = getNewt();
//                                CvXrefQualifier identity = getIdentity();
//
//                                // create identity Newt Xref
//                                BioSourceXref xref = new BioSourceXref( institution, newt, newTaxid, null, null, identity );
//                                newBioSource.addXref( xref );
//
//                                // persist changes
//                                IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getXrefDao().update( xref );
//
//                            } catch ( IntactException e ) {
//                                log.error( "An error occured when trying to add Newt Xref to " + newBioSource, e );
//                            }
//
//                            break;
//
//                        case 1:
//                            // it exists, try to update it.
//                            BioSource intactBs = ( BioSource ) bioSources.iterator().next();
//                            log.info( "Updating existing BioSource (" + newTaxid + ")" );
//                            newBioSource = updateBioSource( intactBs, validBioSource );
//
//                            break;
//
//                        default:
//                            // more than one !
//                            log.error( "More than one BioSource with this taxId found: " + aTaxId +
//                                       ". Check for the original one." );
//
//                            BioSource original = getOriginalBioSource( bioSources ); // fail if more than one !
//                            newBioSource = updateBioSource( original, validBioSource );
//                    }
//
//                    // cache the new taxid as well.
////                    bioSourceCache.put( newTaxid, newBioSource );
//                }
//
//            } else {
//                // intact biosource was up-to-date
//                newBioSource = intactBioSource;
//            }
//        }
//
//        // The bioSourceCache will also contain associations from obsolete taxIds to valid
//        // BioSource objects to avoid looking up the same obsolete Id over and over again.
////        bioSourceCache.put( aTaxId, newBioSource );
//
//        return newBioSource;
//    }

    /**
     * Try to update an existing IntAct BioSource from an other.
     * <p/>
     * Only the taxid is possibly updated. </p>
     *
     * @param bioSource     the IntAct BioSource
     * @param newtBioSource the one from which we get the up-to-date data
     *
     * @return an up-to-date IntAct BioSource
     *
     * @throws IntactException
     */
    private BioSource updateBioSource( BioSource bioSource, BioSource newtBioSource ) throws BioSourceServiceException {

        boolean needUpdate = false;

        // compare these two BioSources and update in case of differences
        String newtTaxid = newtBioSource.getTaxId();
        if ( false == bioSource.getTaxId().equals( newtTaxid ) ) {
            bioSource.setTaxId( newtTaxid );
            log.debug( "Obsolete taxid: taxid " + bioSource.getTaxId() +
                       " becomes " + newtTaxid );
            needUpdate = true;
        }

        // retreive required objects
        CvDatabase newt = getNewt();
        CvXrefQualifier identity = getIdentity();

        // get the Newt/identity Xref
        // Note: if the BioSource lacks the identity Xref, we create it !
        boolean foundNewtIdentityXref = false;
        for ( Iterator iterator = bioSource.getXrefs().iterator(); iterator.hasNext() && foundNewtIdentityXref == false; )
        {
            Xref xref = ( Xref ) iterator.next();

            CvXrefQualifier qualifier = xref.getCvXrefQualifier();

            if ( xref.getCvDatabase().equals( newt ) &&
                 ( qualifier != null && qualifier.equals( identity ) ) ) {
                // found it !
                foundNewtIdentityXref = true;
                if ( false == xref.getPrimaryId().equals( newtBioSource.getTaxId() ) ) {

                    log.debug( "The identity Xref for that BioSource was not set to the correct taxid, updating it..." );

                    xref.setPrimaryId( newtBioSource.getTaxId() );

                    // update the Xref.
                    IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getXrefDao().update( xref );

                    needUpdate = true;
                }
            }
        }

        if ( false == foundNewtIdentityXref ) {

            log.debug( "The identity Xref for that BioSource was missing, creating it..." );

            BioSourceXref xref = new BioSourceXref( institution, newt, newtBioSource.getTaxId(), null, null, identity );
            bioSource.addXref( xref );

            // persist changes
            IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getXrefDao().persist( xref );
        }

        /**
         * The IntAct shortlabel and fullName has to be maintained.
         * eg. for the taxid 4932 we have the shortlabel 'yeast' in the database where Newt have 's cerevisae'
         * If the shortlabel has been changed via the editor, that change is maintained.
         */
        if ( needUpdate ) {
            log.info( "update biosource (taxid=" + bioSource.getTaxId() + ")" );
            IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getBioSourceDao().update( bioSource );
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
