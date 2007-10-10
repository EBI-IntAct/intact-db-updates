/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.protein.utils;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.context.IntactContext;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.persistence.dao.AliasDao;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotSpliceVariant;
import uk.ac.ebi.intact.util.protein.CvHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Utilities for updating Aliases.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 1.1.2
 */
public class AliasUpdaterUtils {

    private AliasUpdaterUtils() {
    }

    /**
     * Sets up a logger for that class.
     */
    public static final Log log = LogFactory.getLog( AliasUpdaterUtils.class );

    public static void updateAllAliases( Protein protein, UniprotProtein uniprotProtein ) {

        updateAliasCollection( protein, buildAliases( uniprotProtein, protein ) );
    }

    public static void updateAllAliases( Protein protein, UniprotSpliceVariant uniprotSpliceVariant ) {

        updateAliasCollection( protein, buildAliases( uniprotSpliceVariant, protein ) );
    }

    public static boolean addNewAlias( AnnotatedObject current, InteractorAlias alias ) {

        // Make sure the alias does not yet exist in the object
        Collection aliases = current.getAliases();
        for ( Iterator iterator = aliases.iterator(); iterator.hasNext(); ) {
            Alias anAlias = ( Alias ) iterator.next();
            if ( anAlias.equals( alias ) ) {
                if ( log.isDebugEnabled() ) {
                    log.debug( "SKIPPED: [" + alias + "] already exists" );
                }
                return false; // already in, exit
            }
        }

        // add the alias to the AnnotatedObject
        current.addAlias( alias );

        // That test is done to avoid to record in the database an Alias
        // which is already linked to that AnnotatedObject.
        if ( alias.getParentAc() == current.getAc() ) {
            try {
                IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getAliasDao( InteractorAlias.class ).persist( alias );
                if ( log.isDebugEnabled() ) {
                    log.debug( "CREATED: [" + alias + "]" );
                }
            } catch ( Exception e_alias ) {
                log.error( "Error when creating an Alias for protein " + current, e_alias );
                return false;
            }
        }

        return true;
    }

    /**
     * Update of the Aliases of a protein.
     * <p/>
     * <pre>
     * Algo sketch:
     * 1) select all aliases of the given protein
     * 2) select the outdated aliases
     * 3) reused them to create new Alias and delete the remaining one. By doing so we don't waste ACs
     * </pre>
     *
     * @param protein    the protein what we want to update the Aliases
     * @param newAliases the new set of Aliases
     *
     * @return true if the protein has been updated, otherwise false
     */
    public static boolean updateAliasCollection( Protein protein, Collection<Alias> newAliases ) {

        AliasDao aliasDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getAliasDao( InteractorAlias.class );

        if ( protein == null ) {
            throw new IllegalArgumentException( "You must give a non null protein." );
        }

        if ( newAliases == null ) {
            throw new IllegalArgumentException( "You must give a non null collection of xref." );
        }

        boolean updated = false;
        Collection currentAliases = protein.getAliases();

        Collection<InteractorAlias> toDelete = CollectionUtils.subtract( currentAliases, newAliases ); // current minus new
        Collection<InteractorAlias> toCreate = CollectionUtils.subtract( newAliases, currentAliases );

        Iterator toDeleteIterator = toDelete.iterator();
        for ( InteractorAlias alias : toCreate ) {
            if ( toDeleteIterator.hasNext() ) {
                // in order to avoid wasting ACs, we overwrite attributes of an outdated xref.
                InteractorAlias recycledAlias = ( InteractorAlias ) toDeleteIterator.next();

                // note: parent_ac was already set before as the object was persistent
                recycledAlias.setName( alias.getName() );
                recycledAlias.setCvAliasType( alias.getCvAliasType() );

                aliasDao.update( recycledAlias );
                updated = true;

            } else {

                updated = updated | addNewAlias( protein, alias );
            }
        }

        for ( ; toDeleteIterator.hasNext(); ) {
            // delete remaining outdated/unrecycled aliases
            Alias alias = ( Alias ) toDeleteIterator.next();
            aliasDao.delete( alias );

            updated = true;
        }

        return updated;
    }

    /**
     * Read the uniprot protein and create a collection of Alias we want to update on the given protein.
     *
     * @param uniprotProtein the uniprot protein from which we will read the gene/locus/synonym/orf information.
     * @param protein        the protein we want to update
     *
     * @return a collection (never null) of Alias. The collection may be empty.
     */
    public static Collection<Alias> buildAliases( UniprotProtein uniprotProtein, Protein protein ) {

        Institution owner = CvHelper.getInstitution();

        CvAliasType geneNameAliasType = CvHelper.getAliasTypeByMi( CvAliasType.GENE_NAME_MI_REF );
        CvAliasType geneNameSynonymAliasType = CvHelper.getAliasTypeByMi( CvAliasType.GENE_NAME_SYNONYM_MI_REF );
        CvAliasType locusNameAliasType = CvHelper.getAliasTypeByMi( CvAliasType.LOCUS_NAME_MI_REF );
        CvAliasType orfNameAliasType = CvHelper.getAliasTypeByMi( CvAliasType.ORF_NAME_MI_REF );

        Collection<Alias> aliases = new ArrayList( 8 );

        for ( String geneName : uniprotProtein.getGenes() ) {
            aliases.add( new InteractorAlias( owner, protein, geneNameAliasType, geneName ) );
        }

        for ( String syn : uniprotProtein.getSynomyms() ) {
            aliases.add( new InteractorAlias( owner, protein, geneNameSynonymAliasType, syn ) );
        }

        for ( String orf : uniprotProtein.getOrfs() ) {
            aliases.add( new InteractorAlias( owner, protein, orfNameAliasType, orf ) );
        }

        for ( String locus : uniprotProtein.getLocuses() ) {
            aliases.add( new InteractorAlias( owner, protein, locusNameAliasType, locus ) );
        }

        return aliases;
    }

    /**
     * Read the splice variant and create a collection of Alias we want to update on the given protein.
     *
     * @param uniprotSpliceVariant the uniprot protein from which we will read the synonym information.
     * @param protein              the protein we want to update
     *
     * @return a collection (never null) of Alias. The collection may be empty.
     */
    public static Collection<Alias> buildAliases( UniprotSpliceVariant uniprotSpliceVariant, Protein protein ) {

        CvAliasType isoformSynonym = CvHelper.getAliasTypeByMi( CvAliasType.ISOFORM_SYNONYM_MI_REF );

        Collection<Alias> aliases = new ArrayList( 2 );

        for ( String syn : uniprotSpliceVariant.getSynomyms() ) {
            aliases.add( new InteractorAlias( CvHelper.getInstitution(), protein, isoformSynonym, syn ) );
        }

        return aliases;
    }
}