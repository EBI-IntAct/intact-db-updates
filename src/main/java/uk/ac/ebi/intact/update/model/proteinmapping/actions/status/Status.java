package uk.ac.ebi.intact.update.model.proteinmapping.actions.status;

/**
 * This class represents the status of an action
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>01-Apr-2010</pre>
 */

public class Status {

    /**
     * The label or name of the status
     */
    private StatusLabel label;

    /**
     * the description
     */
    private String description;

    /**
     * Create a new status with a label and a description
     * @param label : label of the status
     * @param description : description of the status
     */
    public Status(StatusLabel label, String description){
        this.label = label;
        this.description = description;
    }

    /**
     *
     * @return  the label of this object
     */
    public StatusLabel getLabel() {
        return label;
    }

    /**
     *
     * @return the description of this object
     */
    public String getDescription() {
        return description;
    }

    /**
     * set the label of this object to 'label'
     * @param label : the new label
     */
    public void setLabel(StatusLabel label) {
        this.label = label;
    }

    /**
     * set the description of this object to 'description'
     * @param description : the new description
     */
    public void setDescription(String description) {
        this.description = description;
    }


}
