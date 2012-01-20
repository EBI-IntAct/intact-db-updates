package uk.ac.ebi.intact.dbupdate.cv.importer;

import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyAccess;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyTermI;
import uk.ac.ebi.intact.dbupdate.cv.CvUpdateContext;
import uk.ac.ebi.intact.dbupdate.cv.updater.CvUpdater;
import uk.ac.ebi.intact.dbupdate.cv.updater.CvUpdateException;
import uk.ac.ebi.intact.model.CvDagObject;

import java.util.Map;
import java.util.Set;

/**
 * Interface for Cv importer
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19/01/12</pre>
 */

public interface CvImporter {

    public void importCv(CvUpdateContext updateContext, boolean importChildren) throws InstantiationException, IllegalAccessException, CvUpdateException;
    public void importCv(CvUpdateContext updateContext, boolean importChildren, Class<? extends CvDagObject> termClass) throws InstantiationException, IllegalAccessException, CvUpdateException;
    public Class<? extends CvDagObject> findCvClassFor(IntactOntologyTermI ontologyTerm, IntactOntologyAccess ontologyAccess);
    public CvDagObject createCvObjectFrom(IntactOntologyTermI ontologyTerm, IntactOntologyAccess ontologyAccess, Class<? extends CvDagObject> termClass, boolean hideParents, CvUpdateContext updateContext) throws IllegalAccessException, InstantiationException, CvUpdateException;
    public Set<IntactOntologyTermI> collectDeepestChildren(IntactOntologyAccess ontologyAccess, IntactOntologyTermI parent);
    public Map<String, Class<? extends CvDagObject>> getClassMap();
    public Set<String> getProcessedTerms();
    public Map<String, Set<CvDagObject>> getMissingRootParents();
    public CvUpdater getCvUpdater();

    public void clear();

}
