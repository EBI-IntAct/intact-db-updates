package uk.ac.ebi.intact.update.model.protein.mapping.results;

import org.apache.commons.collections.CollectionUtils;
import uk.ac.ebi.intact.protein.mapping.results.PICRCrossReferences;
import uk.ac.ebi.intact.update.model.HibernateUpdatePersistentImpl;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentPICRReport;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * This class contains the cross references that returns PICR for an identifier/sequence
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-May-2010</pre>
 */
@Entity
@Table(name = "ia_picr_xrefs")
public class PersistentPICRCrossReferences extends HibernateUpdatePersistentImpl implements PICRCrossReferences {

    /**
     * The database name returned by PICR
     */
    private String database;

    /**
     * The list of accessions from this database PICR returned
     */
    private Set<String> accessions = new HashSet<String>();

    /**
     * The updateProcess report
     */
    private PersistentPICRReport picrReport;

    /**
     * Create a new PersistentPICRCrossReferences instance
     */
    public PersistentPICRCrossReferences() {
        database = null;
    }

    /**
     *
     * @return the database name
     */
    @Column(name = "database", nullable = false)
    public String getDatabase() {
        return database;
    }

    /**
     *
     * @return the list of accessions
     */
    @Transient
    public Set<String> getAccessions() {
        return accessions;
    }

    /**
     *
     * @return the list of accessions as a String, separated by a semi-colon
     */
    @Column(name = "accession", nullable = false, length = 500)
    public String getListOfAccessions(){

        if (this.accessions.isEmpty()){
            return null;
        }
        StringBuffer concatenedList = new StringBuffer( 1064 );

        for (String ref : this.accessions){
            concatenedList.append(ref+";");
        }

        if (concatenedList.length() > 0){
            concatenedList.deleteCharAt(concatenedList.length() - 1);
        }

        return concatenedList.toString();
    }

    /**
     * set the list of accessions
     * @param possibleAccessions : the list of accessions as a String, separated by a semi colon
     */
    public void setListOfAccessions(String possibleAccessions){
        this.accessions.clear();

        if (possibleAccessions != null){
            if (possibleAccessions.contains(";")){
                String [] list = possibleAccessions.split(";");

                for (String s : list){
                    this.accessions.add(s);
                }
            }
            else {
                this.accessions.add(possibleAccessions);
            }
        }
    }

    /**
     * Set the database name
     * @param database
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * Set the list of accessions
     * @param accessions
     */
    public void setAccessions(Set<String> accessions) {
        this.accessions = accessions;
    }

    /**
     * Add a new accession
     * @param accession
     */
    public void addAccession(String accession){
        this.accessions.add(accession);
    }

    /**
     *
     * @return the updateProcess report
     */
    @ManyToOne
    @JoinColumn(name="picr_report_id")
    public PersistentPICRReport getPicrReport() {
        return picrReport;
    }

    /**
     * Set the updateProcess report
     * @param picrReport
     */
    public void setPicrReport(PersistentPICRReport picrReport) {
        this.picrReport = picrReport;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final PersistentPICRCrossReferences refs = (PersistentPICRCrossReferences) o;

        if ( database != null ) {
            if (!database.equals( refs.getDatabase() )){
                return false;
            }
        }
        else if (refs.getDatabase()!= null){
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

        if ( database != null ) {
            code = 29 * code + database.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final PersistentPICRCrossReferences refs = (PersistentPICRCrossReferences) o;

        if ( database != null ) {
            if (!database.equals( refs.getDatabase() )){
                return false;
            }
        }
        else if (refs.getDatabase()!= null){
            return false;
        }

        return CollectionUtils.isEqualCollection(this.accessions, refs.getAccessions());
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("PICR cross reference : [" + database != null ? database : "");

        return buffer.toString();
    }
}

