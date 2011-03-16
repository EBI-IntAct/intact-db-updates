package uk.ac.ebi.intact.update.model.protein.mapping.actions;

import uk.ac.ebi.intact.update.model.HibernatePersistentImpl;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.status.Status;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.status.StatusLabel;
import uk.ac.ebi.intact.update.model.protein.mapping.results.UpdateMappingResults;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class contains all the information/ results that an action can store
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>01-Apr-2010</pre>
 */
@Entity
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="objclass", discriminatorType= DiscriminatorType.STRING, length = 100)
@DiscriminatorValue("ActionReport")
@Table( name = "ia_mapping_report" )
public class ActionReport extends HibernatePersistentImpl {

    /**
     * the name of the action
     */
    protected ActionName name;

    /**
     * the status of the action
     */
    protected Status status;

    /**
     * a list of warnings
     */
    protected List<String> warnings = new ArrayList<String>();

    /**
     * the list of possible uniprot proteins which need to be reviewed by a curator
     */
    protected Set<String> possibleAccessions = new HashSet<String>();

    /**
     * boolean value to know if the unique uniprot id that this action retrieved is a swissprot entry
     */
    private boolean isASwissprotEntry = false;

    /**
     * The parent object of this report. We store the parent of this object only if it is an instance of UpdateMappingResults
     */
    protected UpdateMappingResults updateResult;

    /**
     * Create a new report for an action with a specific name
     * @param name the naem of the action
     */
    public ActionReport(ActionName name){
        this.name = name;
    }

    /**
     *
     * @return the name of the action
     */
    @Column(name = "name", nullable = false)
    @Enumerated(EnumType.STRING)
    public ActionName getName(){
        return this.name;
    }

    /**
     * set a new name for this report
     * @param name : new name
     */
    public void setName(ActionName name){
        this.name = name;
    }

    /**
     *
     * @return the warnings
     */
    @ElementCollection
    @JoinTable(name = "ia_action2warning", joinColumns = @JoinColumn(name="action_id"))
    @Column(name = "warnings", nullable = false)
    // TODO change the annotation with @elementCollection when we will change the version of hibernate
    public List<String> getWarnings(){
        return this.warnings;
    }

    /**
     * add a warning to the list of warnings
     * @param warn : new warning
     */
    public void addWarning(String warn){
        this.warnings.add(warn);
    }

    /**
     *
     * @return the list of possible uniprot accessions
     */
    @Transient
    public Set<String> getPossibleAccessions(){
        return this.possibleAccessions;
    }

    /**
     *
     * @return  the list of possible Uniprot accession separated by a semi-colon in a String.
     */
    @Column(name = "possible_uniprot", nullable = true, length = 500)
    public String getListOfPossibleAccessions(){

        if (this.possibleAccessions.isEmpty()){
            return null;
        }
        StringBuffer concatenedList = new StringBuffer( 1064 );

        for (String prot : this.possibleAccessions){
            concatenedList.append(prot+";");
        }

        if (concatenedList.length() > 0){
            concatenedList.deleteCharAt(concatenedList.length() - 1);
        }

        return concatenedList.toString();
    }

    /**
     * Set the list of possible Uniprot accessions
     * @param possibleAccessions : the list of possible accessions separated by a semi-colon
     */
    public void setListOfPossibleAccessions(String possibleAccessions){
        this.possibleAccessions.clear();

        if (possibleAccessions != null){
            if (possibleAccessions.contains(";")){
                String [] list = possibleAccessions.split(";");

                for (String s : list){
                    this.possibleAccessions.add(s);
                }
            }
            else {
                this.possibleAccessions.add(possibleAccessions);
            }
        }
    }

    /**
     * Set the warnings
     * @param warnings
     */
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    /**
     * set the possible accessions
     * @param possibleAccessions
     */
    public void setPossibleAccessions(Set<String> possibleAccessions) {
        this.possibleAccessions = possibleAccessions;
    }

    /**
     * add a new possible uniprot accession
     * @param ac : new uniprot accession
     */
    public void addPossibleAccession(String ac){
        this.possibleAccessions.add(ac);
    }

    /**
     *
     * @return the status of this action
     */
    @Transient
    public Status getStatus() {
        return status;
    }

    /**
     *
     * @return the status label of this action. Can be FAILED, TO_BE_REVIEWED or COMPLETED. However, if the status of this object
     * is null and/or its label is null, this method return NONE
     */
    @Column(name = "status", length = 15, nullable = false)
    @Enumerated(EnumType.STRING)
    public StatusLabel getStatusLabel() {
        if (this.status == null){
            return StatusLabel.NONE;
        }
        else {
            if (this.status.getLabel() == null){
                return StatusLabel.NONE;
            }
            else {
                return this.status.getLabel();
            }
        }
    }

    /**
     * Set the status label of this action
     * @param label : the label. (COMPLETED, TO_BE_REVIEWED or FAILED)
     */
    public void setStatusLabel(StatusLabel label){

        if (label != null){
            if (this.status == null){
                status = new Status(label, null);
            }
            else {
                status.setLabel(label);
            }
        }
    }

    /**
     * Set the description of this action
     * @param description : the status description
     */
    public void setStatusDescription(String description){
        if (this.status == null){
            status = new Status(StatusLabel.NONE, description);
        }
        else {
            status.setDescription(description);
        }
    }


    /**
     *
     * @return the status description of this action.
     */
    @Column(name = "description", nullable = true)
    public String getStatusDescription() {
        if (this.status == null){
            return null;
        }
        else {
            return this.status.getDescription();
        }
    }

    /**
     * set the status of this action
     * @param status : the status
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     *
     * @return the isASwissprotEntry boolean
     */
    @Basic
    public boolean isASwissprotEntry(){
        return this.isASwissprotEntry;
    }

    /**
     * set the isASwissprotEntry value
     * @param isSwissprot : boolean value
     */
    public void setASwissprotEntry(boolean isSwissprot){
        this.isASwissprotEntry = isSwissprot;
    }

    /**
     *
     * @return  the parent object of this action. If the parent is not an instance of UpdateMappingResults, the parent returned is null
     */
    @ManyToOne
    @JoinColumn(name="result_id")
    public UpdateMappingResults getUpdateResult() {
        return updateResult;
    }

    /**
     * Set the parent of this object only if it is an instance of UpdateMappingResults
     * @param result
     */
    public void setUpdateResult(UpdateMappingResults result) {
        this.updateResult = result;
    }
}
