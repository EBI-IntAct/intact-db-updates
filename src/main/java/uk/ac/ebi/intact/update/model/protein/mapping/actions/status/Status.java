package uk.ac.ebi.intact.update.model.protein.mapping.actions.status;

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

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof Status ) ) {
            return false;
        }

        final Status status = ( Status ) o;

        if ( label != null ) {
            if (!label.equals( status.getLabel() )){
                return false;
            }
        }
        else if (status.getLabel() != null){
            return false;
        }

        if ( description != null ) {
            if (!description.equals( status.getDescription() )){
                return false;
            }
        }
        else if (status.getDescription() != null){
            return false;
        }

        return true;
    }

    /**
     * This class overwrites equals. To ensure proper functioning of HashTable,
     * hashCode must be overwritten, too.
     *
     * @return hash code of the object.
     */
    @Override
    public int hashCode() {

        int code = 29;

        if ( label != null ) {
            code = 29 * code + label.hashCode();
        }

        if ( null != description ) {
            code = 29 * code + description.hashCode();
        }

        return code;
    }

    @Override
    public String toString() {
        return "Status [label: " + ( label != null ? label.toString() : "" ) +
                ", description: " + description + "]";
    }
}
