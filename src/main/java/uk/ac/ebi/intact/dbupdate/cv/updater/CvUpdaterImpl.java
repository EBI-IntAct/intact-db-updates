package uk.ac.ebi.intact.dbupdate.cv.updater;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyAccess;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyTermI;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.*;
import uk.ac.ebi.intact.dbupdate.cv.CvUpdateContext;
import uk.ac.ebi.intact.dbupdate.cv.CvUpdateManager;
import uk.ac.ebi.intact.dbupdate.cv.events.UpdatedEvent;
import uk.ac.ebi.intact.dbupdate.cv.utils.CvUpdateUtils;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * this class is for updating a cv
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>10/11/11</pre>
 */

public class CvUpdaterImpl implements CvUpdater{

    private CvParentUpdater cvParentUpdater;
    private CvAliasUpdater cvAliasUpdater;
    private CvXrefUpdater cvXrefUpdater;
    private CvAnnotationUpdater cvAnnotationUpdater;
    private Set<String> processedTerms;

    public CvUpdaterImpl() {
        processedTerms = new HashSet<String>();
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void updateTerm(CvUpdateContext updateContext){
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
        processedTerms.add(identifier);

        boolean hasUpdatedIdentifier = false;
        boolean hasUpdatedShortLabel = false;
        boolean hasUpdatedFullName = false;

        // update shortLabel
        if (!ontologyTerm.getShortLabel().equalsIgnoreCase(term.getShortLabel())){
            hasUpdatedShortLabel = true;
            term.setShortLabel(ontologyTerm.getShortLabel());
        }

        // update fullName
        if (!ontologyTerm.getFullName().equalsIgnoreCase(term.getFullName())){
            hasUpdatedFullName = true;
            term.setFullName(ontologyTerm.getFullName());
        }

        // update identifier if necessary
        if (!identifier.equalsIgnoreCase(term.getIdentifier())){
            hasUpdatedIdentifier = true;
            // update identifier
            term.setIdentifier(identifier);
        }

        // create update evt
        String updatedShortLabel = hasUpdatedShortLabel ? ontologyTerm.getShortLabel() : null;
        String updatedFullName = hasUpdatedFullName ? ontologyTerm.getFullName() : null;
        UpdatedEvent updateEvt = new UpdatedEvent(this, identifier, term.getAc(), updatedShortLabel, updatedFullName, hasUpdatedIdentifier);

        // update identity xref if necessary
        CvObjectXref identityXref = updateContext.getIdentityXref();
        if (identityXref == null) {
            identityXref = CvUpdateUtils.createIdentityXref(term, database, identifier);
            updateEvt.getCreatedXrefs().add(identityXref);
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

            updateEvt.getUpdatedXrefs().add(identityXref);
        }

        // update xrefs
        updateXrefs(updateContext, updateEvt);

        // update aliases
        updateAliases(updateContext, updateEvt);

        // update annotations
        updateAnnotations(updateContext, updateEvt);

        // update parents only if not obsolete
        if (!isObsolete){
            updateParents(updateContext, updateEvt);
        }

        doUpdate(updateContext, updateEvt);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    private void doUpdate(CvUpdateContext updateContext, UpdatedEvent updateEvt) {
        if (updateEvt.isTermUpdated()){

            DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

            // update/persist xrefs. Don't call delete because of annotation orphanRemoval = true which is detaching the xref
            XrefDao<CvObjectXref> xrefDao = factory.getXrefDao(CvObjectXref.class);

            for (CvObjectXref updated : updateEvt.getUpdatedXrefs()){
                xrefDao.update(updated);
            }
            for (CvObjectXref created : updateEvt.getCreatedXrefs()){
                xrefDao.persist(created);
            }

            // update/persist aliases. Don't call delete because of annotation orphanRemoval = true which is detaching the aliases
            AliasDao<CvObjectAlias> aliasDao = factory.getAliasDao(CvObjectAlias.class);

            for (CvObjectAlias updated : updateEvt.getUpdatedAliases()){
                aliasDao.update(updated);
            }
            for (CvObjectAlias created : updateEvt.getCreatedAliases()){
                aliasDao.persist(created);
            }

            // update/persist/delete annotations
            AnnotationDao annotationDao = factory.getAnnotationDao();

            for (Annotation updated : updateEvt.getUpdatedAnnotations()){
                annotationDao.update(updated);
            }
            for (Annotation created : updateEvt.getCreatedAnnotations()){
                annotationDao.persist(created);
            }
            for (Annotation deleted : updateEvt.getDeletedAnnotations()){
                annotationDao.delete(deleted);
            }

            // update parents
            CvObjectDao<CvDagObject> cvDao = factory.getCvObjectDao(CvDagObject.class);

            for (CvDagObject created : updateEvt.getCreatedParents()){
                cvDao.update(created);
            }
            for (CvDagObject deleted : updateEvt.getDeletedParents()){
                cvDao.update(deleted);
            }

            // update term
            factory.getCvObjectDao(CvDagObject.class).update(updateContext.getCvTerm());

            // fire event
            CvUpdateManager manager = updateContext.getManager();
            manager.fireOnUpdateCase(updateEvt);
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void updateParents(CvUpdateContext updateContext, UpdatedEvent updateEvt){
        cvParentUpdater.updateParents(updateContext, updateEvt);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void updateAnnotations(CvUpdateContext updateContext, UpdatedEvent updateEvt){
        cvAnnotationUpdater.updateAnnotations(updateContext, updateEvt);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void updateXrefs(CvUpdateContext updateContext, UpdatedEvent updateEvt){
        cvXrefUpdater.updateXrefs(updateContext, updateEvt);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void updateAliases(CvUpdateContext updateContext, UpdatedEvent updateEvt){
        cvAliasUpdater.updateAliases(updateContext, updateEvt);
    }

    public Map<String, Set<CvDagObject>> getMissingParents() {
        return cvParentUpdater.getMissingParents();
    }

    public Set<String> getProcessedTerms() {
        return processedTerms;
    }

    public CvParentUpdater getCvParentUpdater() {
        if (this.cvParentUpdater == null){
            cvParentUpdater = new CvParentUpdaterImpl();
        }
        return cvParentUpdater;
    }

    public void setCvParentUpdater(CvParentUpdater cvParentUpdater) {
        this.cvParentUpdater = cvParentUpdater;
    }

    public CvAliasUpdater getCvAliasUpdater() {
        if (this.cvAliasUpdater == null){
            cvAliasUpdater = new CvAliasUpdaterImpl();
        }
        return cvAliasUpdater;
    }

    public void setCvAliasUpdater(CvAliasUpdater cvAliasUpdater) {
        this.cvAliasUpdater = cvAliasUpdater;
    }

    public CvXrefUpdater getCvXrefUpdater() {
        if (cvXrefUpdater == null){
            cvXrefUpdater = new CvXrefUpdaterImpl();
        }
        return cvXrefUpdater;
    }

    public void setCvXrefUpdater(CvXrefUpdater cvXrefUpdater) {
        this.cvXrefUpdater = cvXrefUpdater;
    }

    public CvAnnotationUpdater getCvAnnotationUpdater() {
        if (cvAnnotationUpdater == null){
            cvAnnotationUpdater = new CvAnnotationUpdaterImpl();
        }
        return cvAnnotationUpdater;
    }

    public void setCvAnnotationUpdater(CvAnnotationUpdater cvAnnotationUpdater) {
        this.cvAnnotationUpdater = cvAnnotationUpdater;
    }

    public void clear(){
        this.cvParentUpdater.clear();
        this.cvAliasUpdater.clear();
        this.cvXrefUpdater.clear();
        this.processedTerms.clear();
    }
}
