package uk.ac.ebi.intact.dbupdate.prot.util;

import org.apache.commons.collections.CollectionUtils;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.persistence.dao.AnnotationDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.persistence.dao.InteractionDao;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>17-Dec-2010</pre>
 */

public class ComponentTools {

    /**
     * !!! don't check the interactor of the components, it is supposing that we did a merge of the proteins and the interactor is the same
     * !!!! all interactions have ac
     * @param c1
     * @param c2
     * @return
     */
    public static boolean areEqualParticipants(Component c1, Component c2){
        if (c1 == null && c2 == null){
            return true;
        }
        else if (c1 != null && c2 != null){
            // check cvs and interactor first, and then check the interaction
            if (c1.getStoichiometry() != c2.getStoichiometry() || c1.getStoichiometry() > 0 || c2.getStoichiometry() > 0){
                return false;
            }
            if ( !CollectionUtils.isEqualCollection(c1.getExperimentalRoles(), c2.getExperimentalRoles()) ) {
                return false;
            }

            if ( c1.getCvBiologicalRole() != null) {

                if (!c1.getCvBiologicalRole().equals( c2.getCvBiologicalRole() ) ){
                    return false;
                }
            }
            else if (c2.getCvBiologicalRole() != null){
                return false;
            }

            if ((c1.getInteraction() != null && c2.getInteraction() == null) || (c1.getInteraction() == null && c2.getInteraction() != null)){
                return false;
            }
            else if ( c1.getInteraction() != null && c2.getInteraction() != null ) {
                if ( !c1.getInteraction().getAc().equals(c2.getInteraction().getAc()) ) {
                    return false;
                }
            }

            if ( !isEqualCollectionOfFeatures( c1.getBindingDomains(), c2.getBindingDomains() ) ) {
                return false;
            }

            if (!areCollectionEqual(c1.getAliases(), c2.getAliases())){
                return false;
            }
            if (!areCollectionEqual(c1.getAnnotations(), c2.getAnnotations())){
                return false;
            }
            if (!areCollectionEqual(c1.getXrefs(), c2.getXrefs())){
                return false;
            }

            return true;
        }
        else{
            return false;
        }
    }

    public static boolean containsParticipant(Protein p, Component c){
        for (Component comp : p.getActiveInstances()){
            if (areEqualParticipants(comp, c)){
                return true;
            }
        }

        return false;
    }

    public static boolean isEqualCollectionOfFeatures(Collection<Feature> f1, Collection<Feature> f2){
        if (f1.size() == f2.size()){

            for (Feature f : f1){
                boolean hasFoundCvFeatureIdentification = false;
                boolean hasFoundCvFeatureType= false;
                boolean hasFoundRanges = false;
                boolean hasFoundAlias = false;
                boolean hasFoundXRef= false;
                boolean hasFoundAnnotation = false;

                boolean hasFoundFeature = false;

                for (Feature fs : f2){
                    if ( f.getCvFeatureIdentification() != null ? f.getCvFeatureIdentification().equals( fs.getCvFeatureIdentification() ) : fs.getCvFeatureIdentification() == null ) {
                        hasFoundCvFeatureIdentification = true;
                    }

                    if ( f.getCvFeatureType() != null ? f.getCvFeatureType().equals( fs.getCvFeatureType() ) : fs.getCvFeatureType() == null ) {
                        hasFoundCvFeatureType = true;
                    }

                    if (areCollectionEqual(f.getRanges(), fs.getRanges())){
                        hasFoundRanges = true;
                    }

                    if (areCollectionEqual(f.getAliases(), fs.getAliases())){
                        hasFoundAlias = true;
                    }
                    if (areCollectionEqual(f.getAnnotations(), fs.getAnnotations())){
                        hasFoundAnnotation = true;
                    }
                    if (areCollectionEqual(f.getXrefs(), fs.getXrefs())){
                        hasFoundXRef = true;
                    }

                    if (hasFoundAlias && hasFoundAnnotation && hasFoundCvFeatureIdentification && hasFoundCvFeatureType && hasFoundRanges && hasFoundXRef){
                        hasFoundFeature = true;
                        break;
                    }
                }

                if (!hasFoundFeature){
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    private static boolean areCollectionEqual(Collection<? extends IntactObject> intactObjects1, Collection<? extends IntactObject> intactObjects2) {
        if (intactObjects1.size() != intactObjects2.size()) {
            return false;
        }

        List<String> uniqueStrings1 = new ArrayList<String>();

        for (IntactObject io1 : intactObjects1) {
            uniqueStrings1.add(createUniqueString(io1));
        }

        for (IntactObject io2 : intactObjects2) {
            String unique2 = createUniqueString(io2);

            if (!uniqueStrings1.contains(unique2)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Creates unique strings for Annotations,Xrefs and aliases.
     * @param io the object to use
     * @return a unique string for that object
     */
    protected static String createUniqueString(IntactObject io) {
        if (io == null) throw new NullPointerException("IntactObject cannot be null to create a unique String");

        if (io instanceof Annotation) {
            Annotation annot = (Annotation)io;
            String cvId = (annot.getCvTopic() != null)? annot.getCvTopic().getIdentifier() : "";
            return annot.getAnnotationText()+"__"+cvId;
        } else if (io instanceof Xref) {
            Xref xref = (Xref)io;
            String qualId = (xref.getCvXrefQualifier() != null)? xref.getCvXrefQualifier().getIdentifier() : "";
            return xref.getPrimaryId()+"__"+xref.getCvDatabase().getIdentifier()+"__"+qualId;
        } else if (io instanceof Alias) {
            Alias alias = (Alias)io;
            String typeId = (alias.getCvAliasType() != null)? alias.getCvAliasType().getIdentifier() : "";
            return alias.getName()+"__"+typeId;
        }
        else if (io instanceof Range) {
            Range r = (Range) io;
            String rtype = (r.getFromCvFuzzyType() != null ? r.getFromCvFuzzyType().getIdentifier() : "") + "_" + r.getFromIntervalStart()
                    + "-" + r.getFromIntervalEnd() + "_" + (r.getToCvFuzzyType() != null ? r.getToCvFuzzyType().getIdentifier() : "") +
                    "_" + r.getToIntervalStart() + "-" + r.getToIntervalEnd() + "_" + (r.getFullSequence() != null ? r.getFullSequence() : "");

            return rtype;
        }
        return io.toString();
    }

    /**
     * Two new annotations will be added : a 'no-uniprot-update' and a 'caution' explaining that this protein is now obsolete in uniprot
     * @param protein :the dead protein in IntAct
     */
    public static void addCautionDuplicatedComponent(Protein protein, Protein proteinToDelete, Interaction interaction, DataContext context){
        DaoFactory factory = context.getDaoFactory();
        Collection<Annotation> annotations = interaction.getAnnotations();

        // caution CvTopic
        CvTopic caution = factory.getCvObjectDao(CvTopic.class).getByPsiMiRef(CvTopic.CAUTION_MI_REF);

        if (caution == null) {
            caution = CvObjectUtils.createCvObject(protein.getOwner(), CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);
            factory.getCvObjectDao(CvTopic.class).saveOrUpdate(caution);
        }

        boolean has_caution = false;
        String cautionMessage = "["+proteinToDelete.getAc()+"] The protein has been merged with " + protein.getAc() + " so the duplicated interactor has been deleted from this interaction.";

        for (Annotation annotation : annotations){
            if (caution.equals(annotation.getCvTopic())){
                if (annotation.getAnnotationText() != null){
                    if (annotation.getAnnotationText().equalsIgnoreCase(cautionMessage)){
                        has_caution = true;
                    }
                }
            }
        }

        AnnotationDao annotationDao = factory.getAnnotationDao();
        InteractionDao interactionDao = factory.getInteractionDao();

        // if no 'caution' exists, add the annotation
        if (!has_caution){
            Annotation obsolete = new Annotation(caution, cautionMessage);
            annotationDao.persist(obsolete);

            interaction.addAnnotation(obsolete);
        }

        interactionDao.update((InteractionImpl) interaction);
    }
}
