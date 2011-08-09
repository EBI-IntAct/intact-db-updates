package uk.ac.ebi.intact.update.model.protein.mapping.results;

import org.apache.commons.collections.CollectionUtils;
import uk.ac.ebi.intact.protein.mapping.actions.ActionName;
import uk.ac.ebi.intact.protein.mapping.results.IdentificationResults;
import uk.ac.ebi.intact.update.model.HibernateUpdatePersistentImpl;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentMappingReport;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;


/**
 * This class contains all the results of the protein identification process.
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>24-Mar-2010</pre>
 */
@Entity
@Table( name = "ia_mapping_result" )
public class PersistentIdentificationResults extends HibernateUpdatePersistentImpl implements IdentificationResults<PersistentMappingReport>{

    /**
     * the unique uniprot id identifying the protein
     */
    private String finalUniprotId;

    /**
     * the list of actions done to identify the protein
     */
    private List<PersistentMappingReport> listOfActions = new ArrayList<PersistentMappingReport>();

    /**
     * Create a new Identificationresult
     */
    public PersistentIdentificationResults(){
        this.finalUniprotId = null;
    }

    public void setListOfActions(List<PersistentMappingReport> listOfActions) {
        this.listOfActions = listOfActions;
    }

    /**
     * set the final uniprot accession identifying the protein
     * @param id : uniprot accession
     */
    public void setFinalUniprotId(String id){
        this.finalUniprotId = id;
    }

    /**
     *
     * @return the final uniprot accession identifying the protein
     */
    @Column(name="final_uniprot", length = 10)
    public String getFinalUniprotId(){
        return this.finalUniprotId;
    }

    /**
     *
     * @return true if the unique uniprot id is not null
     */
    @Transient
    public boolean hasUniqueUniprotId(){
        return this.finalUniprotId != null;
    }

    /**
     *
     * @return the list of actions done to identify the protein
     */
    @OneToMany(mappedBy = "updateResult", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
    public List<PersistentMappingReport> getListOfActions(){
        return this.listOfActions;
    }

    /**
     * add a new action report to the list of reports
     * @param report : action report
     */
    public boolean addActionReport(PersistentMappingReport report){
        if (this.listOfActions.add(report)){

            report.setUpdateResult(this);
            return true;
        }
        return false;
    }

    public boolean removeActionReport(PersistentMappingReport report){
        if (this.listOfActions.remove(report)){
            report.setUpdateResult(null);
            return true;
        }
        return false;
    }

    /**
     *
     * @return the last action report added to this result
     */
    @Transient
    public PersistentMappingReport getLastAction(){
        if (listOfActions.isEmpty()){
            return null;
        }
        return this.listOfActions.get(this.listOfActions.size() - 1);
    }

    /**
     *
     * @param name : name of a specific action
     * @return the list of actions with this specific name which have been done to identify the protein
     */
    @Transient
    public List<PersistentMappingReport> getActionsByName(ActionName name){
        ArrayList<PersistentMappingReport> reports = new ArrayList<PersistentMappingReport>();

        for (PersistentMappingReport action : this.listOfActions){
            if (action.getName() != null && action.getName().equals(name)){
                reports.add(action);
            }
        }
        return reports;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final PersistentIdentificationResults results = (PersistentIdentificationResults) o;

        if ( finalUniprotId != null ) {
            if (!finalUniprotId.equals( results.getFinalUniprotId() )){
                return false;
            }
        }
        else if (results.getFinalUniprotId()!= null){
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

        if ( finalUniprotId != null ) {
            code = 29 * code + finalUniprotId.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final PersistentIdentificationResults results = (PersistentIdentificationResults) o;

        if ( finalUniprotId != null ) {
            if (!finalUniprotId.equals( results.getFinalUniprotId() )){
                return false;
            }
        }
        else if (results.getFinalUniprotId()!= null){
            return false;
        }

        return CollectionUtils.isEqualCollection(this.listOfActions, results.getListOfActions());
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("Identification result : [" + finalUniprotId != null ? finalUniprotId : "none");

        buffer.append("] \n");

        return buffer.toString();
    }
}

