package uk.ac.ebi.intact.dbupdate.feature.mutation.listener;

import uk.ac.ebi.intact.dbupdate.feature.mutation.actions.MutationUpdate;
import uk.ac.ebi.intact.dbupdate.feature.mutation.actions.impl.MutationUpdateImpl;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.events.ModifiedMutationShortlabelEvent;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class UpdateListener extends AbstractShortlabelGeneratorListener {

    private MutationUpdate mutationUpdate;

    public UpdateListener() {
        this.mutationUpdate = new MutationUpdateImpl();
    }

    public void onModifiedMutationShortlabel(ModifiedMutationShortlabelEvent event) {
        mutationUpdate.updateMutation(event);
    }
}
