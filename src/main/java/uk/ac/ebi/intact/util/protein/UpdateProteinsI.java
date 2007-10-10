/**
 * Created by IntelliJ IDEA.
 * User: hhe
 * Date: Apr 11, 2003
 * Time: 1:31:41 PM
 * To change this prediction use Options | File Templates.
 */
package uk.ac.ebi.intact.util.protein;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.business.IntactException;
import uk.ac.ebi.intact.context.CvContext;
import uk.ac.ebi.intact.context.IntactContext;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.util.HttpProxyManager;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Defines the functionality of protein import utilities.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id: UpdateProteinsI.java 8487 2007-05-30 08:52:30Z CatherineLeroy $
 */
@Deprecated
public abstract class UpdateProteinsI {

    private static final Log logger = LogFactory.getLog(UpdateProteinsI.class);

    private final static String CV_TOPIC_SEARCH_URL_ASCII = "search-url-ascii";

    public static class UpdateException extends RuntimeException {

        public UpdateException( String message ) {
            super( message );
        }
    }

    // cache useful object to avoid redoing queries

    /**
     * The owner of the created object
     */
    protected static Institution myInstitution;

    ////////////////////
    // Xref databases

    protected static CvDatabase uniprotDatabase;
    protected static String srsUrl;
    protected static CvDatabase intactDatabase;
    protected static CvDatabase sgdDatabase;
    protected static CvDatabase goDatabase;
    protected static CvDatabase interproDatabase;
    protected static CvDatabase flybaseDatabase;
    protected static CvDatabase reactomeDatabase;
    protected static CvDatabase hugeDatabase;

    /////////////////////////////////////////////////
    // Describe wether an Xref is related the primary
    // SPTR AC (identityCrefQualifier) or
    // not (secondaryXrefQualifier)
    protected static CvXrefQualifier identityXrefQualifier;
    protected static CvXrefQualifier secondaryXrefQualifier;

    protected static CvXrefQualifier isoFormParentXrefQualifier;

    protected static CvTopic isoformComment;
    protected static CvTopic noUniprotUpdate;

    protected static CvAliasType isoformSynonym;
    protected static CvAliasType geneNameAliasType;
    protected static CvAliasType geneNameSynonymAliasType;
    protected static CvAliasType orfNameAliasType;
    protected static CvAliasType locusNameAliasType;

    protected static CvInteractorType proteinType;

    protected BioSourceFactory bioSourceFactory;

    protected boolean offlineMode;

    /**
     * If true, each protein is updated in a distinct transaction. If localTransactionControl is false, no local
     * transactions are initiated, control is left with the calling class. This can be used e.g. to have transactions
     * span the insertion of all proteins of an entire complex. Default is true.
     */
    protected static boolean localTransactionControl = false;

    // Keeps eventual parsing error while the processing is carried on
    protected Map<Integer,Exception> parsingExceptions = new HashMap<Integer,Exception>();


    //////////////////////////////////
    // Constructors
    // todo : change managing of connection should be done outside
    public UpdateProteinsI( boolean offlineMode, boolean setOutputOn )
    {
        this.offlineMode = offlineMode;

        try {
            if ( setOutputOn ) {
                HttpProxyManager.setup();
            } else {
                HttpProxyManager.setup( null );
            }

        } catch ( HttpProxyManager.ProxyConfigurationNotFound proxyConfigurationNotFound ) {
            //proxyConfigurationNotFound.printStackTrace();
            logger.warn("Proxy configuration not found: "+ proxyConfigurationNotFound.getMessage());
        }

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        collectDefaultObject( );

        bioSourceFactory = new BioSourceFactory(  );
    }

    /**
     * @param cacheSize the number of valid biosource to cache during the update process.
     *
     * @throws UpdateException
     */
    public UpdateProteinsI(  int cacheSize, boolean offlineMode ) throws UpdateException {
        this( offlineMode, true );


        bioSourceFactory = new BioSourceFactory( myInstitution, cacheSize, offlineMode );
    }


    /**
     * Default constructor which initialize the bioSource cache to default.
     *
     * @throws UpdateException
     */
    public UpdateProteinsI( ) throws UpdateException {
        this(false, true);
    }


    //////////////////////////////////
    // Methods

    private void collectDefaultObject()
    {

        try
        {
            myInstitution = IntactContext.getCurrentInstance().getInstitution();
        }
        catch (IntactException e)
        {
            e.printStackTrace();
        }

        CvContext cvContext = IntactContext.getCurrentInstance().getCvContext();

        /**
         * Load CVs
         */

        sgdDatabase = cvContext.getByMiRef(CvDatabase.class, CvDatabase.SGD_MI_REF); // sgd
        uniprotDatabase = cvContext.getByMiRef(CvDatabase.class, CvDatabase.UNIPROT_MI_REF); // uniprot

        // search for the SRS link.
        if (!offlineMode)
        {

            Collection<Annotation> annotations = uniprotDatabase.getAnnotations();
            if (annotations != null)
            {
                // find the CvTopic search-url-ascii
                Annotation searchedAnnotation = null;
                for (Iterator<Annotation> iterator = annotations.iterator(); iterator.hasNext() && searchedAnnotation == null;)
                {
                    Annotation annotation = iterator.next();
                    String annotCvTopicLabel = annotation.getCvTopic().getShortLabel();
                    if (CV_TOPIC_SEARCH_URL_ASCII.equals(annotCvTopicLabel))
                    {
                        searchedAnnotation = annotation;
                    }
                }

                if (searchedAnnotation != null)
                {
                    srsUrl = searchedAnnotation.getAnnotationText();
                    if (logger != null)
                    {
                        logger.info("Found UniProt URL in the Uniprot CvDatabase: " + srsUrl);
                    }
                }
                else
                {
                    String msg = "Unable to find an annotation having a CvTopic: " + CV_TOPIC_SEARCH_URL_ASCII +
                            " in the UNIPROT database";
                    if (logger != null)
                    {
                        logger.error(msg);
                    }
                    throw new UpdateException(msg);
                }
            }
            else
            {
                String msg = "No Annotation in the UNIPROT database, could not get the UniProt URL.";
                if (logger != null)
                {
                    logger.error(msg);
                }
                throw new UpdateException(msg);
            }
        }

        intactDatabase = cvContext.getByMiRef(CvDatabase.class, CvDatabase.INTACT_MI_REF);
        goDatabase = cvContext.getByMiRef(CvDatabase.class, CvDatabase.GO_MI_REF);
        interproDatabase = cvContext.getByMiRef(CvDatabase.class, CvDatabase.INTERPRO_MI_REF);
        flybaseDatabase = cvContext.getByMiRef(CvDatabase.class, CvDatabase.FLYBASE_MI_REF);
        reactomeDatabase = cvContext.getByMiRef(CvDatabase.class, CvDatabase.REACTOME_PROTEIN_PSI_REF);
        hugeDatabase = cvContext.getByMiRef(CvDatabase.class, CvDatabase.HUGE_MI_REF);

        identityXrefQualifier = cvContext.getByMiRef(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF);
        secondaryXrefQualifier = cvContext.getByMiRef(CvXrefQualifier.class, CvXrefQualifier.SECONDARY_AC_MI_REF);
        isoFormParentXrefQualifier = cvContext.getByMiRef(CvXrefQualifier.class, CvXrefQualifier.ISOFORM_PARENT_MI_REF);

        // only one search by shortlabel as it still doesn't have MI number.
        isoformComment = cvContext.getByLabel(CvTopic.class, CvTopic.ISOFORM_COMMENT);
        noUniprotUpdate = cvContext.getByLabel(CvTopic.class, CvTopic.NON_UNIPROT);


        geneNameAliasType = cvContext.getByMiRef(CvAliasType.class, CvAliasType.GENE_NAME_MI_REF);
        geneNameSynonymAliasType = cvContext.getByMiRef(CvAliasType.class, CvAliasType.GENE_NAME_SYNONYM_MI_REF);
        isoformSynonym = cvContext.getByMiRef(CvAliasType.class, CvAliasType.ISOFORM_SYNONYM_MI_REF);
        locusNameAliasType = cvContext.getByMiRef(CvAliasType.class, CvAliasType.LOCUS_NAME_MI_REF);
        orfNameAliasType = cvContext.getByMiRef(CvAliasType.class, CvAliasType.ORF_NAME_MI_REF);

        proteinType = (CvInteractorType) cvContext.getByMiRef(CvInteractorType.class, CvInteractorType.getProteinMI());


    }

    /**
     * Gives all Exceptions that have been raised during the last processing.
     *
     * @return a map Entry Count ---> Exception. It can be null.
     */
    public Map getParsingExceptions() {
        return parsingExceptions;
    }

    /**
     * Inserts zero or more proteins created from SPTR entries which are retrieved from a Stream. IntAct Protein objects
     * represent a specific amino acid sequence in a specific organism. If a SPTr entry contains more than one organism,
     * one IntAct entry will be created for each organism, unless the taxid parameter is not null.
     *
     * @param inputStream The straem from which YASP will read the ENtries content.
     * @param taxid       Of all entries retrieved from sourceURL, insert only those which have this taxid. If taxid is
     *                    empty, insert all protein objects.
     * @param update      If true, update existing Protein objects according to the retrieved data. else, skip existing
     *                    Protein objects.
     *
     * @return Collection of protein objects created/updated.
     */
    public abstract Collection insertSPTrProteins( InputStream inputStream, String taxid, boolean update );

    /**
     * Inserts zero or more proteins created from SPTR entries which are retrieved from a URL. IntAct Protein objects
     * represent a specific amino acid sequence in a specific organism. If a SPTr entry contains more than one organism,
     * one IntAct entry will be created for each organism, unless the taxid parameter is not null.
     *
     * @param sourceUrl The URL which delivers zero or more SPTR flat file formatted entries.
     * @param taxid     Of all entries retrieved from sourceURL, insert only those which have this taxid. If taxid is
     *                  empty, insert all protein objects.
     * @param update    If true, update existing Protein objects according to the retrieved data. else, skip existing
     *                  Protein objects.
     *
     * @return The number of protein objects created.
     */
    public abstract int insertSPTrProteinsFromURL( String sourceUrl, String taxid, boolean update );

    /**
     * Inserts zero or more proteins created from SPTR entries which are retrieved from an SPTR Accession number. IntAct
     * Protein objects represent a specific amino acid sequence in a specific organism. If a SPTr entry contains more
     * than one organism, one IntAct entry will be created for each organism.
     *
     * @param proteinAc SPTR Accession number of the protein to insert/update
     *
     * @return a set of created/updated protein.
     */
    public abstract Collection insertSPTrProteins( String proteinAc );

    /**
     * Inserts zero or more proteins created from SPTR entries which are retrieved from an SPTR Accession number. IntAct
     * Protein objects represent a specific amino acid sequence in a specific organism. If a SPTr entry contains more
     * than one organism, one IntAct entry will be created for each organism.
     *
     * @param proteinAc SPTR Accession number of the protein to insert/update
     * @param taxId     The tax id the protein should have
     * @param update    If true, update existing Protein objects according to the retrieved data. else, skip existing
     *                  Protein objects.
     *
     * @return a set of created/updated protein.
     */
    public abstract Collection insertSPTrProteins( String proteinAc, String taxId, boolean update );

    /**
     * Creates a simple Protein object for entries which are not in SPTR. The Protein will more or less only contain the
     * crossreference to the source database.
     *
     * @param anAc      The primary identifier of the protein in the external database.
     * @param aDatabase The database in which the protein is listed.
     * @param aTaxId    The tax id the protein should have
     *
     * @return the protein created or retrieved from the IntAct database
     */
    public abstract Protein insertSimpleProtein( String anAc, CvDatabase aDatabase, String aTaxId )
            throws IntactException;

    /**
     * From a given sptr AC, returns a full URL from where a flatfile format SPTR entry will be fetched. Note, the SRS
     * has several format of data output, the URLs which outputs html format SPTR entry CANNOT be used, since YASP
     * does't have html parsing function.
     *
     * @param sptrAC a SPTR AC
     *
     * @return a full URL.
     */
    public abstract String getUrl( String sptrAC );

    /**
     * add (not update) a new Xref to the given Annotated object and write it in the database.
     *
     * @param current the object to which we add a new Xref
     * @param xref    the Xref to add to the AnnotatedObject
     *
     * @return true if the object as been added, else false.
     */
    public abstract boolean addNewXref( AnnotatedObject current, final Xref xref );

    /**
     * add (not update) a new Xref to the given Annotated object and write it in the database.
     *
     * @param current the object to which we add a new Xref
     * @param alias   the Alias to add to the AnnotatedObject
     */
    public abstract boolean addNewAlias( AnnotatedObject current, final InteractorAlias alias );

    /**
     * Gives the count of created protein
     *
     * @return created protein count
     */
    public abstract int getProteinCreatedCount();

    /**
     * Gives the count of updated protein
     *
     * @return updated protein count
     */
    public abstract int getProteinUpdatedCount();

    /**
     * Gives the count of up-to-date protein (i.e. existing in IntAct but don't need to be updated)
     *
     * @return up-to-date protein count
     */
    public abstract int getProteinUpToDateCount();

    /**
     * Gives the count of all potential protein (i.e. for an SPTREntry, we can create/update several IntAct protein. One
     * by entry's taxid)
     *
     * @return potential protein count
     */
    public abstract int getProteinCount();

    /**
     * Gives the count of protein which gaves us errors during the processing.
     *
     * @return protein skipped count
     */
    public abstract int getProteinSkippedCount();

    /**
     * Gives the count of created splice variant
     *
     * @return created protein count
     */
    public abstract int getSpliceVariantCreatedCount();

    /**
     * Gives the count of updated splice variant
     *
     * @return updated protein count
     */
    public abstract int getSpliceVariantUpdatedCount();

    /**
     * Gives the count of up-to-date splice variant (i.e. existing in IntAct but don't need to be updated)
     *
     * @return up-to-date protein count
     */
    public abstract int getSpliceVariantUpToDateCount();

    /**
     * Gives the count of all potential splice variant (i.e. for an SPTREntry, we can create/update several IntAct
     * protein. One by entry's taxid)
     *
     * @return potential protein count
     */
    public abstract int getSpliceVariantCount();

    /**
     * Gives the count of splice variant which gaves us errors during the processing.
     *
     * @return splice variant skipped count
     */
    public abstract int getSpliceVariantSkippedCount();

    /**
     * Gives the number of entry found in the given URL
     *
     * @return entry count
     */
    public abstract int getEntryCount();

    /**
     * Gives the number of entry successfully processed.
     *
     * @return entry successfully processed count.
     */
    public abstract int getEntryProcessededCount();

    /**
     * Gives the number of entry skipped during the process.
     *
     * @return skipped entry count.
     */
    public abstract int getEntrySkippedCount();

    /**
     * Allows to displays on the screen what's going on during the update process.
     *
     * @param debug <b>true</b> to enable, <b>false</b> to disable
     */
    public abstract void setDebugOnScreen( boolean debug );

    /**
     * return the filename in which have been saved all Entries which gaves us processing errors.
     *
     * @return the filename or null if not existing
     */
    public abstract String getErrorFileName();

    /**
     * If true, each protein is updated in a distinct transaction. If localTransactionControl is false, no local
     * transactions are initiated, control is left with the calling class. This can be used e.g. to have transctions
     * span the insertion of all proteins of an entire complex.
     *
     * @return current value of localTransactionControl
     */
    public abstract boolean isLocalTransactionControl();

    /**
     * If true, each protein is updated in a distinct transaction. If localTransactionControl is false, no local
     * transactions are initiated, control is left with the calling class. This can be used e.g. to have transctions
     * span the insertion of all proteins of an entire complex.
     *
     * @param localTransactionControl New value for localTransactionControl
     */
    public abstract void setLocalTransactionControl( boolean localTransactionControl );

    /**
     * Update all protein found in the provided collection. If none provided, update all protein that can be retreived
     * from the current intact node.
     *
     * @param proteins a Collection of protein.
     */
    public abstract int updateAllProteins( Collection<ProteinImpl> proteins ) throws IntactException;
}