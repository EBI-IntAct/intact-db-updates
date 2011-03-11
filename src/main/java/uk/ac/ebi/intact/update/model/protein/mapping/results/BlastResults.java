package uk.ac.ebi.intact.update.model.protein.mapping.results;

import uk.ac.ebi.intact.bridges.ncbiblast.model.BlastProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.update.model.HibernatePersistentImpl;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.BlastReport;

import javax.persistence.*;

/**
 * The annotated class containing the basic Blast results
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>17-May-2010</pre>
 */
@Entity
@Table(name = "ia_blast_results")
public class BlastResults extends HibernatePersistentImpl {

    private BlastProtein blastProtein;

    /**
     * the taxId of the protein identified in these blast results
     */
    private int taxId;

    /**
     * The trembl accession of the protein we tried to remap using the swissprot remapping process. Can be null if the blast has directly be
     * done on a anonymous sequence.
     */
    private String tremblAccession;

    /**
     * The parent of this object.
     */
    private BlastReport blastReport;

    /**
     * Create a new BlastResults instance
     */
    public BlastResults() {
        taxId = 0;
        this.tremblAccession = null;
        this.blastProtein = new BlastProtein();
    }

    /**
     * Create a new BlastResults instance from a previous BlastProtein instance
     * @param protein : the Blastprotein instance
     */
    public BlastResults(BlastProtein protein) {
        if( protein == null ) {
            throw new IllegalArgumentException( "You must give a non null protein" );
        }
        this.blastProtein = protein;
        this.tremblAccession = null;
    }

    /**
     *
     * @return The trembl accession we want to remap using the swissprto remapping process, null if it is a blast from anonymous sequence
     */
    @Column(name = "trembl_accession_remapping", nullable = true, length = 20)
    public String getTremblAccession() {
        return tremblAccession;
    }

    /**
     * Set the trembl accession
     * @param tremblAccession
     */
    public void setTremblAccession(String tremblAccession) {
        this.tremblAccession = tremblAccession;
    }

    /**
     *
     * @return The taxId
     */
    @Column(name = "taxId")
    public int getTaxId() {
        return taxId;
    }

    /**
     * Set the taxId
     * @param taxId
     */
    public void setTaxId(int taxId) {
        this.taxId = taxId;
    }

    /**
     *
     * @return the accession of the protein
     */
    @Column(name = "protein_ac", nullable = false)
    public String getAccession() {
        if (this.blastProtein != null){
            return this.blastProtein.getAccession();
        }
        return null;
    }

    /**
     *
     * @return the start position of the alignment in the query
     */
    @Column(name = "start_query", nullable = false)
    public int getStartQuery() {
        if (this.blastProtein != null){
            return this.blastProtein.getStartQuery();
        }
        return 0;
    }

    /**
     *
     * @return the end position of the alignment in the query
     */
    @Column(name = "end_query", nullable = false)
    public int getEndQuery() {
        if (this.blastProtein != null){
            return this.blastProtein.getEndQuery();
        }
        return 0;
    }

    /**
     * Set the start position of the alignment in the query
     * @param startQuery
     */
    public void setStartQuery(int startQuery) {
        if (this.blastProtein == null){
            this.blastProtein = new BlastProtein();
        }

        this.blastProtein.setStartQuery(startQuery);
    }

    /**
     * Set the end position of the alignment in the query
     * @param endQuery
     */
    public void setEndQuery(int endQuery) {
        if (this.blastProtein == null){
            this.blastProtein = new BlastProtein();
        }

        this.blastProtein.setEndQuery(endQuery);
    }

    /**
     *
     * @return The uniprot protein
     */
    @Transient
    public UniprotProtein getUniprotProtein() {
        if (this.blastProtein != null){
            return this.blastProtein.getUniprotProtein();
        }
        return null;
    }

    /**
     * Set the uniprot protein
     * @param prot
     */
    public void setUniprotProtein(UniprotProtein prot) {
        if (this.blastProtein == null){
            this.blastProtein = new BlastProtein();
        }

        this.blastProtein.setUniprotProtein(prot);
    }

    /**
     *
     * @return The sequence
     */
    @Lob
    @Column(name = "match_sequence")
    public String getSequence() {
        if (this.blastProtein != null){
            return this.blastProtein.getSequence();
        }
        return null;
    }

    /**
     *
     * @return  the database name
     */
    @Column(name = "database", nullable = false)
    public String getDatabase() {
        if (this.blastProtein != null){
            return this.blastProtein.getDatabase();
        }
        return null;
    }

    /**
     *
     * @return the identity
     */
    @Column(name = "identity", nullable = false)
    public float getIdentity() {
        if (this.blastProtein != null){
            return this.blastProtein.getIdentity();
        }
        return 0;
    }

    /**
     *
     * @return the start match
     */
    @Column(name = "start_match", nullable = false)
    public int getStartMatch() {
        if (this.blastProtein != null){
            return this.blastProtein.getStartMatch();
        }
        return 0;
    }

    /**
     *
     * @return  the end match
     */
    @Column(name = "end_match", nullable = false)
    public int getEndMatch() {
        if (this.blastProtein != null){
            return this.blastProtein.getEndMatch();
        }
        return 0;
    }

    /**
     * Set the accession
     * @param accession
     */
    public void setAccession(String accession) {
        if (this.blastProtein == null){
            this.blastProtein = new BlastProtein();
        }

        this.blastProtein.setAccession(accession);
    }

    /**
     * Set the sequence
     * @param sequence
     */
    public void setSequence(String sequence) {
        if (this.blastProtein == null){
            this.blastProtein = new BlastProtein();
        }

        this.blastProtein.setSequence(sequence);
    }

    /**
     * Set the identity
     * @param identity
     */
    public void setIdentity(float identity) {
        if (this.blastProtein == null){
            this.blastProtein = new BlastProtein();
        }

        this.blastProtein.setIdentity(identity);
    }

    /**
     * Set the database
     * @param database
     */
    public void setDatabase(String database) {
        if (this.blastProtein == null){
            this.blastProtein = new BlastProtein();
        }

        this.blastProtein.setDatabase(database);
    }

    /**
     * Set the start match
     * @param startMatch
     */
    public void setStartMatch(int startMatch) {
        if (this.blastProtein == null){
            this.blastProtein = new BlastProtein();
        }

        this.blastProtein.setStartMatch(startMatch);
    }

    /**
     * Set the end match
     * @param endMatch
     */
    public void setEndMatch(int endMatch) {
        if (this.blastProtein == null){
            this.blastProtein = new BlastProtein();
        }

        this.blastProtein.setEndMatch(endMatch);
    }

    /**
     *
     * @return  the description
     */
    public String getDescription() {
        if (this.blastProtein != null){
            return this.blastProtein.getDescription();
        }
        return null;
    }

    /**
     * Set the description
     * @param description
     */
    @Column(name = "protein_description")
    public void setDescription(String description) {
        if (this.blastProtein == null){
            this.blastProtein = new BlastProtein();
        }

        this.blastProtein.setDescription(description);
    }

    /**
     *
     * @return the alignment
     */
    @Lob
    @Column(name = "alignment")
    public String getAlignment() {
        if (this.blastProtein != null){
            return this.blastProtein.getAlignment();
        }
        return null;
    }

    /**
     * Set the alignment
     * @param alignment
     */
    public void setAlignment(String alignment) {
        if (this.blastProtein == null){
            this.blastProtein = new BlastProtein();
        }

        this.blastProtein.setAlignment(alignment);
    }

    /**
     *
     * @return the parent report
     */
    @ManyToOne
    @JoinColumn(name = "blast_report_id")
    public BlastReport getBlastReport() {
        return blastReport;
    }

    /**
     * Set the parent report
     * @param blastReport
     */
    public void setBlastReport(BlastReport blastReport) {
        this.blastReport = blastReport;
    }
}
