package uk.ac.ebi.intact.dbupdate.cv.importer;

import psidev.psi.mi.jami.bridges.ontologymanager.MIOntologyAccess;
import psidev.psi.mi.jami.bridges.ontologymanager.MIOntologyTermI;
import uk.ac.ebi.intact.dbupdate.cv.CvUpdateContext;
import uk.ac.ebi.intact.dbupdate.cv.updater.CvUpdateException;
import uk.ac.ebi.intact.dbupdate.cv.updater.CvUpdater;
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
    public Set<Class<? extends CvDagObject>> findCvClassFor(MIOntologyTermI ontologyTerm, MIOntologyAccess ontologyAccess);
    public CvDagObject createCvObjectFrom(MIOntologyTermI ontologyTerm, MIOntologyAccess ontologyAccess, Class<? extends CvDagObject> termClass, boolean hideParents, CvUpdateContext updateContext) throws IllegalAccessException, InstantiationException, CvUpdateException;
    public Set<MIOntologyTermI> collectDeepestChildren(MIOntologyAccess ontologyAccess, MIOntologyTermI parent);
    public Map<String, Class<? extends CvDagObject>> getClassMap();
    public Map<String, Set<CvDagObject>> getMissingRootParents();
    public CvUpdater getCvUpdater();
    public boolean isFromAnotherClassCategory(MIOntologyTermI term, MIOntologyAccess access, Class<? extends CvDagObject> termClass);

    public void clear();

}
