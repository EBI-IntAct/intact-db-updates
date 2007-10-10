/*
Copyright (c) 2002 The European Bioinformatics Institute, and others.
 All rights reserved. Please see the file LICENSE
in the root directory of this distribution.
*/
package uk.ac.ebi.intact.util.protein;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.business.IntactException;
import uk.ac.ebi.intact.context.IntactContext;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.persistence.dao.BioSourceDao;
import uk.ac.ebi.intact.util.NewtServerProxy;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

/**
 * That class his hidding the logic which allow to get a valid biosource from a taxid.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id: BioSourceFactory.java 7200 2006-12-16 18:17:18Z baranda $
 */
@Deprecated
public class BioSourceFactory {

    /**
     * Sets up a logger for that class.
     */
    public static final Log log = LogFactory.getLog( BioSourceFactory.class );

    /**
     * The institution to which we have to link all new BioSource
     */
    private Institution institution;

    /**
     * Cache valid BioSource objects for taxIds.
     */
    private static LRUMap bioSourceCache;
    private final static int DEFAULT_CACHE_SIZE = 200;

    private boolean offlineMode;

    /**
     * To retreive up to date biosource data
     */
    private static NewtServerProxy newtProxy;
    private static final String NEWT_URL = "http://www.ebi.ac.uk/newt/display";

    public BioSourceFactory( ) throws IntactException {
        this( IntactContext.getCurrentInstance().getInstitution(), DEFAULT_CACHE_SIZE, false );
    }

    public BioSourceFactory( boolean offlineMode ) throws IntactException {
        this( IntactContext.getCurrentInstance().getInstitution(), DEFAULT_CACHE_SIZE, offlineMode );
    }

    public BioSourceFactory( Institution institution, int cacheSize, boolean offlineMode ) {

        if ( institution == null ) {
            throw new RuntimeException( "The institution must not be null!" );
        }

        this.institution = institution;
        this.offlineMode = offlineMode;

        URL url = null;
        try {
            url = new URL( NEWT_URL );
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( "Unable to create Newt proxy, malformed URL: " + url );
        }

        newtProxy = new NewtServerProxy( url );
        newtProxy.disableCaching();

        bioSourceCache = new LRUMap( cacheSize );
    }

    /**
     * Select a BioSource that has neither CvCellType nor CvTissue.
     *
     * @param bioSources the Collection of BioSource that potentially contains some having CvCellType or CvTissue.
     *
     * @return the unique BioSource that has neither CvCellType nor CvTissue
     *
     * @throws IntactException if several of such BioSource are found
     */
    private BioSource getOriginalBioSource( Collection<BioSource> bioSources ) throws IntactException {
        BioSource original = null;

        for ( BioSource bioSource : bioSources ) {
            if ( bioSource.getCvTissue() == null &&
                 bioSource.getCvCellType() == null ) {
                if ( original == null ) {
                    // first on is found
                    original = bioSource;
                } else {
                    // multiple bioSource, error.
                    log.error( "More than one BioSource with this taxId found: " + original.getTaxId() );
                    throw new IntactException( "More than one BioSource with this taxId found: " + original.getTaxId() );
                }
            }
        }
        return original;
    }

    /**
     * Returns CvDatabase(newt)
     *
     * @return
     *
     * @throws IntactException
     */
    private CvDatabase getNewt() throws IntactException {
        CvDatabase newt = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvDatabase.class, CvDatabase.NEWT_MI_REF);
        if ( newt == null ) {
            throw new IllegalStateException( "Could not find newt in the Database. Please update your controlled vocabularies." );
        }
        return newt;
    }

    private CvXrefQualifier getIdentity() throws IntactException {
        CvXrefQualifier identity = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF);
        if ( identity == null ) {
            throw new IllegalStateException( "Could not find the qualifier(identity) in the Database. Please update your controlled vocabularies." );
        }
        return identity;
    }

    /**
     * Create or update a BioSource object from a taxid.
     *
     * @param aTaxId The tax id to create/update a biosource for
     *
     * @return a valid, persistent BioSource
     */
    public BioSource getValidBioSource( String aTaxId ) throws IntactException {

        BioSourceDao bioSourceDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getBioSourceDao();

        int taxid = 0;
        try {
            taxid = Integer.parseInt( aTaxId );
        } catch ( NumberFormatException e ) {
            throw new IntactException( "A taxid must be a integer value.", e );
        }

        // If a valid BioSource object already exists, return it.
        if ( bioSourceCache.containsKey( aTaxId ) ) {
            return (BioSource) bioSourceCache.get( aTaxId );
        }

        // Get all existing BioSources with aTaxId
        Collection currentBioSources = bioSourceDao.getByTaxonId( aTaxId );

        log.info( currentBioSources.size() + " BioSource found for " + aTaxId );

        if ( null == currentBioSources ) {
            throw new IntactException( "Search for a BioSource having the taxId: " + aTaxId + " failed." );
        }

        BioSource intactBioSource = null;
        if ( currentBioSources.size() > 0 ) {
            intactBioSource = getOriginalBioSource( currentBioSources );
        }

        // The verified BioSource
        BioSource newBioSource = null;

        // Get a correct BioSource from Newt
        // we could have created our own biosource like 'in vitro' ... in which case, newt is unaware of it !
        BioSource validBioSource = null;
        if ( taxid != -1 ) {
            validBioSource = getNewtBiosource( aTaxId );
        }

        if ( null == validBioSource && intactBioSource == null ) {

            if ( taxid == -1 ) {
                // special case: in vitro was defined as taxid -1.
                BioSource inVitro = new BioSource( institution, "in-vitro", aTaxId );

                bioSourceDao.persist( inVitro );

                CvDatabase newt = getNewt();
                CvXrefQualifier identity = getIdentity();
                BioSourceXref xref = new BioSourceXref( institution, newt, aTaxId, identity );
                inVitro.addXref( xref );

                IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getXrefDao().persist( xref );

                newBioSource = inVitro;

            } else {

                log.error( "The taxId is invalid: " + aTaxId );
                throw new IntactException( "The taxId is invalid: " + aTaxId );
            }

        } else if ( null == validBioSource && intactBioSource != null ) {

            // we have a biosource in intact that Newt doesn't know about, return it.
            log.error( "The taxId " + aTaxId + " was found in IntAct but doesn't exists in Newt." );
            newBioSource = intactBioSource;

        } else if ( null != validBioSource && intactBioSource == null ) {

            // newt biosource has been found and nothing in intact.
            // chech if the given taxid was obsolete.
            if ( validBioSource.getTaxId().equals( aTaxId ) ) {

                // not in IntAct and found in Newt so make it persistent in IntAct
                IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getBioSourceDao().persist( validBioSource );
                newBioSource = validBioSource;

            } else {
                // the given taxid was obsolete, check if intact contains already a biosource for the new taxid.

                // both were found but different taxid, ie. taxid was obsolete.
                final String newTaxid = validBioSource.getTaxId();
                Collection<BioSource> bioSources = bioSourceDao.getByTaxonId( aTaxId );

                switch ( bioSources.size() ) {
                    case 0:
                        // doesn't exists, so create it.
                        log.info( "Creating new bioSource(" + newTaxid + ")." );
                        bioSourceDao.persist( validBioSource );
                        newBioSource = validBioSource;

                        // cache the new taxid as well.
                        bioSourceCache.put( newTaxid, newBioSource );
                        break;

                    case 1:
                        // it exists, try to update it.
                        BioSource intactBs = (BioSource) bioSources.iterator().next();
                        log.info( "Updating existing BioSource (" + newTaxid + ")" );
                        newBioSource = updateBioSource( intactBs, validBioSource );

                        // cache the new taxid as well.
                        bioSourceCache.put( newTaxid, newBioSource );
                        break;

                    default:
                        // more than one !
                        log.error( "More than one BioSource with this taxId found: " + aTaxId +
                                   ". Check for the original one." );

                        newBioSource = getOriginalBioSource( bioSources ); // fail if more than one !
                }
            }

        } else {

            // BioSource found in IntAct AND in Newt.

            if ( !intactBioSource.equals( validBioSource ) ) {
                log.info( "Updating existing BioSource (" + validBioSource.getTaxId() + ")" );
                if ( validBioSource.getTaxId().equals( aTaxId ) ) {

                    // given taxid was ok
                    newBioSource = updateBioSource( intactBioSource, validBioSource );

                } else {

                    // The given taxid was obsolete.
                    // (!) It could be a problem if the taxid was obsolete, and there is already a biosource
                    // in intact with the new taxid. In which case, we can't just update or two BioSources
                    // will have the same taxid.
                    final String newTaxid = validBioSource.getTaxId();
                    Collection<BioSource> bioSources = bioSourceDao.getByTaxonId( aTaxId );

                    switch ( bioSources.size() ) {
                        case 0:
                            // doesn't exists, so create it.
                            log.info( "Creating new bioSource(" + newTaxid + ")." );
                            bioSourceDao.persist( validBioSource );
                            newBioSource = validBioSource;

                            try {
                                // retreive required objects
                                CvDatabase newt = getNewt();
                                CvXrefQualifier identity = getIdentity();

                                // create identity Newt Xref
                                BioSourceXref xref = new BioSourceXref( institution, newt, newTaxid, null, null, identity );
                                newBioSource.addXref( xref );

                                // persist changes
                                IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getXrefDao().update( xref );

                            } catch ( IntactException e ) {
                                log.error( "An error occured when trying to add Newt Xref to " + newBioSource, e );
                            }

                            break;

                        case 1:
                            // it exists, try to update it.
                            BioSource intactBs = (BioSource) bioSources.iterator().next();
                            log.info( "Updating existing BioSource (" + newTaxid + ")" );
                            newBioSource = updateBioSource( intactBs, validBioSource );

                            break;

                        default:
                            // more than one !
                            log.error( "More than one BioSource with this taxId found: " + aTaxId +
                                       ". Check for the original one." );

                            BioSource original = getOriginalBioSource( bioSources ); // fail if more than one !
                            newBioSource = updateBioSource( original, validBioSource );
                    }

                    // cache the new taxid as well.
                    bioSourceCache.put( newTaxid, newBioSource );
                }

            } else {
                // intact biosource was up-to-date
                newBioSource = intactBioSource;
            }
        }

        // The bioSourceCache will also contain associations from obsolete taxIds to valid
        // BioSource objects to avoid looking up the same obsolete Id over and over again.
        bioSourceCache.put( aTaxId, newBioSource );

        return newBioSource;
    } // getValidBioSource

    /**
     * Gives a valid taxid.
     *
     * @param taxid the original taxid
     *
     * @return a valid taxid (can be different from the original in case of obsoletness).
     *
     * @throws IntactException if an error occur when accessing IntAct or an inconstistancy.
     */
    public String getUpToDateTaxid( final String taxid ) throws IntactException {

        if ( taxid == null ) {
            return null;
        }
        BioSource validBioSource = getValidBioSource( taxid );
        return validBioSource.getTaxId();
    }

    /**
     * Update the given BioSource with data taken from Newt.<br> it assumes that the taxid is existing in the given
     * BioSource.
     *
     * @param taxid the taxid from which we want to get a Biosource
     *
     * @return an updated BioSource or null
     */
    private BioSource getNewtBiosource( String taxid ) throws IntactException {

        if ( taxid == null ) {
            return null;
        }

        log.info( "Try to get BioSource data from Newt" );
        NewtServerProxy.NewtResponse response = null;

        String shortLabel;
        String fullName;

        if (!offlineMode)
        {
            try
            {
                response = newtProxy.query(Integer.parseInt(taxid));
            }
            catch (IOException e)
            {
                log.error("Could not access the Newt web server.", e);
                return null;
            }
            catch (NumberFormatException e)
            {
                log.error("invalid taxid: " + taxid, e);
                return null;
            }
            catch (NewtServerProxy.TaxIdNotFoundException e)
            {
                log.error("taxId not found from Newt: " + taxid, e);
                return null;
            }

            shortLabel = response.getShortLabel();
            fullName = response.getFullName();
        }
        else
        {
            log.warn("Running in offline mode. No access to Newt attempted.");

            String offlineName = "unknown-"+System.currentTimeMillis();
            shortLabel = offlineName;
            fullName = "Offline name: "+offlineName;
        }

        // the taxId can be different in obsoleteness case.
        BioSource bioSource = new BioSource( institution,
                                             shortLabel,
                                             "" + taxid );
        bioSource.setFullName( fullName );

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
    private BioSource updateBioSource( BioSource bioSource,
                                       BioSource newtBioSource ) throws IntactException {

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
            Xref xref = (Xref) iterator.next();

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

            try {
                IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getBioSourceDao().update( bioSource );
            } catch ( IntactException ie ) {
                throw ie;
            }
        }

        return bioSource;
    } // updateBioSource
}
