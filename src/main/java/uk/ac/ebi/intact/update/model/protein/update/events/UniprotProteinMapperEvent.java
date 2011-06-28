package uk.ac.ebi.intact.update.model.protein.update.events;

import org.hibernate.annotations.DiscriminatorFormula;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PersistentIdentificationResults;
import uk.ac.ebi.intact.update.model.protein.update.UpdateProcess;

import javax.persistence.*;

/**
 * Event for remapping of non uniprot proteins
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>16/03/11</pre>
 */
@Entity
@DiscriminatorFormula("objclass")
@DiscriminatorValue("UniprotProteinMapperEvent")
public class UniprotProteinMapperEvent extends PersistentProteinEvent {

    PersistentIdentificationResults identificationResults;

    public UniprotProteinMapperEvent(){
        super();
    }

    public UniprotProteinMapperEvent(UpdateProcess process, EventName name, Protein protein, PersistentIdentificationResults identificationResults){
        super(process, name, protein);

        this.identificationResults = identificationResults;
    }

    @OneToOne(orphanRemoval = true, cascade = {
            CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
    @JoinColumn(name = "identification_id")
    public PersistentIdentificationResults getIdentificationResults() {
        return identificationResults;
    }

    public void setIdentificationResults(PersistentIdentificationResults identificationResults) {
        this.identificationResults = identificationResults;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final UniprotProteinMapperEvent event = ( UniprotProteinMapperEvent ) o;

        if ( identificationResults != null ) {
            if (!identificationResults.equals( event.getIdentificationResults())){
                return false;
            }
        }
        else if (event.getIdentificationResults()!= null){
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

        if ( identificationResults != null ) {
            code = 29 * code + identificationResults.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final UniprotProteinMapperEvent event = ( UniprotProteinMapperEvent ) o;

        if ( identificationResults != null ) {
            if (!identificationResults.isIdenticalTo( event.getIdentificationResults())){
                return false;
            }
        }
        else if (event.getIdentificationResults()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Uniprot proteinAc remapping event : \n");

        buffer.append(identificationResults != null ? identificationResults.toString() : "No results.");

        return buffer.toString();
    }
}
