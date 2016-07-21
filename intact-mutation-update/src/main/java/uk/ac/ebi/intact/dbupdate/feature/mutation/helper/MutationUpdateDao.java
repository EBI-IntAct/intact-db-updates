package uk.ac.ebi.intact.dbupdate.feature.mutation.helper;

import psidev.psi.mi.jami.model.CvTerm;
import uk.ac.ebi.intact.jami.model.extension.IntactFeatureEvidence;

import java.util.Collection;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public interface MutationUpdateDao {

    public Collection<IntactFeatureEvidence> getFeatureEvidenceByType(String term);

    public void doUpdate(IntactFeatureEvidence intactFeatureEvidence);

    public CvTerm getCVTermByAc(String ac);
}
