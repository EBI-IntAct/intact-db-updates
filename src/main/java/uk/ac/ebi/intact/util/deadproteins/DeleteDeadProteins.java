/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.deadproteins;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.business.IntactTransactionException;
import uk.ac.ebi.intact.context.IntactContext;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.persistence.dao.CvObjectDao;
import uk.ac.ebi.intact.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.persistence.dao.ProteinDao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO comment this
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since <pre>16-Nov-2006</pre>
 */
public class DeleteDeadProteins {

    /**
     * Sets up a logger for that class.
     */
    public static final Log log = LogFactory.getLog( DeleteDeadProteins.class );

    /**
     * If true, doesn't alter the database, only output what would happen.
     */
    private boolean dryRun = false;

    /**
     * A file containing a list of Protein ids to delete.
     */
    private File proteinFilterFile;

    private CvXrefQualifier isoParent = null;
    private CvXrefQualifier identity = null;
    private CvDatabase intact = null;

    //////////////////////
    // Constructors

    public DeleteDeadProteins() {
        this( false );
    }

    public DeleteDeadProteins( boolean dryRun ) {
        this.dryRun = dryRun;
    }

    public DeleteDeadProteins( File proteinFilterFile ) {
        this( proteinFilterFile, false );
    }

    public DeleteDeadProteins( File proteinFilterFile, boolean dryRun ) {
        this.proteinFilterFile = proteinFilterFile;
        this.dryRun = dryRun;
    }

    ////////////////////////
    // Initialisation

    private void initControlledVocabularies() throws IntactTransactionException {

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
        ProteinDao proteinDao = daoFactory.getProteinDao();
        CvObjectDao<CvObject> cvDao = daoFactory.getCvObjectDao();

        // Loading of necessary CVs to the processing of these proteins
        isoParent = cvDao.getByPrimaryId( CvXrefQualifier.class, CvXrefQualifier.ISOFORM_PARENT_MI_REF );
        if ( isoParent == null ) {
            throw new IllegalStateException( "Could not find CvXrefQualifier( isoParent ) in the database. abort." );
        }

        identity = cvDao.getByPrimaryId( CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF );
        if ( isoParent == null ) {
            throw new IllegalStateException( "Could not find CvXrefQualifier( identity ) in the database. abort." );
        }

        intact = cvDao.getByPrimaryId( CvDatabase.class, CvDatabase.INTACT_MI_REF );
        if ( intact == null ) {
            throw new IllegalStateException( "Could not find CvDatabase( intact ) in the database. abort." );
        }

        try {
            IntactContext.getCurrentInstance().getDataContext().commitTransaction();
        } catch ( IntactTransactionException e ) {
            throw new RuntimeException( e );
        }
    }

    //////////////////////////
    // Business logic

    public int process() throws IOException, IntactTransactionException {
        int count = 0;

        initControlledVocabularies();

        if ( proteinFilterFile != null ) {
            log.debug( "Starting processing based on file filter provided..." );
            count = processFromFile();
        } else {
            log.debug( "Starting processing on the complete database..." );
            count = processWholeDatabase();
        }
        return count;
    }

    public int processFromFile() throws IOException, IntactTransactionException {

        // 1. load the list of proteins
        Collection<String> proteinsIds = parse( proteinFilterFile );
        log.debug( "Loaded " + proteinsIds.size() + " protein ID(s) from file: " + proteinFilterFile.getAbsolutePath() );

        // 2. iterate through and delete proteins not involved in an interaction
        //    This only apply if all respective parent and splice variant do not have any interaction either.
        int proteinDeletedCount = 0;

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
        ProteinDao proteinDao = daoFactory.getProteinDao();

        for ( String id : proteinsIds ) {

            log.debug( "----------------------------------------------------------" );
            log.debug( "Processing: " + id );
            List<ProteinImpl> proteins = proteinDao.getByXrefLike( id );
            log.debug( "Found " + proteins.size() + " protein(s)" );

            for ( ProteinImpl protein : proteins ) {
                proteinDeletedCount += process( protein, proteinDao );
            } // proteins
        } // ids

        try {
            IntactContext.getCurrentInstance().getDataContext().commitTransaction();
        } catch ( IntactTransactionException e ) {
            throw new RuntimeException( e );
        }

        return proteinDeletedCount;
    }

    public static final int CHUNK_SIZE = 500;

    public int processWholeDatabase() throws IntactTransactionException {
        int count = 0;

        DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        ProteinDao proteinDao = daoFactory.getProteinDao();
        final int proteinCount = proteinDao.countAll();
        proteinDao = null;
        try {
            IntactContext.getCurrentInstance().getDataContext().commitTransaction();
        } catch ( IntactTransactionException e ) {
            throw new RuntimeException( e );
        }

        // iterate over all proteins
        System.out.println( proteinCount + " proteins to process..." );
        int i = 0;
        List<ProteinImpl> proteins = null;


        while ( i < proteinCount ) {

            IntactContext.getCurrentInstance().getDataContext().beginTransaction();
            proteinDao = daoFactory.getProteinDao();

            System.out.println( "Processing proteins " + i + ".." + ( i + CHUNK_SIZE ) );
            proteins = proteinDao.getAll( i, CHUNK_SIZE );

            for ( ProteinImpl protein : proteins ) {

                log.debug( "- " + i + "/" + proteinCount + " ---------------------------------------------------------" );
                process( protein, proteinDao );

                i++;
            } // for proteins

            proteins.clear();
            proteins = null;

            // release the memory for that chunk of data.
            try {
                IntactContext.getCurrentInstance().getDataContext().commitTransaction();
            } catch ( IntactTransactionException e ) {
                throw new RuntimeException( e );
            }

        } // while

        return count;
    }

    private int process( ProteinImpl protein, ProteinDao proteinDao ) {

        log.debug( "Checking on " + protein.getShortLabel() + "(" + protein.getAc() + ")" );

        int count = 0;

        String parentAc = getIsoformParentAc( protein );
        if ( parentAc != null ) {
            // we are handling a splice variant
            if ( !hasInteraction( protein ) ) {
                count += deleteProtein( protein, proteinDao );
            }

        } else {
            // then a protein
            Collection<ProteinImpl> variants = getSpliceVariant( protein, proteinDao );

            log.debug( "Found " + variants.size() + " splice variants associated." );

            boolean hasInteraction = hasInteraction( protein );
            boolean hasVariantWithInteraction = false;

            for ( ProteinImpl variant : variants ) {
                if ( hasInteraction( variant ) ) {
                    log.debug( "Splice variant " + variant.getShortLabel() + " (" + variant.getAc() + ") has interactions" );
                    hasVariantWithInteraction = true;
                } else {
                    // no interaction, we delete
                    log.debug( "Splice variant " + variant.getShortLabel() + " (" + variant.getAc() + ") has no interactions" );
                    count += deleteProtein( variant, proteinDao );
                }
            } // variants


            if ( !hasInteraction && !hasVariantWithInteraction ) {

                log.debug( "This protein has no interaction or any splice variant having interactions, will delete it." );
                count += deleteProtein( protein, proteinDao );

            } else {
                if ( log.isDebugEnabled() ) {

                    String info = protein.getShortLabel() + "(" + protein.getAc() + ")";

                    if ( hasInteraction ) {
                        log.debug( "No deletion of " + info + ", it has interactions." );
                    }

                    if ( hasVariantWithInteraction ) {
                        log.debug( "No deletion of " + info + ", it has at least a splice variant with interactions." );
                    }
                }
            }
        }

        return count;
    }

    ///////////////////////
    // Utilities

    private Collection<String> parse( File proteinFile ) throws IOException {

        if ( proteinFile == null ) {
            throw new IllegalArgumentException( "You must give a non null file." );
        }

        if ( !proteinFile.canRead() ) {
            throw new IllegalArgumentException( "Cannot read file: " + proteinFile.getAbsolutePath() );
        }

        Collection<String> ids = new ArrayList<String>();

        BufferedReader in = new BufferedReader( new FileReader( proteinFile ) );
        String line;
        while ( ( line = in.readLine() ) != null ) {
            // process line here
            line = line.trim();
            if ( line.length() == 0 || line.startsWith( "#" ) ) {
                continue;
            }

            ids.add( line );
        }
        in.close();

        return ids;
    }

    private String getIsoformParentAc( Protein protein ) {
        for ( Xref xref : protein.getXrefs() ) {
            if ( isoParent.equals( xref.getCvXrefQualifier() ) ) {
                return xref.getPrimaryId();
            }
        }
        return null;
    }

    private int deleteProtein( ProteinImpl protein, ProteinDao proteinDao ) {

        log.debug( "Deleting protein: " + protein.getShortLabel() + "(" + protein.getAc() + ")" );
        if ( !dryRun ) {
            proteinDao.delete( protein );
        }
        return 1;
    }

    private boolean hasInteraction( Protein protein ) {
        return !protein.getActiveInstances().isEmpty();
    }

    private Collection<ProteinImpl> getSpliceVariant( ProteinImpl protein, ProteinDao proteinDao ) {
        List<ProteinImpl> variants = proteinDao.getByXrefLike( intact, isoParent, protein.getAc() );
        return variants;
    }

    ////////////////////////
    // Getters and Setters

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun( boolean dryRun ) {
        this.dryRun = dryRun;
    }
}