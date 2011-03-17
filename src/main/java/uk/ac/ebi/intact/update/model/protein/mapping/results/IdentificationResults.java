package uk.ac.ebi.intact.update.model.protein.mapping.results;

import org.hibernate.annotations.Cascade;
import uk.ac.ebi.intact.update.model.HibernatePersistentImpl;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.ActionName;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.MappingReport;

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
@MappedSuperclass
public class IdentificationResults extends HibernatePersistentImpl{

    /**
     * the unique uniprot id identifying the protein
     */
    private String finalUniprotId;

    /**
     * the list of actions done to identify the protein
     */
    private List<MappingReport> listOfActions = new ArrayList<MappingReport>();

    /**
     * Create a new Identificationresult
     */
    public IdentificationResults(){
        this.finalUniprotId = null;
    }

    public void setListOfActions(List<MappingReport> listOfActions) {
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
    @Column(name="uniprot_ac", length = 10)
    public String getFinalUniprotId(){
        return this.finalUniprotId;
    }

    /**
     *
     * @return true if the unique uniprot id is not null
     */
    public boolean hasUniqueUniprotId(){
        return this.finalUniprotId != null;
    }

    /**
     *
     * @return the list of actions done to identify the protein
     */
    @OneToMany(mappedBy = "updateResult", cascade = CascadeType.ALL)
    @Cascade( value = org.hibernate.annotations.CascadeType.SAVE_UPDATE )
    public List<MappingReport> getListOfActions(){
        return this.listOfActions;
    }

    /**
     * add a new action report to the list of reports
     * @param report : action report
     */
    public void addActionReport(MappingReport report){
        this.listOfActions.add(report);
    }

    /**
     *
     * @return the last action report added to this result
     */
    @Transient
    public MappingReport getLastAction(){
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
    public List<MappingReport> getActionsByName(ActionName name){
        ArrayList<MappingReport> reports = new ArrayList<MappingReport>();

        for (MappingReport action : this.listOfActions){
            if (action.getName() != null && action.getName().equals(name)){
                reports.add(action);
            }
        }
        return reports;
    }
}
