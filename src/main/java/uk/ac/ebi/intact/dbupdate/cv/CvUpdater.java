package uk.ac.ebi.intact.dbupdate.cv;

import uk.ac.ebi.intact.bridges.ontology_manager.TermAnnotation;
import uk.ac.ebi.intact.bridges.ontology_manager.TermDbXref;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyAccess;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyTermI;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.*;
import uk.ac.ebi.intact.dbupdate.cv.errors.CvUpdateError;
import uk.ac.ebi.intact.dbupdate.cv.errors.UpdateError;
import uk.ac.ebi.intact.dbupdate.cv.events.UpdateErrorEvent;
import uk.ac.ebi.intact.dbupdate.cv.events.UpdatedEvent;
import uk.ac.ebi.intact.dbupdate.cv.utils.CvUpdateUtils;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.XrefUtils;

import java.util.*;

/**
 * this class is for updating a cv
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>10/11/11</pre>
 */

public class CvUpdater {

    private Map<String, List<CvDagObject>> missingParents;
    private Set<String> processedTerms;

    public final static String ALIAS_TYPE="database alias";

    public CvUpdater() {
        missingParents = new HashMap<String, List<CvDagObject>>();
        processedTerms = new HashSet<String>();
    }

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

        doUpdate(updateContext, factory, term, updateEvt);
    }

    public void doUpdate(CvUpdateContext updateContext, DaoFactory factory, CvDagObject term, UpdatedEvent updateEvt) {
        if (updateEvt.isTermUpdated()){
            // update/persist/delete xrefs
            XrefDao<CvObjectXref> xrefDao = factory.getXrefDao(CvObjectXref.class);

            for (CvObjectXref updated : updateEvt.getUpdatedXrefs()){
                xrefDao.update(updated);
            }
            for (CvObjectXref created : updateEvt.getCreatedXrefs()){
                xrefDao.persist(created);
            }
            for (CvObjectXref deleted : updateEvt.getDeletedXrefs()){
                xrefDao.delete(deleted);
            }

            // update/persist/delete aliases
            AliasDao<CvObjectAlias> aliasDao = factory.getAliasDao(CvObjectAlias.class);

            for (CvObjectAlias updated : updateEvt.getUpdatedAliases()){
                aliasDao.update(updated);
            }
            for (CvObjectAlias created : updateEvt.getCreatedAliases()){
                aliasDao.persist(created);
            }
            for (CvObjectAlias deleted : updateEvt.getDeletedAliases()){
                aliasDao.delete(deleted);
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
            factory.getCvObjectDao(CvDagObject.class).update(term);

            // fire event
            CvUpdateManager manager = updateContext.getManager();
            manager.fireOnUpdateCase(updateEvt);
        }
    }

    public void updateParents(CvUpdateContext updateContext, UpdatedEvent updateEvt){

        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        IntactOntologyAccess ontologyAccess = updateContext.getOntologyAccess();
        IntactOntologyTermI ontologyTerm = updateContext.getOntologyTerm();
        CvDagObject term = updateContext.getCvTerm();

        // parents of the term in the ontology
        Set<IntactOntologyTermI> parents = ontologyAccess.getDirectParents(ontologyTerm);
        // missing parents to create
        Set<IntactOntologyTermI> missingParents = new HashSet<IntactOntologyTermI>(parents);
        // parents to delete
        Collection<CvDagObject> cvParents = new ArrayList<CvDagObject>(term.getParents());

        // when updating parents, we only update parents from current ontology so we can filter parents which need to be excluded
        for (CvDagObject parent : cvParents){

            CvObjectXref identity = XrefUtils.getIdentityXref(parent, ontologyAccess.getDatabaseIdentifier());
            // this parent cannot be updated because is not from the same ontology
            if (identity == null && !parent.getIdentifier().startsWith(ontologyAccess.getOntologyID()+ ":")){
                continue;
            }
            // check if parent exist
            else {
                boolean hasFound = false;

                // try to find a match in the ontology
                for (IntactOntologyTermI ontologyTermI : parents){
                    if (parent.getIdentifier().equalsIgnoreCase(ontologyTermI.getTermAccession())){
                        missingParents.remove(ontologyTermI);
                        hasFound = true;
                    }
                    else if (identity != null && ontologyTermI.getTermAccession().equalsIgnoreCase(identity.getPrimaryId())){
                        missingParents.remove(ontologyTermI);
                        hasFound = true;
                    }
                }

                // parent which should be removed
                if (!hasFound){
                    // get term in the ontology
                    IntactOntologyTermI parentInOntology = ontologyAccess.getTermForAccession(parent.getIdentifier());

                    if (parentInOntology == null && identity != null){
                        parentInOntology = ontologyAccess.getTermForAccession(identity.getPrimaryId());
                    }

                    // if term does not exist, we fire an error
                    if (parentInOntology == null){
                        CvUpdateManager updateManager = updateContext.getManager();

                        CvUpdateError error = updateManager.getErrorFactory().createCvUpdateError(UpdateError.non_existing_term, "The term " + updateContext.getIdentifier() + " has a parent " + parent.getIdentifier() + " which does not exist in the ontology.", parent.getIdentifier(), parent.getAc(), parent.getShortLabel());
                        UpdateErrorEvent errorEvt = new UpdateErrorEvent(this, error);

                        updateManager.fireOnUpdateError(errorEvt);
                    }
                    // if term is not obsolete, we remove it from the parents
                    else if (!ontologyAccess.isObsolete(parentInOntology)){
                        term.removeParent(parent);

                        updateEvt.getDeletedParents().add(parent);
                    }
                }
            }
        }

        // update parents for current ontology
        for (IntactOntologyTermI parent : missingParents){
            CvDagObject parentFromDb = factory.getCvObjectDao(CvDagObject.class).getByIdentifier(parent.getTermAccession());

            if (parentFromDb == null){
                if (this.missingParents.containsKey(parent.getTermAccession())){
                    this.missingParents.get(parent.getTermAccession()).add(term);
                }
                else {
                    List<CvDagObject> objects = new ArrayList<CvDagObject>();
                    objects.add(term);

                    this.missingParents.put(parent.getTermAccession(), objects);
                }
            }
            else {
                term.addParent(parentFromDb);
                updateEvt.getCreatedParents().add(parentFromDb);
            }
        }
    }

    public void updateAnnotations(CvUpdateContext updateContext, UpdatedEvent updateEvt){
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        IntactOntologyTermI ontologyTerm = updateContext.getOntologyTerm();
        CvDagObject term = updateContext.getCvTerm();
        boolean isObsolete = updateContext.isTermObsolete();

        // The annotations to delete
        Collection<Annotation> cvAnnotations = new ArrayList<Annotation>(term.getAnnotations());
        // The annotations to create
        Collection<TermAnnotation> ontologyAnnotations = new ArrayList<TermAnnotation>(ontologyTerm.getAnnotations());
        // the comments to create
        Set<String> comments = new HashSet<String>(ontologyTerm.getComments());

        // boolean value to know if url is in the annotations
        boolean hasFoundURL = false;
        // boolean value to know if definition is in the annotations
        boolean hasFoundDefinition = false;
        // boolean value to know if obsolete is in the annotations
        boolean hasFondObsolete = false;
        // boolean value to know if hidden is in the annotations
        boolean isHidden = false;

        // the CvTopic for comment
        CvTopic comment = null;

        // for each existing annotation, check if it exists in the ontology. If the annotation and its topic are not in the ontology, we don't touch it
        // because can be an annotation manually added by a curator (used in class and hidden topics for instance)
        for (Annotation a : term.getAnnotations()){
            String cvTopicId = a.getCvTopic() != null ? a.getCvTopic().getIdentifier() : null;
            String cvTopicLabel = a.getCvTopic() != null ? a.getCvTopic().getShortLabel() : null;

            // we have a definition. Only one is allowed
            if (CvTopic.DEFINITION.equalsIgnoreCase(cvTopicLabel)){
                if (!hasFoundDefinition){
                    hasFoundDefinition = true;
                    cvAnnotations.remove(a);

                    // we update existing definition
                    if (!ontologyTerm.getDefinition().equalsIgnoreCase(a.getAnnotationText())){
                        a.setAnnotationText(ontologyTerm.getDefinition());

                        updateEvt.getUpdatedAnnotations().add(a);
                    }
                }
            }
            // we have an obsolete annotation. Only one is allowed
            else if (CvTopic.OBSOLETE_MI_REF.equalsIgnoreCase(cvTopicId)){
                if (!hasFondObsolete && isObsolete){
                    hasFondObsolete = true;
                    cvAnnotations.remove(a);

                    if (ontologyTerm.getObsoleteMessage() ==  null && a.getAnnotationText() != null){
                        a.setAnnotationText(null);

                        updateEvt.getUpdatedAnnotations().add(a);
                    }
                    else if (ontologyTerm.getObsoleteMessage() !=  null && !ontologyTerm.getObsoleteMessage().equalsIgnoreCase(a.getAnnotationText())){
                        a.setAnnotationText(ontologyTerm.getDefinition());

                        updateEvt.getUpdatedAnnotations().add(a);
                    }
                }
            }
            // the term is hidden. We do nothing for now
            else if (CvTopic.HIDDEN.equalsIgnoreCase(cvTopicLabel)){
                isHidden = true;
            }
            // we have a comment. Checks that it exists in the ontology
            else if (CvTopic.COMMENT_MI_REF.equalsIgnoreCase(cvTopicId)){
                comment = a.getCvTopic();

                // comment exist
                if (comments.contains(a.getAnnotationText())){
                    cvAnnotations.remove(a);
                    comments.remove(a.getAnnotationText());
                }
                // comment does not exist but can be updated
                else if (!comments.contains(a.getAnnotationText()) && comments.size() > 0){
                    a.setAnnotationText(comments.iterator().next());
                    updateEvt.getUpdatedAnnotations().add(a);

                    cvAnnotations.remove(a);
                    comments.remove(a.getAnnotationText());
                }
            }
            // we have a url. Only one url is allowed
            else if (CvTopic.URL_MI_REF.equalsIgnoreCase(cvTopicId)){
                if (!hasFoundURL){
                    hasFoundURL = true;
                    cvAnnotations.remove(a);

                    if (!ontologyTerm.getDefinition().equalsIgnoreCase(a.getAnnotationText())){
                        a.setAnnotationText(ontologyTerm.getDefinition());

                        updateEvt.getUpdatedAnnotations().add(a);
                    }
                }
            }
            // checks that the annotations is in the ontology annotations
            else {
                boolean hasFoundAnnotation = false;
                boolean hasFoundTopic = false;
                TermAnnotation ontAnnot = null;

                for (TermAnnotation termAnnotation : ontologyAnnotations){
                    String topicId = termAnnotation.getTopicId();

                    if (topicId.equalsIgnoreCase(cvTopicId)){
                        ontAnnot = termAnnotation;
                        hasFoundTopic = true;

                        if (termAnnotation.getDescription().equalsIgnoreCase(a.getAnnotationText())){
                            hasFoundAnnotation = true;
                            break;
                        }
                    }
                }

                // the topic does not exist in the term , we don't touch it
                if (!hasFoundTopic){
                    cvAnnotations.remove(a);
                }
                // exact annotation already exists, no need to update it
                else if (hasFoundTopic && hasFoundAnnotation){
                    cvAnnotations.remove(a);
                    ontologyAnnotations.remove(ontAnnot);
                }
                // the topic exist but the text is not exactly the same and needs to be updated
                else if (hasFoundTopic && !hasFoundAnnotation && ontAnnot != null){

                    a.setAnnotationText(ontAnnot.getDescription());
                    updateEvt.getUpdatedAnnotations().add(a);

                    cvAnnotations.remove(a);
                    ontologyAnnotations.remove(ontAnnot);
                }
            }
        }

        // remove all annotations having a topic present in new term but which needs to be deleted because out of date
        for (Annotation a : cvAnnotations){
            term.removeAnnotation(a);

            updateEvt.getDeletedAnnotations().add(a);
        }

        // create missing annotations
        for (TermAnnotation termAnnotation : ontologyAnnotations){
            CvTopic topicFromDb = factory.getCvObjectDao(CvTopic.class).getByIdentifier(termAnnotation.getTopicId());

            if (topicFromDb == null){
                topicFromDb = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, termAnnotation.getTopicId(), termAnnotation.getTopic());
                IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(topicFromDb);
            }

            Annotation newAnnotation = new Annotation(topicFromDb, termAnnotation.getDescription());
            term.addAnnotation(newAnnotation);

            updateEvt.getCreatedAnnotations().add(newAnnotation);
        }

        // create missing definition
        if (!hasFoundDefinition && ontologyTerm.getDefinition() != null){
            CvTopic topicFromDb = factory.getCvObjectDao(CvTopic.class).getByShortLabel(CvTopic.DEFINITION);

            if (topicFromDb == null){
                topicFromDb = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, null, CvTopic.DEFINITION);
                IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(topicFromDb);
            }

            Annotation newAnnotation = new Annotation(topicFromDb, ontologyTerm.getDefinition());
            term.addAnnotation(newAnnotation);

            updateEvt.getCreatedAnnotations().add(newAnnotation);
        }

        // create missing url
        if (!hasFoundURL && ontologyTerm.getURL() != null){
            CvTopic topicFromDb = factory.getCvObjectDao(CvTopic.class).getByIdentifier(CvTopic.URL_MI_REF);

            if (topicFromDb == null){
                topicFromDb = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, CvTopic.URL_MI_REF, CvTopic.URL);
                IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(topicFromDb);
            }

            Annotation newAnnotation = new Annotation(topicFromDb, ontologyTerm.getURL());
            term.addAnnotation(newAnnotation);

            updateEvt.getCreatedAnnotations().add(newAnnotation);
        }

        // create missing obsolete
        if (!hasFondObsolete && isObsolete){
            CvTopic topicFromDb = factory.getCvObjectDao(CvTopic.class).getByIdentifier(CvTopic.OBSOLETE_MI_REF);

            if (topicFromDb == null){
                topicFromDb = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, CvTopic.OBSOLETE_MI_REF, CvTopic.OBSOLETE);
                IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(topicFromDb);
            }

            Annotation newAnnotation = new Annotation(topicFromDb, ontologyTerm.getObsoleteMessage());
            term.addAnnotation(newAnnotation);

            updateEvt.getCreatedAnnotations().add(newAnnotation);
        }

        // hide term if obsolete
        if (isObsolete && !isHidden){
            updateEvt.getCreatedAnnotations().add(CvUpdateUtils.hideTerm(term, "obsolete term"));
        }

        // create missing comments
        if (!comments.isEmpty()){
            if (comment == null){
                comment = factory.getCvObjectDao(CvTopic.class).getByIdentifier(CvTopic.COMMENT_MI_REF);

                if (comment == null){
                    comment = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, CvTopic.COMMENT_MI_REF, CvTopic.COMMENT);
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(comment);
                }
            }

            for (String com : comments){
                Annotation newAnnotation = new Annotation(comment, com);
                term.addAnnotation(newAnnotation);

                updateEvt.getCreatedAnnotations().add(newAnnotation);
            }
        }
    }

    public void updateXrefs(CvUpdateContext updateContext, UpdatedEvent updateEvt){
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        IntactOntologyAccess ontologyAccess = updateContext.getOntologyAccess();
        IntactOntologyTermI ontologyTerm = updateContext.getOntologyTerm();
        CvDagObject term = updateContext.getCvTerm();

        // cluster ontology term xrefs
        Map<String, Collection<TermDbXref>> ontologyCluster = clusterOntologyReferences(ontologyTerm);
        // cluster Cvobject xrefs
        Map<String, Collection<CvObjectXref>> cvCluster = clusterCvReferences(term, ontologyAccess.getDatabaseIdentifier());

        // for each existing xref, compare if exist in ontology otherwise delete
        for (Map.Entry<String, Collection<CvObjectXref>> cvRef : cvCluster.entrySet()){

            // the xref is in the ontology cluster
            if (ontologyCluster.containsKey(cvRef.getKey())){
                // get the cvDatabase
                CvDatabase cvDatabase = cvRef.getValue().iterator().next().getCvDatabase();
                // get the xrefs from ontology
                Collection<TermDbXref> ontologyReferences = ontologyCluster.get(cvRef.getKey());

                // for each xref in the database, find the one in the ontology matching the existing xref if it exists
                for (CvObjectXref ref : cvRef.getValue()){
                    // the matching term in the ontology
                    TermDbXref match = null;

                    for (TermDbXref ontRef : ontologyReferences){
                        // the database accession is matching
                        if (ontRef.getAccession().equalsIgnoreCase(ref.getPrimaryId())){

                            match = ontRef;
                            String qualifierId = ref.getCvXrefQualifier() != null ? ref.getCvXrefQualifier().getIdentifier() : null;

                            // the qualifier is not matching, meaning that the xref needs to be updated
                            if (!ontRef.getQualifierId().equalsIgnoreCase(qualifierId)){
                                CvXrefQualifier qualifierFromDb = factory.getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(ontRef.getQualifierId());

                                if (qualifierFromDb == null){
                                    qualifierFromDb = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvXrefQualifier.class, ontRef.getQualifierId(), ontRef.getQualifier());
                                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(qualifierFromDb);
                                }
                                ref.setCvXrefQualifier(qualifierFromDb);

                                updateEvt.getUpdatedXrefs().add(ref);
                            }
                            break;
                        }
                    }

                    // we found the xref in the ontology so we can remove it from the xref to create
                    if (match != null){
                        ontologyReferences.remove(match);
                    }
                    // we didn't found the xref in the ontology so it need to be removed
                    else {
                        term.removeXref(ref);
                        updateEvt.getDeletedXrefs().add(ref);
                    }
                }

                // create missing xrefs for this db
                for (TermDbXref termRef : ontologyReferences){
                    CvXrefQualifier qualifierFromDb = factory.getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(termRef.getQualifierId());

                    if (qualifierFromDb == null){
                        qualifierFromDb = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvXrefQualifier.class, termRef.getQualifierId(), termRef.getQualifier());
                        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(qualifierFromDb);
                    }

                    CvObjectXref newRef = new CvObjectXref(term.getOwner(), cvDatabase, termRef.getAccession(), null, null, qualifierFromDb);
                    term.addXref(newRef);

                    updateEvt.getCreatedXrefs().add(newRef);
                }

                // remove the xrefs for this database id from the ontology cluster
                ontologyCluster.remove(cvRef.getKey());
            }
            // the xref does not exist in the ontology
            else {
                Collection<CvObjectXref> refsToDelete = cvRef.getValue();

                for (CvObjectXref r : refsToDelete){
                    term.removeXref(r);

                    updateEvt.getDeletedXrefs().add(r);
                }
            }
        }

        // create missing db xrefs
        for (Map.Entry<String, Collection<TermDbXref>> entry : ontologyCluster.entrySet()){
            CvDatabase cvDatabase = factory.getCvObjectDao(CvDatabase.class).getByPsiMiRef(entry.getKey());

            // create missing xrefs for this db
            for (TermDbXref termRef : entry.getValue()){
                CvXrefQualifier qualifierFromDb = factory.getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(termRef.getQualifierId());

                if (qualifierFromDb == null){
                    qualifierFromDb = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvXrefQualifier.class, termRef.getQualifierId(), termRef.getQualifier());
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(qualifierFromDb);
                }

                CvObjectXref newRef = new CvObjectXref(term.getOwner(), cvDatabase, termRef.getAccession(), null, null, qualifierFromDb);
                term.addXref(newRef);

                updateEvt.getCreatedXrefs().add(newRef);
            }
        }
    }

    public void updateAliases(CvUpdateContext updateContext, UpdatedEvent updateEvt){
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        IntactOntologyTermI ontologyTerm = updateContext.getOntologyTerm();
        CvDagObject term = updateContext.getCvTerm();

        CvAliasType aliasType = factory.getCvObjectDao(CvAliasType.class).getByShortLabel(ALIAS_TYPE);

        if (aliasType == null){
            aliasType = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvAliasType.class, null, ALIAS_TYPE);
            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(aliasType);
        }

        // the aliases in the ontology to create
        Set<String> aliasesToCreate = new HashSet<String>(ontologyTerm.getAliases());

        // for each existing alias in db, find if it exists in the ontology, delete it otherwise
        for (CvObjectAlias alias : term.getAliases()){
            // only one alias type is allowed
            // the alias exists, we can remove it from the aliases to create and to delete
            if (aliasesToCreate.contains(alias.getName())) {
                // we may need to update the alias type
                if (alias.getCvAliasType() == null || (alias.getCvAliasType() != null && !alias.getCvAliasType().getShortLabel().equalsIgnoreCase(ALIAS_TYPE))){
                    alias.setCvAliasType(aliasType);

                    updateEvt.getUpdatedAliases().add(alias);
                }

                aliasesToCreate.remove(alias.getName());
            }
            // the alias does not exist, we need to delete it
            else {
                term.removeAlias(alias);

                updateEvt.getDeletedAliases().add(alias);
            }
        }

        // Create the missing aliases
        for (String aliasToCreate : aliasesToCreate){
            CvObjectAlias newAlias = new CvObjectAlias(term.getOwner(), term, aliasType, aliasToCreate);
            term.addAlias(newAlias);

            updateEvt.getCreatedAliases().add(newAlias);
        }
    }

    private Map<String, Collection<TermDbXref>> clusterOntologyReferences(IntactOntologyTermI ontologyTerm){

        if (ontologyTerm.getDbXrefs().isEmpty()){
            return Collections.EMPTY_MAP;
        }

        Map<String, Collection<TermDbXref>> cluster = new HashMap<String, Collection<TermDbXref>>();

        for (TermDbXref ref : ontologyTerm.getDbXrefs()){
            String databaseId = ref.getDatabaseId() != null ? ref.getDatabaseId() : "null";
            if (cluster.containsKey(databaseId)){
                cluster.get(databaseId).add(ref);
            }
            else {
                Collection<TermDbXref> refs = new ArrayList<TermDbXref>();
                refs.add(ref);
                cluster.put(databaseId, refs);
            }
        }

        return cluster;
    }

    private Map<String, Collection<CvObjectXref>> clusterCvReferences(CvObject term, String database){

        if (term.getXrefs().isEmpty()){
            return Collections.EMPTY_MAP;
        }

        Map<String, Collection<CvObjectXref>> cluster = new HashMap<String, Collection<CvObjectXref>>();

        for (CvObjectXref ref : term.getXrefs()){
            if (cluster.containsKey(ref.getCvDatabase().getIdentifier())){
                cluster.get(ref.getCvDatabase().getIdentifier()).add(ref);
            }
            else if (ref.getCvDatabase() != null && !ref.getCvDatabase().getIdentifier().equals(database)){
                Collection<CvObjectXref> refs = new ArrayList<CvObjectXref>();
                refs.add(ref);
                cluster.put(ref.getCvDatabase().getIdentifier(), refs);
            }
            else {
                String db = ref.getCvDatabase() != null ? ref.getCvDatabase().getIdentifier() : "null";
                Collection<CvObjectXref> refs = new ArrayList<CvObjectXref>();
                refs.add(ref);
                cluster.put(db, refs);
            }
        }

        return cluster;
    }

    public Map<String, List<CvDagObject>> getMissingParents() {
        return missingParents;
    }

    public Set<String> getProcessedTerms() {
        return processedTerms;
    }

    public void clear(){
        this.missingParents.clear();
        this.processedTerms.clear();
    }
}
