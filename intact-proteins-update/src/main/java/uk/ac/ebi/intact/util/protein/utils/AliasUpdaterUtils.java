/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.protein.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.persistence.dao.AliasDao;
import uk.ac.ebi.intact.model.CvAliasType;
import uk.ac.ebi.intact.model.InteractorAlias;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;
import uk.ac.ebi.intact.util.protein.CvHelper;

import java.util.Iterator;
import java.util.TreeSet;

/**
 * Utilities for updating Aliases.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 1.1.2
 */
public class AliasUpdaterUtils {

    /**
     * Sets up a logger for that class.
     */
    public static final Log log = LogFactory.getLog(AliasUpdaterUtils.class);

    private AliasUpdaterUtils() {
    }

    public static AliasUpdateReport updateAliases(UniprotProtein uniprotProtein, Protein protein, AliasDao<InteractorAlias> aliasDao, TreeSet<InteractorAlias> sortedAliases) {

        sortedAliases.clear();
        sortedAliases.addAll(protein.getAliases());
        Iterator<InteractorAlias> intactIterator = sortedAliases.iterator();

        AliasUpdateReport report = new AliasUpdateReport(protein);

        // process genes
        TreeSet<String> geneNames = new TreeSet<>(uniprotProtein.getGenes());
        Iterator<String> geneIterator = geneNames.iterator();
        InteractorAlias currentIntact = null;

        if (geneIterator.hasNext()) {
            currentIntact = compareAndUpdateAliases(protein, null, intactIterator, geneIterator, CvAliasType.GENE_NAME_MI_REF, report, aliasDao);
        }

        // process synonyms
        TreeSet<String> geneSynonyms = new TreeSet<>(uniprotProtein.getSynomyms());
        Iterator<String> geneSynonymsIterator = geneSynonyms.iterator();

        if (geneSynonymsIterator.hasNext()) {
            currentIntact = compareAndUpdateAliases(protein, currentIntact, intactIterator, geneSynonymsIterator, CvAliasType.GENE_NAME_SYNONYM_MI_REF, report, aliasDao);
        }

        // process orfs
        TreeSet<String> orfs = new TreeSet<>(uniprotProtein.getOrfs());
        Iterator<String> orfsIterator = orfs.iterator();

        if (orfsIterator.hasNext()) {
            currentIntact = compareAndUpdateAliases(protein, currentIntact, intactIterator, orfsIterator, CvAliasType.ORF_NAME_MI_REF, report, aliasDao);
        }

        // process locus
        TreeSet<String> locuses = new TreeSet<>(uniprotProtein.getLocuses());
        Iterator<String> locusesIterator = locuses.iterator();

        if (locusesIterator.hasNext()) {
            currentIntact = compareAndUpdateAliases(protein, currentIntact, intactIterator, locusesIterator, CvAliasType.LOCUS_NAME_MI_REF, report, aliasDao);
        }

        // delete remaining aliases
        if (currentIntact != null || intactIterator.hasNext()) {
            if (currentIntact == null) {
                currentIntact = intactIterator.next();
            }
            do {
                protein.removeAlias(currentIntact);
                report.getRemovedAliases().add(currentIntact);

                aliasDao.delete(currentIntact);

                if (intactIterator.hasNext()) {
                    currentIntact = intactIterator.next();
                } else {
                    currentIntact = null;
                }
            } while (currentIntact != null);
        }

        sortedAliases.clear();
        return report;
    }

    private static InteractorAlias compareAndUpdateAliases(Protein protein, InteractorAlias currentAlias, Iterator<InteractorAlias> intactIterator, Iterator<String> uniprotIterator, String aliasTypeMI, AliasUpdateReport report, AliasDao<InteractorAlias> aliasDao) {
        String currentUniprot = null;
        CvAliasType currentCvType = null;

        if (currentAlias == null && intactIterator.hasNext()) {
            currentAlias = intactIterator.next();
            currentCvType = currentAlias.getCvAliasType();
        }

        if (currentAlias != null && uniprotIterator.hasNext()) {
            currentUniprot = uniprotIterator.next();

            // the alias has the alias type we expect so we can compare with uniprot and update
            if (currentCvType != null && aliasTypeMI.equalsIgnoreCase(currentCvType.getIdentifier())) {
                do {

                    if (currentAlias.getName() == null) {
                        protein.removeAlias(currentAlias);
                        report.getRemovedAliases().add(currentAlias);

                        aliasDao.delete(currentAlias);

                        if (intactIterator.hasNext()) {
                            currentAlias = intactIterator.next();
                            currentCvType = currentAlias.getCvAliasType();
                        } else {
                            currentAlias = null;
                            currentCvType = null;
                        }
                    } else {
                        int nameComparator = currentAlias.getName().compareTo(currentUniprot);

                        // existing alias in intact and uniprot
                        if (nameComparator == 0) {
                            if (uniprotIterator.hasNext() && intactIterator.hasNext()) {
                                currentUniprot = uniprotIterator.next();
                                currentAlias = intactIterator.next();
                                currentCvType = currentAlias.getCvAliasType();
                            } else {
                                currentUniprot = null;
                                currentAlias = null;
                                currentCvType = null;
                            }
                        }
                        // alias not in uniprot, needs to be deleted
                        else if (nameComparator < 0) {
                            protein.removeAlias(currentAlias);
                            report.getRemovedAliases().add(currentAlias);

                            aliasDao.delete(currentAlias);

                            if (intactIterator.hasNext()) {
                                currentAlias = intactIterator.next();
                                currentCvType = currentAlias.getCvAliasType();
                            } else {
                                currentAlias = null;
                                currentCvType = null;
                            }
                        }
                        // alias not in intact, needs to be created
                        else {
                            InteractorAlias newAlias = new InteractorAlias(protein.getOwner(), protein, currentCvType, currentUniprot);
                            aliasDao.persist(newAlias);

                            report.getAddedAliases().add(newAlias);

                            protein.addAlias(newAlias);

                            if (uniprotIterator.hasNext()) {
                                currentUniprot = uniprotIterator.next();
                            } else {
                                currentUniprot = null;
                            }
                        }
                    }

                } while (currentUniprot != null && currentAlias != null && aliasTypeMI.equalsIgnoreCase(currentCvType.getIdentifier()));
            }
            // the alias does not have a type that we expect so it should be removed
            else if (currentCvType != null && !aliasTypeMI.equalsIgnoreCase(currentCvType.getIdentifier())) {
                // first delete all aliases not in uniprot until we come to the current alias type
                do {

                    protein.removeAlias(currentAlias);
                    report.getRemovedAliases().add(currentAlias);

                    aliasDao.delete(currentAlias);

                    if (intactIterator.hasNext()) {
                        currentAlias = intactIterator.next();
                        currentCvType = currentAlias.getCvAliasType();
                    } else {
                        currentAlias = null;
                        currentCvType = null;
                    }

                } while (currentAlias != null && !aliasTypeMI.equalsIgnoreCase(currentCvType.getIdentifier()));

                // then, we can update aliases of same type if we still have protein aliases to process
                if (currentAlias != null) {

                    // if the alias that we compare with uniprot does have the valid type. We can compare and update
                    if (currentCvType != null && aliasTypeMI.equalsIgnoreCase(currentCvType.getIdentifier())) {
                        do {

                            if (currentAlias.getName() == null) {
                                protein.removeAlias(currentAlias);
                                report.getRemovedAliases().add(currentAlias);

                                aliasDao.delete(currentAlias);

                                if (intactIterator.hasNext()) {
                                    currentAlias = intactIterator.next();
                                    currentCvType = currentAlias.getCvAliasType();
                                } else {
                                    currentAlias = null;
                                    currentCvType = null;
                                }
                            } else {
                                int nameComparator = currentAlias.getName().compareTo(currentUniprot);

                                // existing alias in intact and uniprot
                                if (nameComparator == 0) {
                                    if (uniprotIterator.hasNext() && intactIterator.hasNext()) {
                                        currentUniprot = uniprotIterator.next();
                                        currentAlias = intactIterator.next();
                                        currentCvType = currentAlias.getCvAliasType();
                                    } else {
                                        currentUniprot = null;
                                        currentAlias = null;
                                        currentCvType = null;
                                    }
                                }
                                // alias not in uniprot, needs to be deleted
                                else if (nameComparator < 0) {
                                    protein.removeAlias(currentAlias);
                                    report.getRemovedAliases().add(currentAlias);

                                    aliasDao.delete(currentAlias);

                                    if (intactIterator.hasNext()) {
                                        currentAlias = intactIterator.next();
                                        currentCvType = currentAlias.getCvAliasType();
                                    } else {
                                        currentAlias = null;
                                        currentCvType = null;
                                    }
                                }
                                // alias not in intact, needs to be created
                                else {
                                    InteractorAlias newAlias = new InteractorAlias(protein.getOwner(), protein, currentCvType, currentUniprot);
                                    aliasDao.persist(newAlias);

                                    report.getAddedAliases().add(newAlias);

                                    protein.addAlias(newAlias);

                                    if (uniprotIterator.hasNext()) {
                                        currentUniprot = uniprotIterator.next();
                                    } else {
                                        currentUniprot = null;
                                    }
                                }
                            }

                        } while (currentUniprot != null && currentAlias != null && aliasTypeMI.equalsIgnoreCase(currentCvType.getIdentifier()));
                    }
                }
            }
        }

        // we still have some aliases in uniprot which need to be created in intact
        if (currentUniprot != null || uniprotIterator.hasNext()) {
            CvAliasType aliasTypeFromDb = CvHelper.getAliasTypeByMi(aliasTypeMI);

            if (currentUniprot == null) {
                currentUniprot = uniprotIterator.next();
            }

            do {
                InteractorAlias newAlias = new InteractorAlias(protein.getOwner(), protein, aliasTypeFromDb, currentUniprot);
                aliasDao.persist(newAlias);

                report.getAddedAliases().add(newAlias);

                protein.addAlias(newAlias);

                if (uniprotIterator.hasNext()) {
                    currentUniprot = uniprotIterator.next();
                } else {
                    currentUniprot = null;
                }
            } while (currentUniprot != null);
        }

        // we still have some aliases in intact which may need to be removed
        if (currentAlias != null) {

            if (currentCvType != null && aliasTypeMI.equalsIgnoreCase(currentCvType.getIdentifier())) {
                do {
                    protein.removeAlias(currentAlias);
                    report.getRemovedAliases().add(currentAlias);

                    aliasDao.delete(currentAlias);

                    if (intactIterator.hasNext()) {
                        currentAlias = intactIterator.next();
                        currentCvType = currentAlias.getCvAliasType();
                    } else {
                        currentAlias = null;
                        currentCvType = null;
                    }
                } while (currentAlias != null && aliasTypeMI.equalsIgnoreCase(currentCvType.getIdentifier()));
            }
        }

        return currentAlias;
    }

    public static AliasUpdateReport updateIsoformAliases(UniprotProtein master, UniprotProteinTranscript uniprotProteinTranscript, Protein protein, AliasDao<InteractorAlias> aliasDao, TreeSet<InteractorAlias> sortedAliases) {

        sortedAliases.clear();
        sortedAliases.addAll(protein.getAliases());
        Iterator<InteractorAlias> intactIterator = sortedAliases.iterator();

        AliasUpdateReport report = new AliasUpdateReport(protein);

        // process genes
        TreeSet<String> geneNames = new TreeSet<>(master.getGenes());
        Iterator<String> geneIterator = geneNames.iterator();
        InteractorAlias currentIntact = null;

        if (geneIterator.hasNext()) {
            currentIntact = compareAndUpdateAliases(protein, null, intactIterator, geneIterator, CvAliasType.GENE_NAME_MI_REF, report, aliasDao);
        }

        // process synonyms
        TreeSet<String> geneSynonyms = new TreeSet<>(master.getSynomyms());
        Iterator<String> geneSynonymsIterator = geneSynonyms.iterator();

        if (geneSynonymsIterator.hasNext()) {
            currentIntact = compareAndUpdateAliases(protein, currentIntact, intactIterator, geneSynonymsIterator, CvAliasType.GENE_NAME_SYNONYM_MI_REF, report, aliasDao);
        }

        // process orfs
        TreeSet<String> orfs = new TreeSet<>(master.getOrfs());
        Iterator<String> orfsIterator = orfs.iterator();

        if (orfsIterator.hasNext()) {
            currentIntact = compareAndUpdateAliases(protein, currentIntact, intactIterator, orfsIterator, CvAliasType.ORF_NAME_MI_REF, report, aliasDao);
        }

        // process locus
        TreeSet<String> locuses = new TreeSet<>(master.getLocuses());
        Iterator<String> locusesIterator = locuses.iterator();

        if (locusesIterator.hasNext()) {
            currentIntact = compareAndUpdateAliases(protein, currentIntact, intactIterator, locusesIterator, CvAliasType.LOCUS_NAME_MI_REF, report, aliasDao);
        }

        // process isoform synonyms
        TreeSet<String> isoformSynonyms = new TreeSet<>(uniprotProteinTranscript.getSynomyms());
        Iterator<String> isoformSynonymsIterator = isoformSynonyms.iterator();

        if (isoformSynonymsIterator.hasNext()) {
            currentIntact = compareAndUpdateAliases(protein, currentIntact, intactIterator, isoformSynonymsIterator, CvAliasType.ISOFORM_SYNONYM_MI_REF, report, aliasDao);
        }

        // delete remaining aliases
        if (currentIntact != null || intactIterator.hasNext()) {
            if (currentIntact == null) {
                currentIntact = intactIterator.next();
            }
            do {
                protein.removeAlias(currentIntact);
                report.getRemovedAliases().add(currentIntact);

                aliasDao.delete(currentIntact);

                if (intactIterator.hasNext()) {
                    currentIntact = intactIterator.next();
                } else {
                    currentIntact = null;
                }
            } while (currentIntact != null);
        }

        sortedAliases.clear();
        return report;
    }
}