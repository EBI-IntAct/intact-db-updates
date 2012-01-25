package uk.ac.ebi.intact.dbupdate.cv.updater;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyAccess;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyTermI;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.cv.CvUpdateContext;
import uk.ac.ebi.intact.dbupdate.cv.utils.CvUpdateUtils;
import uk.ac.ebi.intact.model.CvDagObject;
import uk.ac.ebi.intact.model.CvDatabase;
import uk.ac.ebi.intact.model.CvObjectXref;
import uk.ac.ebi.intact.model.CvXrefQualifier;
import uk.ac.ebi.intact.model.util.CvObjectUtils;

/**
 * This class is based on the cv updater but is aimed at initializing a cv only
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>20/01/12</pre>
 */

public class CvInitializer extends CvUpdaterImpl{

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void updateTerm(CvUpdateContext updateContext) throws CvUpdateException {
        checkUpdateContext(updateContext);

        // use dao factory
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        IntactOntologyAccess ontologyAccess = updateContext.getOntologyAccess();
        IntactOntologyTermI ontologyTerm = updateContext.getOntologyTerm();
        CvDagObject term = updateContext.getCvTerm();
        String identifier = updateContext.getIdentifier();
        boolean isObsolete = updateContext.isTermObsolete();

        // extract database of ontology
        String database = updateContext.getOntologyAccess().getDatabaseIdentifier();

        // add term to the list of updated terms
        if (processedTerms.add(identifier)){
            // update shortLabel
            term.setShortLabel(ontologyTerm.getShortLabel());

            // update fullName
            term.setFullName(ontologyTerm.getFullName());

            // update identifier if necessary
            term.setIdentifier(identifier);

            // update identity xref if necessary
            CvObjectXref identityXref = updateContext.getIdentityXref();
            if (identityXref == null) {
                identityXref = CvUpdateUtils.createIdentityXref(term, database, identifier);
                updateContext.setIdentityXref(identityXref);
            }
            else if (!identifier.equalsIgnoreCase(identityXref.getPrimaryId()) || !database.equalsIgnoreCase(identityXref.getCvDatabase().getIdentifier())
                    || !CvXrefQualifier.IDENTITY_MI_REF.equalsIgnoreCase(identityXref.getCvXrefQualifier().getIdentifier())){
                identityXref.setPrimaryId(identifier);

                if (identityXref.getCvXrefQualifier()  == null || !CvXrefQualifier.IDENTITY_MI_REF.equalsIgnoreCase(identityXref.getCvXrefQualifier().getIdentifier())){
                    CvXrefQualifier identity = factory.getCvObjectDao(CvXrefQualifier.class).getByIdentifier(CvXrefQualifier.IDENTITY_MI_REF);

                    if (identity == null){
                        identity = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF, CvXrefQualifier.IDENTITY);
                        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(identity);
                    }

                    identityXref.setCvXrefQualifier(identity);
                }

                if (identityXref.getCvDatabase()  == null || !database.equalsIgnoreCase(identityXref.getCvDatabase().getIdentifier())){
                    CvDatabase cvDatabase = factory.getCvObjectDao(CvDatabase.class).getByIdentifier(database);

                    if (cvDatabase == null){
                        cvDatabase = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvDatabase.class, database, ontologyAccess.getOntologyID().toLowerCase());
                        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(cvDatabase);
                    }

                    identityXref.setCvDatabase(cvDatabase);
                }
            }

            // update xrefs
            updateXrefs(updateContext, null);

            // update aliases
            updateAliases(updateContext, null);

            // update annotations
            updateAnnotations(updateContext, null);

            // in case of cvtopics, we may need to create new used in class annotations
            if (usedInClassAnnotationUpdater.canUpdate(term)){
                updateUsedInClass(updateContext, null);
            }
        }
    }
}
