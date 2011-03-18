package uk.ac.ebi.intact.update.model.protein.mapping.results;


import uk.ac.ebi.intact.update.model.protein.mapping.actions.MappingReport;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

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

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final UpdateMappingResults results = ( UpdateMappingResults ) o;

        if ( intactAccession != null ) {
            if (!intactAccession.equals( results.getIntactAccession() )){
                return false;
            }
        }
        else if (results.getIntactAccession()!= null){
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

        if ( intactAccession != null ) {
            code = 29 * code + intactAccession.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final UpdateMappingResults results = ( UpdateMappingResults ) o;

        if ( intactAccession != null ) {
            if (!intactAccession.equals( results.getIntactAccession() )){
                return false;
            }
        }
        else if (results.getIntactAccession()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("Intact Ac : [" + intactAccession != null ? intactAccession : "none");

        buffer.append("] \n");

        buffer.append(super.toString());

        return buffer.toString();
    }

}
