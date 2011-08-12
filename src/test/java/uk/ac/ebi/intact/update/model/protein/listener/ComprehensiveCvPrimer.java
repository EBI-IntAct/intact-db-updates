/**
 * Copyright 2008 The European Bioinformatics Institute, and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.intact.update.model.protein.listener;

import uk.ac.ebi.intact.core.config.impl.SmallCvPrimer;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.model.*;

/**
 * TODO comment that class header
*
* @author Bruno Aranda (baranda@ebi.ac.uk)
* @version $Id$
*/
public class ComprehensiveCvPrimer extends SmallCvPrimer {

    public ComprehensiveCvPrimer(DaoFactory daoFactory) {
        super(daoFactory);
    }

    @Override
    public void createCVs() {
        super.createCVs();

        getCvObject(CvInteractorType.class, CvInteractorType.PROTEIN, CvInteractorType.PROTEIN_MI_REF);
        getCvObject(CvInteractorType.class, CvInteractorType.DNA, CvInteractorType.DNA_MI_REF);
        getCvObject(CvDatabase.class, CvDatabase.UNIPROT, CvDatabase.UNIPROT_MI_REF);
        getCvObject(CvDatabase.class, CvDatabase.INTERPRO, CvDatabase.INTERPRO_MI_REF);
        getCvObject(CvXrefQualifier.class, CvXrefQualifier.SECONDARY_AC, CvXrefQualifier.SECONDARY_AC_MI_REF);
        getCvObject(CvXrefQualifier.class, CvXrefQualifier.ISOFORM_PARENT, CvXrefQualifier.ISOFORM_PARENT_MI_REF);
        getCvObject(CvXrefQualifier.class, "chain-updateProcess", "MI:0951");
        getCvObject(CvAliasType.class, CvAliasType.GENE_NAME, CvAliasType.GENE_NAME_MI_REF);
        getCvObject(CvAliasType.class, CvAliasType.GENE_NAME_SYNONYM, CvAliasType.GENE_NAME_SYNONYM_MI_REF);
        getCvObject(CvAliasType.class, CvAliasType.ISOFORM_SYNONYM, CvAliasType.ISOFORM_SYNONYM_MI_REF);
        getCvObject(CvAliasType.class, CvAliasType.LOCUS_NAME, CvAliasType.LOCUS_NAME_MI_REF);
        getCvObject(CvAliasType.class, CvAliasType.ORF_NAME, CvAliasType.ORF_NAME_MI_REF);
        getCvObject(CvInteractionType.class, CvInteractionType.DIRECT_INTERACTION, CvInteractionType.DIRECT_INTERACTION_MI_REF);
        getCvObject(CvExperimentalRole.class, CvExperimentalRole.ANCILLARY, CvExperimentalRole.ANCILLARY_MI_REF);
        getCvObject(CvExperimentalRole.class, CvExperimentalRole.NEUTRAL, CvExperimentalRole.NEUTRAL_PSI_REF);
        getCvObject(CvBiologicalRole.class, CvBiologicalRole.COFACTOR, CvBiologicalRole.COFACTOR_MI_REF);
        getCvObject(CvFuzzyType.class, CvFuzzyType.UNDETERMINED, CvFuzzyType.UNDETERMINED_MI_REF);
        getCvObject(CvFuzzyType.class, CvFuzzyType.CERTAIN, CvFuzzyType.CERTAIN_MI_REF);
        getCvObject(CvDatabase.class, "uniprot-taxonomy", "MI:0942");

        getCvObject(CvTopic.class, CvTopic.ISOFORM_COMMENT);
        getCvObject(CvTopic.class, CvTopic.CHAIN_SEQ_START);
        getCvObject(CvTopic.class, CvTopic.CHAIN_SEQ_END);
        getCvObject(CvTopic.class, CvTopic.INVALID_RANGE);
        getCvObject(CvTopic.class, "invalid-positions");
        getCvObject(CvTopic.class, "range-conflicts");
        getCvObject(CvTopic.class, "sequence-version");
        getCvObject(CvTopic.class, CvTopic.NON_UNIPROT);
        getCvObject(CvTopic.class, CvTopic.CAUTION, CvTopic.CAUTION_MI_REF);
        getCvObject(CvXrefQualifier.class, CvXrefQualifier.UNIPROT_REMOVED_AC);
        getCvObject(CvXrefQualifier.class, "intact-secondary");
    }
}
