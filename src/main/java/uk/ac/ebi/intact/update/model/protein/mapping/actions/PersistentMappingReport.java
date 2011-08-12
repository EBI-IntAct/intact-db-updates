package uk.ac.ebi.intact.update.model.protein.mapping.actions;

import org.apache.commons.collections.CollectionUtils;
import uk.ac.ebi.intact.protein.mapping.actions.ActionName;
import uk.ac.ebi.intact.protein.mapping.actions.status.Status;
import uk.ac.ebi.intact.protein.mapping.actions.status.StatusLabel;
import uk.ac.ebi.intact.protein.mapping.model.actionReport.MappingReport;
import uk.ac.ebi.intact.update.model.HibernateUpdatePersistentImpl;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PersistentIdentificationResults;

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
@DiscriminatorColumn(name="type", discriminatorType= DiscriminatorType.STRING, length = 100)
@DiscriminatorValue("default_report")
@Table( name = "ia_mapping_report" )
public class PersistentMappingReport extends HibernateUpdatePersistentImpl implements MappingReport {

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
     * The updateProcess object of this report. We store the updateProcess of this object only if it is an instance of UpdateMappingResults
     */
    protected PersistentIdentificationResults updateResult;

    public PersistentMappingReport(){
        this.name = null;
    }
    /**
     * Create a new report for an action with a specific name
     * @param name the naem of the action
     */
    public PersistentMappingReport(ActionName name){
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
    @JoinTable(name = "ia_action2warn", joinColumns = @JoinColumn(name="action_id"))
    @Column(name = "warnings", nullable = false)
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
    @ElementCollection
    @JoinTable(name = "ia_action2uniprot", joinColumns = @JoinColumn(name="action_id"))
    @Column(name = "uniprot_ac", nullable = false)
    public Set<String> getPossibleAccessions(){
        return this.possibleAccessions;
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
    public void setIsASwissprotEntry(boolean isSwissprot){
        this.isASwissprotEntry = isSwissprot;
    }

    public void setASwissprotEntry(boolean isSwissprot){
        this.isASwissprotEntry = isSwissprot;
    }

    /**
     *
     * @return  the updateProcess object of this action. If the updateProcess is not an instance of UpdateMappingResults, the updateProcess returned is null
     */
    @ManyToOne
    @JoinColumn(name="result_id")
    public PersistentIdentificationResults getUpdateResult() {
        return updateResult;
    }

    /**
     * Set the updateProcess of this object only if it is an instance of UpdateMappingResults
     * @param result
     */
    public void setUpdateResult(PersistentIdentificationResults result) {
        this.updateResult = result;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final PersistentMappingReport report = (PersistentMappingReport) o;

        if ( name != null ) {
            if (!name.equals( report.getName() )){
                return false;
            }
        }
        else if (report.getName()!= null){
            return false;
        }

        if ( status != null ) {
            if (!status.equals( report.getStatus() )){
                return false;
            }
        }
        else if (report.getStatus()!= null){
            return false;
        }

        if (isASwissprotEntry != report.isASwissprotEntry()){
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

        code = 29 * code + super.hashCode();

        if ( name != null ) {
            code = 29 * code + name.hashCode();
        }

        if ( status != null ) {
            code = 29 * code + status.hashCode();
        }

        code = 29 * code + Boolean.toString(isASwissprotEntry).hashCode();

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final PersistentMappingReport report = (PersistentMappingReport) o;

        if ( name != null ) {
            if (!name.equals( report.getName() )){
                return false;
            }
        }
        else if (report.getName()!= null){
            return false;
        }

        if ( status != null ) {
            if (!status.equals( report.getStatus() )){
                return false;
            }
        }
        else if (report.getStatus()!= null){
            return false;
        }

        if (isASwissprotEntry != report.isASwissprotEntry()){
            return false;
        }

        if (!CollectionUtils.isEqualCollection(this.warnings, report.getWarnings())){
            return false;
        }

        return CollectionUtils.isEqualCollection(this.possibleAccessions, report.getPossibleAccessions());
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("Mapping report : [" + name != null ? name.toString() : "");

        buffer.append(", " + status != null ? status.toString() : "");

        buffer.append("] \n");

        buffer.append(" Is A Swissprot entry : " + isASwissprotEntry + "\n");

        return buffer.toString();
    }
}
