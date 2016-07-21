package uk.ac.ebi.intact.dbupdate.feature.mutation.actions;


import uk.ac.ebi.intact.tools.feature.shortlabel.generator.events.ModifiedMutationShortlabelEvent;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public interface MutationUpdate {

    void updateMutation(ModifiedMutationShortlabelEvent event);
}
