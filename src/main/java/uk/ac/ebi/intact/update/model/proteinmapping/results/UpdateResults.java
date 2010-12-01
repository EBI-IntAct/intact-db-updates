package uk.ac.ebi.intact.update.model.proteinmapping.results;


import uk.ac.ebi.intact.update.model.proteinmapping.actions.ActionReport;

import javax.persistence.*;

/**
 * An UpdateResult contains all the results and ActionReports of the update process of a protein
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>11-May-2010</pre>
 */
@Entity
@Table( name = "ia_update_result" )
public class UpdateResults extends IdentificationResults {

    /**
     * The intact accession of the protein to update
     */
    private String intactAccession;

    /**
     * Create a new UpdateResult instance
     */
    public UpdateResults(){
        super();
        this.intactAccession = null;
    }

    /**
     *
     * @return  the intact accession
     */
    @Column(name = "intact_ac", nullable = false, length = 30)
    public String getIntactAccession() {
        return intactAccession;
    }

    /**
     * set the intact accession
     * @param intactAccession
     */
    public void setIntactAccession(String intactAccession) {
        this.intactAccession = intactAccession;
    }

    /**
     * Add an actionReport and set this object as parent
     * @param report : action report
     */
     public void addActionReport(ActionReport report){
         report.setUpdateResult(this);
        super.addActionReport(report);
    }
}
