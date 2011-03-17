package uk.ac.ebi.intact.update.model.protein.mapping.results;


import uk.ac.ebi.intact.update.model.protein.mapping.actions.MappingReport;

import javax.persistence.*;

/**
 * An UpdateResult contains all the results and ActionReports of the update process of a protein
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>11-May-2010</pre>
 */
@Entity
@Table( name = "ia_mapping_result" )
public class UpdateMappingResults extends IdentificationResults {

    /**
     * The intact accession of the protein to update
     */
    private String intactAccession;

    /**
     * Create a new UpdateResult instance
     */
    public UpdateMappingResults(){
        super();
        this.intactAccession = null;
    }

    /**
     *
     * @return  the intact accession
     */
    @Column(name = "protein_ac", nullable = false, length = 30)
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
     public void addActionReport(MappingReport report){
         report.setUpdateResult(this);
        super.addActionReport(report);
    }
}
