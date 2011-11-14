package uk.ac.ebi.intact.dbupdate.cv;

import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyAccess;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyTermI;
import uk.ac.ebi.intact.model.CvDagObject;
import uk.ac.ebi.intact.model.CvObjectXref;

/**
 * Context of a cv update
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>14/11/11</pre>
 */

public class CvUpdateContext {

    private String identifier;
    private CvObjectXref identityXref;
    private CvDagObject cvTerm;
    private IntactOntologyTermI ontologyTerm;
    private IntactOntologyAccess ontologyAccess;
    private boolean isTermObsolete = false;

    private CvUpdateManager manager;

    public CvUpdateContext(CvUpdateManager manager){
        this.manager = manager;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public CvDagObject getCvTerm() {
        return cvTerm;
    }

    public void setCvTerm(CvDagObject cvTerm) {
        this.cvTerm = cvTerm;
    }

    public IntactOntologyTermI getOntologyTerm() {
        return ontologyTerm;
    }

    public void setOntologyTerm(IntactOntologyTermI ontologyTerm) {
        this.ontologyTerm = ontologyTerm;
    }

    public IntactOntologyAccess getOntologyAccess() {
        return ontologyAccess;
    }

    public void setOntologyAccess(IntactOntologyAccess ontologyAccess) {
        this.ontologyAccess = ontologyAccess;
    }

    public CvObjectXref getIdentityXref() {
        return identityXref;
    }

    public void setIdentityXref(CvObjectXref identityXref) {
        this.identityXref = identityXref;
    }

    public boolean isTermObsolete() {
        return isTermObsolete;
    }

    public void setTermObsolete(boolean termObsolete) {
        isTermObsolete = termObsolete;
    }

    public CvUpdateManager getManager() {
        return manager;
    }

    public void clear(){
        this.cvTerm = null;
        this.ontologyAccess = null;
        this.ontologyTerm = null;
        this.identityXref = null;
        this.isTermObsolete = false;
        this.identifier = null;
    }
}
