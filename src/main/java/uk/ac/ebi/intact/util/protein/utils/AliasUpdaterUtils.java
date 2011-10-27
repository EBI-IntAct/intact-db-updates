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

import java.util.*;

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

            ProteinTools.deleteAlias(protein, context, alias);

            //aliasDao.delete( alias );

            report.getRemovedAliases().add(alias);

            updated = true;
        }

        context.getDaoFactory().getProteinDao().update((ProteinImpl) protein);
        return report;
    }

    private static Map<String, Collection<InteractorAlias>> clusterExistingAliases(Protein protein){

        if (protein.getAliases().isEmpty()){
            return Collections.EMPTY_MAP;
        }

        Map<String, Collection<InteractorAlias>> map = new HashMap<String, Collection<InteractorAlias>> (protein.getAliases().size());

        for (InteractorAlias alias : protein.getAliases()){
            CvAliasType type = alias.getCvAliasType();

            if (type == null){
                if (map.containsKey("null")){
                    map.get("null").add(alias);
                }
                else {
                    Collection<InteractorAlias> aliases = new ArrayList<InteractorAlias>();
                    aliases.add(alias);
                    map.put("null", aliases);
                }
            }
            else {
                if (map.containsKey(type.getIdentifier())){
                    map.get(type.getIdentifier()).add(alias);
                }
                else {
                    Collection<InteractorAlias> aliases = new ArrayList<InteractorAlias>();
                    aliases.add(alias);
                    map.put(type.getIdentifier(), aliases);
                }
            }
        }

        return map;
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

    public static AliasUpdateReport updateAliases( UniprotProtein uniprotProtein, Protein protein, AliasDao aliasDao, DataContext context) {

        Institution owner = protein.getOwner();

        CvAliasType geneNameAliasType = null;
        CvAliasType geneNameSynonymAliasType = null;
        CvAliasType locusNameAliasType = null;
        CvAliasType orfNameAliasType = null;

        Map<String, Collection<InteractorAlias>> existingAliases = clusterExistingAliases(protein);
        Set<String> namesToCreate = new HashSet<String>(uniprotProtein.getGenes());

        AliasUpdateReport report = new AliasUpdateReport(protein);

        if (existingAliases.containsKey(CvAliasType.GENE_NAME_MI_REF)){
            Collection<InteractorAlias> aliases = existingAliases.get(CvAliasType.GENE_NAME_MI_REF);

            geneNameAliasType = aliases.iterator().next().getCvAliasType();

            for (InteractorAlias alias : aliases){
                boolean hasFound = false;

                for ( String geneName : uniprotProtein.getGenes() ) {
                    if (geneName.equals(alias.getName())){
                        hasFound = true;
                        namesToCreate.remove(geneName);
                        break;
                    }
                }

                if (!hasFound){
                    ProteinTools.deleteAlias(protein, context, alias);
                    report.getRemovedAliases().add(alias);
                }
            }

            if (!namesToCreate.isEmpty()){
                for ( String geneName : namesToCreate ) {

                    InteractorAlias newAlias = new InteractorAlias( owner, protein, geneNameAliasType, geneName );
                    aliasDao.persist(newAlias);
                    report.getAddedAliases().add(newAlias);

                    protein.addAlias(newAlias);
                }
            }

            existingAliases.remove(geneNameAliasType.getIdentifier());
        }
        else {
            geneNameAliasType = CvHelper.getAliasTypeByMi( CvAliasType.GENE_NAME_MI_REF );

            for ( String geneName : uniprotProtein.getGenes() ) {

                InteractorAlias newAlias = new InteractorAlias( owner, protein, geneNameAliasType, geneName );
                aliasDao.persist(newAlias);

                report.getAddedAliases().add(newAlias);

                protein.addAlias(newAlias);
            }
        }
        namesToCreate.clear();
        namesToCreate.addAll(uniprotProtein.getSynomyms());

        if (existingAliases.containsKey(CvAliasType.GENE_NAME_SYNONYM_MI_REF)){
            Collection<InteractorAlias> aliases = existingAliases.get(CvAliasType.GENE_NAME_SYNONYM_MI_REF);

            geneNameSynonymAliasType = aliases.iterator().next().getCvAliasType();

            for (InteractorAlias alias : aliases){
                boolean hasFound = false;

                for ( String geneNameSynonym : uniprotProtein.getSynomyms() ) {
                    if (geneNameSynonym.equals(alias.getName())){
                        hasFound = true;
                        namesToCreate.remove(geneNameSynonym);
                        break;
                    }
                }

                if (!hasFound){
                    ProteinTools.deleteAlias(protein, context, alias);
                    report.getRemovedAliases().add(alias);
                }
            }

            if (!namesToCreate.isEmpty()){
                for ( String geneNameSynonym : namesToCreate ) {

                    InteractorAlias newAlias = new InteractorAlias( owner, protein, geneNameSynonymAliasType, geneNameSynonym );
                    aliasDao.persist(newAlias);

                    report.getAddedAliases().add(newAlias);

                    protein.addAlias(newAlias);
                }
            }

            existingAliases.remove(geneNameSynonymAliasType.getIdentifier());
        }
        else {
            geneNameSynonymAliasType = CvHelper.getAliasTypeByMi( CvAliasType.GENE_NAME_SYNONYM_MI_REF );

            for ( String geneNameSynonym : uniprotProtein.getSynomyms() ) {

                InteractorAlias newAlias = new InteractorAlias( owner, protein, geneNameSynonymAliasType, geneNameSynonym );
                aliasDao.persist(newAlias);

                report.getAddedAliases().add(newAlias);

                protein.addAlias(newAlias);
            }
        }

        namesToCreate.clear();
        namesToCreate.addAll(uniprotProtein.getOrfs());

        if (existingAliases.containsKey(CvAliasType.ORF_NAME_MI_REF)){
            Collection<InteractorAlias> aliases = existingAliases.get(CvAliasType.ORF_NAME_MI_REF);

            orfNameAliasType = aliases.iterator().next().getCvAliasType();

            for (InteractorAlias alias : aliases){
                boolean hasFound = false;

                for ( String orf : uniprotProtein.getOrfs() ) {
                    if (orf.equals(alias.getName())){
                        hasFound = true;
                        namesToCreate.remove(orf);
                        break;
                    }
                }

                if (!hasFound){
                    ProteinTools.deleteAlias(protein, context, alias);
                    report.getRemovedAliases().add(alias);
                }
            }

            if (!namesToCreate.isEmpty()){
                for ( String orf : namesToCreate ) {

                    InteractorAlias newAlias = new InteractorAlias( owner, protein, orfNameAliasType, orf );
                    aliasDao.persist(newAlias);

                    report.getAddedAliases().add(newAlias);

                    protein.addAlias(newAlias);
                }
            }

            existingAliases.remove(orfNameAliasType.getIdentifier());
        }
        else {
            orfNameAliasType = CvHelper.getAliasTypeByMi( CvAliasType.ORF_NAME_MI_REF );
            for ( String orf : uniprotProtein.getOrfs() ) {

                InteractorAlias newAlias = new InteractorAlias( owner, protein, orfNameAliasType, orf );
                aliasDao.persist(newAlias);

                report.getAddedAliases().add(newAlias);

                protein.addAlias(newAlias);
            }
        }

        namesToCreate.clear();
        namesToCreate.addAll(uniprotProtein.getLocuses());
        if (existingAliases.containsKey(CvAliasType.LOCUS_NAME_MI_REF)){
            Collection<InteractorAlias> aliases = existingAliases.get(CvAliasType.LOCUS_NAME_MI_REF);

            locusNameAliasType = aliases.iterator().next().getCvAliasType();

            for (InteractorAlias alias : aliases){
                boolean hasFound = false;

                for ( String locus : uniprotProtein.getLocuses() ) {
                    if (locus.equals(alias.getName())){
                        hasFound = true;
                        namesToCreate.remove(locus);
                        break;
                    }
                }

                if (!hasFound){
                    ProteinTools.deleteAlias(protein, context, alias);
                    report.getRemovedAliases().add(alias);
                }
            }

            if (!namesToCreate.isEmpty()){
                for ( String locus : namesToCreate ) {

                    InteractorAlias newAlias = new InteractorAlias( owner, protein, locusNameAliasType, locus );
                    aliasDao.persist(newAlias);

                    report.getAddedAliases().add(newAlias);

                    protein.addAlias(newAlias);
                }
            }

            existingAliases.remove(locusNameAliasType.getIdentifier());
        }
        else {
            locusNameAliasType = CvHelper.getAliasTypeByMi( CvAliasType.LOCUS_NAME_MI_REF );
            for ( String locus : uniprotProtein.getLocuses() ) {

                InteractorAlias newAlias = new InteractorAlias( owner, protein, locusNameAliasType, locus );
                aliasDao.persist(newAlias);

                report.getAddedAliases().add(newAlias);

                protein.addAlias(newAlias);
            }
        }

        for (Map.Entry<String, Collection<InteractorAlias>> entry : existingAliases.entrySet()){
            for (InteractorAlias alias : entry.getValue()){
                ProteinTools.deleteAlias(protein, context, alias);
                aliasDao.delete(alias);
            }
        }
        return report;
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

    public static AliasUpdateReport updateIsoformAliases( UniprotProtein master, UniprotProteinTranscript uniprotProteinTranscript, Protein protein, AliasDao aliasDao, DataContext context ) {

        CvAliasType isoformSynonym = null;
        CvAliasType geneNameAliasType = null;
        CvAliasType geneNameSynonymAliasType = null;
        CvAliasType locusNameAliasType = null;
        CvAliasType orfNameAliasType = null;

        Institution owner = protein.getOwner();

        Map<String, Collection<InteractorAlias>> existingAliases = clusterExistingAliases(protein);
        Set<String> namesToCreate = new HashSet<String>(master.getGenes());

        AliasUpdateReport report = new AliasUpdateReport(protein);

        if (existingAliases.containsKey(CvAliasType.GENE_NAME_MI_REF)){
            Collection<InteractorAlias> aliases = existingAliases.get(CvAliasType.GENE_NAME_MI_REF);

            geneNameAliasType = aliases.iterator().next().getCvAliasType();

            for (InteractorAlias alias : aliases){
                boolean hasFound = false;

                for ( String geneName : master.getGenes() ) {
                    if (geneName.equals(alias.getName())){
                        hasFound = true;
                        namesToCreate.remove(geneName);
                        break;
                    }
                }

                if (!hasFound){
                    ProteinTools.deleteAlias(protein, context, alias);
                    report.getRemovedAliases().add(alias);
                }
            }

            if (!namesToCreate.isEmpty()){
                for ( String geneName : namesToCreate ) {

                    InteractorAlias newAlias = new InteractorAlias( owner, protein, geneNameAliasType, geneName );
                    aliasDao.persist(newAlias);

                    report.getAddedAliases().add(newAlias);

                    protein.addAlias(newAlias);
                }
            }

            existingAliases.remove(geneNameAliasType.getIdentifier());
        }
        else {
        geneNameAliasType = CvHelper.getAliasTypeByMi( CvAliasType.GENE_NAME_MI_REF );

            for ( String geneName : master.getGenes() ) {

                InteractorAlias newAlias = new InteractorAlias( owner, protein, geneNameAliasType, geneName );
                aliasDao.persist(newAlias);

                report.getAddedAliases().add(newAlias);

                protein.addAlias(newAlias);
            }
        }
        namesToCreate.clear();
        namesToCreate.addAll(master.getSynomyms());

        if (existingAliases.containsKey(CvAliasType.GENE_NAME_SYNONYM_MI_REF)){
            Collection<InteractorAlias> aliases = existingAliases.get(CvAliasType.GENE_NAME_SYNONYM_MI_REF);

            geneNameSynonymAliasType = aliases.iterator().next().getCvAliasType();

            for (InteractorAlias alias : aliases){
                boolean hasFound = false;

                for ( String geneNameSynonym : master.getSynomyms() ) {
                    if (geneNameSynonym.equals(alias.getName())){
                        hasFound = true;
                        namesToCreate.remove(geneNameSynonym);
                        break;
                    }
                }

                if (!hasFound){
                    ProteinTools.deleteAlias(protein, context, alias);
                    report.getRemovedAliases().add(alias);
                }
            }

            if (!namesToCreate.isEmpty()){
                for ( String geneNameSynonym : namesToCreate ) {

                    InteractorAlias newAlias = new InteractorAlias( owner, protein, geneNameSynonymAliasType, geneNameSynonym );
                    aliasDao.persist(newAlias);

                    report.getAddedAliases().add(newAlias);

                    protein.addAlias(newAlias);
                }
            }

            existingAliases.remove(geneNameSynonymAliasType.getIdentifier());
        }
        else {
        geneNameSynonymAliasType = CvHelper.getAliasTypeByMi( CvAliasType.GENE_NAME_SYNONYM_MI_REF );
            for ( String geneNameSynonym : master.getSynomyms() ) {

                InteractorAlias newAlias = new InteractorAlias( owner, protein, geneNameSynonymAliasType, geneNameSynonym );
                aliasDao.persist(newAlias);

                report.getAddedAliases().add(newAlias);

                protein.addAlias(newAlias);
            }
        }

        namesToCreate.clear();
        namesToCreate.addAll(master.getOrfs());

        if (existingAliases.containsKey(CvAliasType.ORF_NAME_MI_REF)){
            Collection<InteractorAlias> aliases = existingAliases.get(CvAliasType.ORF_NAME_MI_REF);

            orfNameAliasType = aliases.iterator().next().getCvAliasType();

            for (InteractorAlias alias : aliases){
                boolean hasFound = false;

                for ( String orf : master.getOrfs() ) {
                    if (orf.equals(alias.getName())){
                        hasFound = true;
                        namesToCreate.remove(orf);
                        break;
                    }
                }

                if (!hasFound){
                    ProteinTools.deleteAlias(protein, context, alias);
                    report.getRemovedAliases().add(alias);
                }
            }

            if (!namesToCreate.isEmpty()){
                for ( String orf : namesToCreate ) {

                    InteractorAlias newAlias = new InteractorAlias( owner, protein, orfNameAliasType, orf );
                    aliasDao.persist(newAlias);

                    report.getAddedAliases().add(newAlias);

                    protein.addAlias(newAlias);
                }
            }

            existingAliases.remove(orfNameAliasType.getIdentifier());
        }
        else {
        orfNameAliasType = CvHelper.getAliasTypeByMi( CvAliasType.ORF_NAME_MI_REF );
            for ( String orf : master.getOrfs() ) {

                InteractorAlias newAlias = new InteractorAlias( owner, protein, orfNameAliasType, orf );
                aliasDao.persist(newAlias);

                report.getAddedAliases().add(newAlias);

                protein.addAlias(newAlias);
            }
        }

        namesToCreate.clear();
        namesToCreate.addAll(master.getLocuses());
        if (existingAliases.containsKey(CvAliasType.LOCUS_NAME_MI_REF)){
            Collection<InteractorAlias> aliases = existingAliases.get(CvAliasType.LOCUS_NAME_MI_REF);

            locusNameAliasType = aliases.iterator().next().getCvAliasType();

            for (InteractorAlias alias : aliases){
                boolean hasFound = false;

                for ( String locus : master.getLocuses() ) {
                    if (locus.equals(alias.getName())){
                        hasFound = true;
                        namesToCreate.remove(locus);
                        break;
                    }
                }

                if (!hasFound){
                    ProteinTools.deleteAlias(protein, context, alias);
                    report.getRemovedAliases().add(alias);
                }
            }

            if (!namesToCreate.isEmpty()){
                for ( String locus : namesToCreate ) {

                    InteractorAlias newAlias = new InteractorAlias( owner, protein, locusNameAliasType, locus );
                    aliasDao.persist(newAlias);

                    report.getAddedAliases().add(newAlias);

                    protein.addAlias(newAlias);
                }
            }

            existingAliases.remove(locusNameAliasType.getIdentifier());
        }
        else {
        locusNameAliasType = CvHelper.getAliasTypeByMi( CvAliasType.LOCUS_NAME_MI_REF );
            for ( String locus : master.getLocuses() ) {

                InteractorAlias newAlias = new InteractorAlias( owner, protein, locusNameAliasType, locus );
                aliasDao.persist(newAlias);

                report.getAddedAliases().add(newAlias);

                protein.addAlias(newAlias);
            }
        }

        namesToCreate.clear();
        namesToCreate.addAll(uniprotProteinTranscript.getSynomyms());
        if (existingAliases.containsKey(CvAliasType.ISOFORM_SYNONYM_MI_REF)){
            Collection<InteractorAlias> aliases = existingAliases.get(CvAliasType.ISOFORM_SYNONYM_MI_REF);

            isoformSynonym = aliases.iterator().next().getCvAliasType();

            for (InteractorAlias alias : aliases){
                boolean hasFound = false;

                for ( String syn : uniprotProteinTranscript.getSynomyms() ) {
                    if (syn.equals(alias.getName())){
                        hasFound = true;
                        namesToCreate.remove(syn);
                        break;
                    }
                }

                if (!hasFound){
                    ProteinTools.deleteAlias(protein, context, alias);
                    report.getRemovedAliases().add(alias);
                }
            }

            if (!namesToCreate.isEmpty()){
                for ( String syn : namesToCreate ) {

                    InteractorAlias newAlias = new InteractorAlias( owner, protein, isoformSynonym, syn );
                    aliasDao.persist(newAlias);

                    report.getAddedAliases().add(newAlias);

                    protein.addAlias(newAlias);
                }
            }

            existingAliases.remove(isoformSynonym.getIdentifier());
        }
        else {
         isoformSynonym = CvHelper.getAliasTypeByMi( CvAliasType.ISOFORM_SYNONYM_MI_REF );
            for ( String syn : uniprotProteinTranscript.getSynomyms() ) {

                InteractorAlias newAlias = new InteractorAlias( owner, protein, isoformSynonym, syn );
                aliasDao.persist(newAlias);

                report.getAddedAliases().add(newAlias);

                protein.addAlias(newAlias);
            }
        }

        for (Map.Entry<String, Collection<InteractorAlias>> entry : existingAliases.entrySet()){
            for (InteractorAlias alias : entry.getValue()){
                ProteinTools.deleteAlias(protein, context, alias);
                report.getRemovedAliases().add(alias);
            }
        }
        return report;
    }
}