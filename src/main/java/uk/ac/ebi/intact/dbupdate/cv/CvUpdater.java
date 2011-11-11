package uk.ac.ebi.intact.dbupdate.cv;

import uk.ac.ebi.intact.bridges.ontology_manager.TermAnnotation;
import uk.ac.ebi.intact.bridges.ontology_manager.TermDbXref;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyAccess;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyTermI;
import uk.ac.ebi.intact.core.persistence.dao.AliasDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.persistence.dao.XrefDao;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.XrefUtils;

import java.io.IOException;
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

    private final static String ALIAS_TYPE="database alias";

    public CvUpdater() throws IOException {
        missingParents = new HashMap<String, List<CvDagObject>>();
        processedTerms = new HashSet<String>();
    }

    public void updateTerm(CvDagObject term, IntactOntologyTermI ontologyTerm, IntactOntologyAccess ontologyAccess, DaoFactory factory){
        String database = ontologyAccess.getDatabaseIdentifier();

        CvObjectXref cvXref = XrefUtils.getIdentityXref(term, database);
        String identifier;

        if (cvXref != null){
            identifier = cvXref.getPrimaryId();

            // update identifier
            term.setIdentifier(identifier);
        }
        else {
            identifier = term.getIdentifier();

            CvXrefQualifier identity = factory.getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.IDENTITY_MI_REF);
            CvDatabase db = factory.getCvObjectDao(CvDatabase.class).getByPsiMiRef(database);

            // create identity xref
            cvXref = XrefUtils.createIdentityXref(term, identifier, identity, db);
            factory.getXrefDao(CvObjectXref.class).persist(cvXref);

            term.addXref(cvXref);
        }

        processedTerms.add(identifier);

        // update shortLabel
        term.setShortLabel(ontologyTerm.getShortLabel());

        // update fullName
        term.setFullName(ontologyTerm.getFullName());

        // update xrefs
        updateXrefs(term, ontologyTerm, factory);

        // update aliases
        updateAliases(term, ontologyTerm, factory);

        // update annotations
        updateAnnotations(term, ontologyTerm, factory);

        // update parents
        if (!ontologyAccess.isObsolete(ontologyTerm)){
            updateParents(term, ontologyTerm, ontologyAccess, factory);
        }

        factory.getCvObjectDao(CvDagObject.class).update(term);
    }

    public void updateParents(CvDagObject term, IntactOntologyTermI ontologyTerm,
                              IntactOntologyAccess ontologyAccess, DaoFactory factory){

        Set<IntactOntologyTermI> parents = ontologyAccess.getDirectParents(ontologyTerm);
        Set<IntactOntologyTermI> missingParents = new HashSet<IntactOntologyTermI>(parents);

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

                for (IntactOntologyTermI ontologyTermI : parents){
                    if (parent.getIdentifier().equalsIgnoreCase(parent.getIdentifier())){
                        missingParents.remove(ontologyTermI);
                        hasFound = true;
                    }
                    else if (identity != null && parent.getIdentifier().equalsIgnoreCase(identity.getPrimaryId())){
                        missingParents.remove(ontologyTermI);
                        hasFound = true;
                    }
                }

                // parent which should be removed if not obsolete
                if (!hasFound){
                    IntactOntologyTermI parentInOntology = ontologyAccess.getTermForAccession(parent.getIdentifier());

                    if (parentInOntology == null && identity != null){
                        parentInOntology = ontologyAccess.getTermForAccession(identity.getPrimaryId());
                    }

                    if (parentInOntology == null){
                        term.removeParent(parent);

                        factory.getCvObjectDao(CvDagObject.class).update(parent);
                    }
                    else if (parentInOntology != null && !ontologyAccess.isObsolete(parentInOntology)){
                        term.removeParent(parent);

                        factory.getCvObjectDao(CvDagObject.class).update(parent);
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
                factory.getCvObjectDao(CvDagObject.class).update(parentFromDb);
            }
        }
    }

    public void updateAnnotations(CvDagObject term, IntactOntologyTermI ontologyTerm, DaoFactory factory){
        Collection<Annotation> cvannotations = new ArrayList<Annotation>(term.getAnnotations());
        Collection<TermAnnotation> ontologyAnnotations = new ArrayList<TermAnnotation>(ontologyTerm.getAnnotations());
        Set<String> comments = new HashSet<String>(ontologyTerm.getComments());

        boolean hasFoundURL = false;
        boolean hasFoundDefinition = false;
        CvTopic comment = null;

        for (Annotation a : term.getAnnotations()){
            String cvTopicId = a.getCvTopic() != null ? a.getCvTopic().getIdentifier() : null;
            String cvTopicLabel = a.getCvTopic() != null ? a.getCvTopic().getShortLabel() : null;

            if (CvTopic.DEFINITION.equalsIgnoreCase(cvTopicLabel)){
                if (!hasFoundDefinition){
                    hasFoundDefinition = true;
                    cvannotations.remove(a);

                    if (!ontologyTerm.getDefinition().equalsIgnoreCase(a.getAnnotationText())){
                        a.setAnnotationText(ontologyTerm.getDefinition());

                        factory.getAnnotationDao().update(a);
                    }
                }
            }
            else if (CvTopic.COMMENT_MI_REF.equalsIgnoreCase(cvTopicId)){
                comment = a.getCvTopic();

                // comment exist
                if (comments.contains(a.getAnnotationText())){
                    cvannotations.remove(a);
                    comments.remove(a.getAnnotationText());
                }
                // comment does not exist but can be updated
                else if (!comments.contains(a.getAnnotationText()) && comments.size() > 0){
                    a.setAnnotationText(comments.iterator().next());
                    factory.getAnnotationDao().update(a);

                    cvannotations.remove(a);
                    comments.remove(a.getAnnotationText());
                }
            }
            else if (CvTopic.URL_MI_REF.equalsIgnoreCase(cvTopicId)){
                if (!hasFoundURL){
                    hasFoundURL = true;
                    cvannotations.remove(a);

                    if (!ontologyTerm.getDefinition().equalsIgnoreCase(a.getAnnotationText())){
                        a.setAnnotationText(ontologyTerm.getDefinition());

                        factory.getAnnotationDao().update(a);
                    }
                }
            }
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

                // the topic does not exist in the term, we don't touch it
                if (!hasFoundTopic){
                    cvannotations.remove(a);
                }
                // exact annotation already exists, no need to update it
                else if (hasFoundTopic && hasFoundAnnotation){
                    cvannotations.remove(a);
                    ontologyAnnotations.remove(ontAnnot);
                }
                // the topic exist but the text is not exactly the same and needs to be updated
                else if (hasFoundTopic && !hasFoundAnnotation && ontAnnot != null){

                    a.setAnnotationText(ontAnnot.getDescription());
                    factory.getAnnotationDao().update(a);

                    cvannotations.remove(a);
                    ontologyAnnotations.remove(ontAnnot);
                }
            }
        }

        // remove all annotations having a topic present in new term but which needs to be deleted because out of date
        for (Annotation a : cvannotations){
            term.removeAnnotation(a);

            factory.getAnnotationDao().delete(a);
        }

        // create missing annotations
        for (TermAnnotation termAnnotation : ontologyAnnotations){
            CvTopic topicFromDb = factory.getCvObjectDao(CvTopic.class).getByIdentifier(termAnnotation.getTopicId());

            Annotation newAnnotation = new Annotation(topicFromDb, termAnnotation.getDescription());
            term.addAnnotation(newAnnotation);

            factory.getAnnotationDao().persist(newAnnotation);
        }

        // create missing definition
        if (!hasFoundDefinition && ontologyTerm.getDefinition() != null){
            CvTopic topicFromDb = factory.getCvObjectDao(CvTopic.class).getByShortLabel(CvTopic.DEFINITION);

            Annotation newAnnotation = new Annotation(topicFromDb, ontologyTerm.getDefinition());
            term.addAnnotation(newAnnotation);

            factory.getAnnotationDao().persist(newAnnotation);
        }

        // create missing url
        if (!hasFoundURL && ontologyTerm.getURL() != null){
            CvTopic topicFromDb = factory.getCvObjectDao(CvTopic.class).getByIdentifier(CvTopic.URL_MI_REF);

            Annotation newAnnotation = new Annotation(topicFromDb, ontologyTerm.getURL());
            term.addAnnotation(newAnnotation);

            factory.getAnnotationDao().persist(newAnnotation);
        }

        // create missing comments
        if (!comments.isEmpty()){
            if (comment == null){
                comment = factory.getCvObjectDao(CvTopic.class).getByIdentifier(CvTopic.COMMENT_MI_REF);
            }

            for (String com : comments){
                Annotation newAnnotation = new Annotation(comment, com);
                term.addAnnotation(newAnnotation);

                factory.getAnnotationDao().persist(newAnnotation);
            }
        }
    }

    public void updateXrefs(CvDagObject term, IntactOntologyTermI ontologyTerm, DaoFactory factory){

        Map<String, Collection<TermDbXref>> ontologyCluster = clusterOntologyReferences(ontologyTerm);
        Map<String, Collection<CvObjectXref>> cvCluster = clusterCvReferences(term);

        XrefDao<CvObjectXref> xrefDao = factory.getXrefDao(CvObjectXref.class);

        for (Map.Entry<String, Collection<CvObjectXref>> cvRef : cvCluster.entrySet()){

            if (ontologyCluster.containsKey(cvRef.getKey())){
                CvDatabase cvDatabase = cvRef.getValue().iterator().next().getCvDatabase();
                Collection<TermDbXref> ontologyReferences = ontologyCluster.get(cvRef.getKey());

                for (CvObjectXref ref : cvRef.getValue()){
                    TermDbXref match = null;

                    for (TermDbXref ontRef : ontologyReferences){
                        if (ontRef.getAccession().equalsIgnoreCase(ref.getPrimaryId())){
                            match = ontRef;
                            String qualifierId = ref.getCvXrefQualifier() != null ? ref.getCvXrefQualifier().getIdentifier() : null;

                            if (!ontRef.getQualifierId().equalsIgnoreCase(qualifierId)){
                                CvXrefQualifier qualifierFromDb = factory.getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(ontRef.getQualifierId());

                                ref.setCvXrefQualifier(qualifierFromDb);
                                xrefDao.update(ref);
                            }
                            break;
                        }
                    }

                    if (match != null){
                        ontologyReferences.remove(match);
                    }
                    // xref to delete
                    else {
                        term.removeXref(ref);
                        xrefDao.delete(ref);
                    }
                }

                // create missing xrefs for this db
                for (TermDbXref termRef : ontologyReferences){
                    CvXrefQualifier qualifierFromDb = factory.getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(termRef.getQualifierId());

                    CvObjectXref newRef = new CvObjectXref(term.getOwner(), cvDatabase, termRef.getAccession(), null, null, qualifierFromDb);
                    term.addXref(newRef);

                    xrefDao.persist(newRef);
                }

                ontologyCluster.remove(cvRef.getKey());
            }
            // delete cv xref because out of date with current ontology
            else {
                Collection<CvObjectXref> refsToDelete = cvRef.getValue();

                for (CvObjectXref r : refsToDelete){
                    term.removeXref(r);
                    xrefDao.delete(r);
                }
            }
        }

        // create missing db xrefs
        for (Map.Entry<String, Collection<TermDbXref>> entry : ontologyCluster.entrySet()){
            CvDatabase cvDatabase = factory.getCvObjectDao(CvDatabase.class).getByPsiMiRef(entry.getKey());

            // create missing xrefs for this db
            for (TermDbXref termRef : entry.getValue()){
                CvXrefQualifier qualifierFromDb = factory.getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(termRef.getQualifierId());

                CvObjectXref newRef = new CvObjectXref(term.getOwner(), cvDatabase, termRef.getAccession(), null, null, qualifierFromDb);
                term.addXref(newRef);

                xrefDao.persist(newRef);
            }
        }
    }

    public void updateAliases(CvDagObject term, IntactOntologyTermI ontologyTerm, DaoFactory factory){
        AliasDao<CvObjectAlias> aliasDao = factory.getAliasDao(CvObjectAlias.class);

        CvAliasType aliasType = factory.getCvObjectDao(CvAliasType.class).getByShortLabel(ALIAS_TYPE);
        Collection<CvObjectAlias> cvAliases = new ArrayList<CvObjectAlias>(term.getAliases());
        Set<String> aliasesToCreate = new HashSet<String>(ontologyTerm.getAliases());

        for (CvObjectAlias alias : cvAliases){
            // only one alias type is allowed
            if (aliasesToCreate.contains(alias.getName())) {
                if (alias.getCvAliasType() == null || (alias.getCvAliasType() != null && !alias.getCvAliasType().getShortLabel().equalsIgnoreCase(ALIAS_TYPE))){
                    alias.setCvAliasType(aliasType);
                    aliasDao.update(alias);
                }

                aliasesToCreate.remove(alias.getName());
            }
            else {
                term.removeAlias(alias);

                aliasDao.delete(alias);
            }
        }

        for (String aliasToCreate : aliasesToCreate){
            CvObjectAlias newAlias = new CvObjectAlias(term.getOwner(), term, aliasType, aliasToCreate);
            term.addAlias(newAlias);

            aliasDao.persist(newAlias);
        }
    }

    private Map<String, Collection<TermDbXref>> clusterOntologyReferences(IntactOntologyTermI ontologyTerm){

        if (ontologyTerm.getDbXrefs().isEmpty()){
            return Collections.EMPTY_MAP;
        }

        Map<String, Collection<TermDbXref>> cluster = new HashMap<String, Collection<TermDbXref>>();

        for (TermDbXref ref : ontologyTerm.getDbXrefs()){
            if (cluster.containsKey(ref.getDatabaseId())){
                cluster.get(ref.getDatabaseId()).add(ref);
            }
            else {
                Collection<TermDbXref> refs = new ArrayList<TermDbXref>();
                refs.add(ref);
                cluster.put(ref.getDatabaseId(), refs);
            }
        }

        return cluster;
    }

    private Map<String, Collection<CvObjectXref>> clusterCvReferences(CvObject term){

        if (term.getXrefs().isEmpty()){
            return Collections.EMPTY_MAP;
        }

        Map<String, Collection<CvObjectXref>> cluster = new HashMap<String, Collection<CvObjectXref>>();

        for (CvObjectXref ref : term.getXrefs()){
            if (cluster.containsKey(ref.getCvDatabase().getIdentifier())){
                cluster.get(ref.getCvDatabase().getIdentifier()).add(ref);
            }
            else {
                Collection<CvObjectXref> refs = new ArrayList<CvObjectXref>();
                refs.add(ref);
                cluster.put(ref.getCvDatabase().getIdentifier(), refs);
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
}
