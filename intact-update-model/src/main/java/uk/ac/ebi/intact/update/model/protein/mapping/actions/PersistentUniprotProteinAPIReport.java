package uk.ac.ebi.intact.update.model.protein.mapping.actions;

import org.apache.commons.collections.CollectionUtils;
import uk.ac.ebi.intact.protein.mapping.actions.ActionName;
import uk.ac.ebi.intact.protein.mapping.model.actionReport.UniprotProteinAPIReport;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PersistentUniprotProteinAPICrossReferences;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

/**
 * This report aims at storing the information and results of a query on UniprotProteinAPI
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>01-Apr-2010</pre>
 */
@Entity
@DiscriminatorValue("picr_report")
public class PersistentUniprotProteinAPIReport extends PersistentMappingReport implements UniprotProteinAPIReport<PersistentUniprotProteinAPICrossReferences> {

    /**
     * the list of cross references that UniprotProteinAPI could collect
     */
    private Set<PersistentUniprotProteinAPICrossReferences> crossReferences = new HashSet<PersistentUniprotProteinAPICrossReferences>();

    public PersistentUniprotProteinAPIReport() {
        super();
    }
    /**
     * Create a new PersistentUniprotProteinAPIReport
     * @param name : name of the action
     */
    public PersistentUniprotProteinAPIReport(ActionName name) {
        super(name);
    }

    /**
     *
     * @return the cross references
     */
    @OneToMany(mappedBy = "uniprotProteinAPIReport", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
    public Set<PersistentUniprotProteinAPICrossReferences> getCrossReferences(){
        return this.crossReferences;
    }

    /**
     * add a new cross reference
     * @param databaseName : database name
     * @param accession : accessions in the database
     */
    public void addCrossReference(String databaseName, String accession){
        boolean isADatabaseNamePresent = false;

        for (PersistentUniprotProteinAPICrossReferences c : this.crossReferences){
            if (c.getDatabase() != null){
                if (c.getDatabase().equalsIgnoreCase(databaseName)){
                    isADatabaseNamePresent = true;
                    c.addAccession(accession);
                }
            }
        }

        if (!isADatabaseNamePresent){
            PersistentUniprotProteinAPICrossReferences uniprotProteinAPIRefs = new PersistentUniprotProteinAPICrossReferences();
            uniprotProteinAPIRefs.setUniprotProteinAPIReport(this);
            uniprotProteinAPIRefs.setDatabase(databaseName);
            uniprotProteinAPIRefs.addAccession(accession);
        }
    }

    /**
     * Add a new Uniprot Protein API cross reference instance to the list of references
     * @param refs : the Uniprot Protein API cross reference instance to add
     */
    public void addUniprotProteinAPICrossReference(PersistentUniprotProteinAPICrossReferences refs){
        if (refs != null){
            refs.setUniprotProteinAPIReport(this);
            this.crossReferences.add(refs);
        }
    }

    /**
     * Set the Uniprot Protein API cross references
     * @param crossReferences : set containing the UniprotProteinAPI cross references
     */
    public void setCrossReferences(Set<PersistentUniprotProteinAPICrossReferences> crossReferences) {
        this.crossReferences = crossReferences;
    }

    @Override
    public boolean equals( Object o ) {
        return super.equals(o);
    }

    /**
     * This class overwrites equals. To ensure proper functioning of HashTable,
     * hashCode must be overwritten, too.
     *
     * @return hash code of the object.
     */
    @Override
    public int hashCode() {

        return super.hashCode();
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final PersistentUniprotProteinAPIReport report = (PersistentUniprotProteinAPIReport) o;

        return CollectionUtils.isEqualCollection(this.crossReferences, report.getCrossReferences());
    }
}
