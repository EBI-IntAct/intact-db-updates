package uk.ac.ebi.intact.dbupdate.cv.updater;

import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyAccess;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyTermI;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.cv.CvUpdateContext;
import uk.ac.ebi.intact.dbupdate.cv.events.UpdatedEvent;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * This class will create missing used-in-class annotations for cvtopics
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>20/01/12</pre>
 */

public class UsedInClassAnnotationUpdaterImpl implements UsedInClassAnnotationUpdater{

    private Map<String, String> cvTopic2UsedInClass;

    private Set<String> setOfUsedInClass;

    private String classSeparator = ";";

    public UsedInClassAnnotationUpdaterImpl(){
        cvTopic2UsedInClass = new HashMap<String, String>();
        setOfUsedInClass = new HashSet<String>();
    }

    @PostConstruct
    private void initializeMapCvTopic2UsedInClass(){

        cvTopic2UsedInClass.put("MI:2089", "uk.ac.ebi.intact.model.SmallMolecule");
        cvTopic2UsedInClass.put("MI:0667", "uk.ac.ebi.intact.model.CvObject");
        cvTopic2UsedInClass.put("MI:0665", "uk.ac.ebi.intact.model.Experiment");
        cvTopic2UsedInClass.put("MI:1093", "uk.ac.ebi.intact.model.Publication");
        cvTopic2UsedInClass.put("MI:0668", "uk.ac.ebi.intact.model.Feature");
        cvTopic2UsedInClass.put("MI:0664", "uk.ac.ebi.intact.model.Interaction");
        cvTopic2UsedInClass.put("MI:0669", "uk.ac.ebi.intact.model.BioSource");
        cvTopic2UsedInClass.put("MI:0666", "uk.ac.ebi.intact.model.Component");

    }

    public boolean canUpdate(CvDagObject cvObject){
        if (cvObject instanceof CvTopic){
            return true;
        }

        return false;
    }

    private void extractUsedInClassForTerm(IntactOntologyTermI ontologyTerm, IntactOntologyAccess ontologyAccess){

        Collection<IntactOntologyTermI> allParents = ontologyAccess.getAllParents(ontologyTerm);

        for (IntactOntologyTermI parent : allParents){
            if (cvTopic2UsedInClass.containsKey(parent.getTermAccession())){
                setOfUsedInClass.add(cvTopic2UsedInClass.get(parent.getTermAccession()));
            }
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void updateAnnotations(CvUpdateContext updateContext, UpdatedEvent updateEvt){
        setOfUsedInClass.clear();

        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        IntactOntologyTermI ontologyTerm = updateContext.getOntologyTerm();
        IntactOntologyAccess ontologyAccess = updateContext.getOntologyAccess();

        CvDagObject term = updateContext.getCvTerm();

        extractUsedInClassForTerm(ontologyTerm, ontologyAccess);

        // the term is known to have used in class annotations
        if (!setOfUsedInClass.isEmpty()){
            Collection<Annotation> annotations = term.getAnnotations();
            boolean hasFoundUsedInClass = false;
            boolean hasBeenUpdated = false;

            for (Annotation ann : annotations){
                if (ann.getCvTopic() != null && CvTopic.USED_IN_CLASS.equalsIgnoreCase(ann.getCvTopic().getShortLabel())){
                    hasFoundUsedInClass = true;

                    String usedInClassString = ann.getAnnotationText();
                    StringBuffer newUsedInClass = new StringBuffer();
                    newUsedInClass.append(usedInClassString);

                    if (!usedInClassString.contains(classSeparator)){

                        for (String objClass : setOfUsedInClass){
                            if (!usedInClassString.equals(objClass)){
                                hasBeenUpdated = true;
                                newUsedInClass.append(classSeparator);
                                newUsedInClass.append(objClass);
                            }
                        }
                    }
                    else {
                        Collection<String> existingUsedInClass = Arrays.asList(usedInClassString.split(classSeparator));

                        for (String objClass : setOfUsedInClass){
                            if (!existingUsedInClass.contains(objClass)){
                                hasBeenUpdated = true;
                                newUsedInClass.append(classSeparator);
                                newUsedInClass.append(objClass);
                            }
                        }
                    }

                    if(hasBeenUpdated){
                        ann.setAnnotationText(newUsedInClass.toString());
                        if (updateEvt != null){
                            updateEvt.getUpdatedAnnotations().add(ann);
                        }
                    }
                }
            }

            // the used in class annotation has not been found so we create one
            if (!hasFoundUsedInClass){
                CvTopic usedInClass = factory.getCvObjectDao(CvTopic.class).getByShortLabel(CvTopic.USED_IN_CLASS);

                if (usedInClass == null){
                    usedInClass = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, null, CvTopic.USED_IN_CLASS);
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(usedInClass);
                }

                Annotation usedInClassAnnot = new Annotation(usedInClass, StringUtils.join(setOfUsedInClass, classSeparator));
                term.addAnnotation(usedInClassAnnot);

                if (updateEvt != null){
                    updateEvt.getCreatedAnnotations().add(usedInClassAnnot);
                }
            }
        }

        setOfUsedInClass.clear();
    }

    public String getClassSeparator() {
        return classSeparator;
    }

    public void setClassSeparator(String classSeparator) {
        this.classSeparator = classSeparator;
    }

    public void clear(){
        setOfUsedInClass.clear();
    }
}
