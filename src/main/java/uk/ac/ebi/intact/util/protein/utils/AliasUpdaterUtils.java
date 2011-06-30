/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.protein.utils;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.persistence.dao.AliasDao;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;
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

    /**
     * Update all the aliases of a master protein
     * @param protein
     * @param uniprotProtein
     */
    public static AliasUpdateReport updateAllAliases( Protein protein, UniprotProtein uniprotProtein, DataContext context, ProteinUpdateProcessor processor) {

        return updateAliasCollection( protein, buildAliases( uniprotProtein, protein), context, processor);
    }

    /**
     * Update all the aliases of a protein transcript
     * @param protein
     * @param uniprotProteinTranscript
     * @param uniprotProtein
     */
    public static AliasUpdateReport updateAllAliases( Protein protein, UniprotProteinTranscript uniprotProteinTranscript, UniprotProtein uniprotProtein, DataContext context, ProteinUpdateProcessor processor ) {

        return updateAliasCollection( protein, buildAliases(uniprotProtein, uniprotProteinTranscript, protein ), context, processor);
    }

    /**
     *
     * @param current
     * @param alias
     * @return true if the new alias has been added to the annotated object
     */
    public static boolean addNewAlias( AnnotatedObject current, InteractorAlias alias, DataContext context) {

        // Make sure the alias does not yet exist in the object
        Collection aliases = current.getAliases();

        if (aliases.contains(alias)){
            if ( log.isDebugEnabled() ) log.debug("SKIPPED: [" + alias + "] already exists" );
            return false; // already in, exit
        }

        // That test is done to avoid to record in the database an Alias
        // which is already linked to that AnnotatedObject.
        AliasDao<InteractorAlias> aliasAliasDao = context.getDaoFactory().getAliasDao(InteractorAlias.class);

        try {
            aliasAliasDao.persist( alias );
            if ( log.isDebugEnabled() ) {
                log.debug( "CREATED: [" + alias + "]" );
            }
        } catch ( Exception e_alias ) {
            log.error( "Error when creating an Alias for protein " + current, e_alias );
            return false;
        }

        // add the alias to the AnnotatedObject
        current.addAlias( alias );

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
    public static AliasUpdateReport updateAliasCollection( Protein protein, Collection<Alias> newAliases, DataContext context, ProteinUpdateProcessor processor) {

        //AliasDao aliasDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getAliasDao( InteractorAlias.class );

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

        AliasUpdateReport report = new AliasUpdateReport(protein);

        Iterator<InteractorAlias> toDeleteIterator = toDelete.iterator();
        for ( InteractorAlias alias : toCreate ) {
            if ( toDeleteIterator.hasNext() ) {
                // in order to avoid wasting ACs, we overwrite attributes of an outdated xref.
                InteractorAlias recycledAlias = ( InteractorAlias ) toDeleteIterator.next();

                // add a copy of the deleted alias to the report
                InteractorAlias copy = new InteractorAlias();
                copy.setCvAliasType(recycledAlias.getCvAliasType());
                copy.setName(recycledAlias.getName());

                report.getRemovedAliases().add(copy);

                // note: parent_ac was already set before as the object was persistent
                recycledAlias.setName( alias.getName() );
                recycledAlias.setCvAliasType( alias.getCvAliasType() );

                // add the new alias to the report
                report.getAddedAliases().add(recycledAlias);

                context.getDaoFactory().getAliasDao(InteractorAlias.class).update( recycledAlias );
                updated = true;

            } else {

                updated = updated | addNewAlias( protein, alias, context );

                report.getAddedAliases().add(alias);
            }
        }

        for ( ; toDeleteIterator.hasNext(); ) {
            // delete remaining outdated/unrecycled aliases
            InteractorAlias alias = toDeleteIterator.next();

            ProteinTools.deleteAlias(protein, context, alias, processor);

            //aliasDao.delete( alias );

            report.getRemovedAliases().add(alias);

            updated = true;
        }

        context.getDaoFactory().getProteinDao().update((ProteinImpl) protein);
        return report;
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
     * @param master
     * @param uniprotProteinTranscript the uniprot protein from which we will read the synonym information.
     * @param protein              the protein we want to update
     *
     * @return a collection (never null) of Alias. The collection may be empty.
     */
    public static Collection<Alias> buildAliases( UniprotProtein master, UniprotProteinTranscript uniprotProteinTranscript, Protein protein ) {

        CvAliasType isoformSynonym = CvHelper.getAliasTypeByMi( CvAliasType.ISOFORM_SYNONYM_MI_REF );

        Collection<Alias> aliases = new ArrayList( 2 );

        for ( String syn : uniprotProteinTranscript.getSynomyms() ) {
            aliases.add( new InteractorAlias( CvHelper.getInstitution(), protein, isoformSynonym, syn ) );
        }

        aliases.addAll(buildAliases(master, protein));

        return aliases;
    }
}