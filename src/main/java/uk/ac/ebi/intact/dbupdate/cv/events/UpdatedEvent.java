package uk.ac.ebi.intact.dbupdate.cv.events;

import uk.ac.ebi.intact.model.Annotation;
import uk.ac.ebi.intact.model.CvObjectAlias;
import uk.ac.ebi.intact.model.CvObjectXref;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;

/**
 * Event fired when a CvObject is updated
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>11/11/11</pre>
 */

public class UpdatedEvent extends EventObject {

    private String termAccession;
    private String intactAc;
    private String updatedShortLabel;
    private String updatedFullName;

    private Collection<CvObjectXref> createdXrefs = new ArrayList<CvObjectXref>();
    private Collection<CvObjectXref> updatedXrefs = new ArrayList<CvObjectXref>();
    private Collection<CvObjectXref> deletedXrefs = new ArrayList<CvObjectXref>();

    private Collection<CvObjectAlias> createdAliases = new ArrayList<CvObjectAlias>();
    private Collection<CvObjectAlias> updatedAliases= new ArrayList<CvObjectAlias>();
    private Collection<CvObjectAlias> deletedAliases = new ArrayList<CvObjectAlias>();

    private Collection<Annotation> createdAnnotations = new ArrayList<Annotation>();
    private Collection<Annotation> updatedAnnotations = new ArrayList<Annotation>();
    private Collection<Annotation> deletedAnnotations = new ArrayList<Annotation>();

    private Collection<String> createdParents = new ArrayList<String>();
    private Collection<String> deletedParents = new ArrayList<String>();
    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public UpdatedEvent(Object source, String termAc, String intactAc, String updatedLabel, String updatedFullName) {
        super(source);

        this.termAccession = termAc;
        this.intactAc = intactAc;
        this.updatedShortLabel = updatedLabel;
        this.updatedFullName = updatedFullName;
    }

    public String getTermAccession() {
        return termAccession;
    }

    public String getUpdatedShortLabel() {
        return updatedShortLabel;
    }

    public String getIntactAc() {
        return intactAc;
    }

    public String getUpdatedFullName() {
        return updatedFullName;
    }

    public Collection<CvObjectXref> getCreatedXrefs() {
        return createdXrefs;
    }

    public Collection<CvObjectXref> getUpdatedXrefs() {
        return updatedXrefs;
    }

    public Collection<CvObjectXref> getDeletedXrefs() {
        return deletedXrefs;
    }

    public Collection<CvObjectAlias> getCreatedAliases() {
        return createdAliases;
    }

    public Collection<CvObjectAlias> getUpdatedAliases() {
        return updatedAliases;
    }

    public Collection<CvObjectAlias> getDeletedAliases() {
        return deletedAliases;
    }

    public Collection<Annotation> getCreatedAnnotations() {
        return createdAnnotations;
    }

    public Collection<Annotation> getUpdatedAnnotations() {
        return updatedAnnotations;
    }

    public Collection<Annotation> getDeletedAnnotations() {
        return deletedAnnotations;
    }

    public Collection<String> getCreatedParents() {
        return createdParents;
    }

    public Collection<String> getDeletedParents() {
        return deletedParents;
    }
}
