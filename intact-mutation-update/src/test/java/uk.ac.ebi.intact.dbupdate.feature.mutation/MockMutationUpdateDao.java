package uk.ac.ebi.intact.dbupdate.feature.mutation;

import psidev.psi.mi.jami.model.CvTerm;
import uk.ac.ebi.intact.dbupdate.feature.mutation.helper.MutationUpdateDao;
import uk.ac.ebi.intact.jami.model.extension.IntactFeatureEvidence;

import java.util.Collection;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class MockMutationUpdateDao implements MutationUpdateDao {

    @Override
    public Collection<IntactFeatureEvidence> getFeatureEvidenceByType(String term) {
        return null;
    }

    @Override
    public void doUpdate(IntactFeatureEvidence intactFeatureEvidence) {

    }

    @Override
    public CvTerm getCVTermByAc(String ac) {
        return null;
    }
}
