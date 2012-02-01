package uk.ac.ebi.intact.dbupdate.cv.utils;

import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.CvObjectDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.XrefUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for cv update
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>14/11/11</pre>
 */

public class CvUpdateUtils {
    public static final Pattern decimalPattern = Pattern.compile("\\d");

    public static Annotation hideTerm(CvObject c, String message){
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        CvTopic topicFromDb = factory.getCvObjectDao(CvTopic.class).getByShortLabel(CvTopic.HIDDEN);

        if (topicFromDb == null){
            topicFromDb = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, null, CvTopic.HIDDEN);
            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(topicFromDb);
        }

        Annotation newAnnotation = new Annotation(topicFromDb, message);
        c.addAnnotation(newAnnotation);

        return newAnnotation;
    }

    public static CvObjectXref createIdentityXref(CvDagObject term, String database, String identifier) {
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        CvObjectXref cvXref;
        CvXrefQualifier identity = factory.getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.IDENTITY_MI_REF);
        CvDatabase db = factory.getCvObjectDao(CvDatabase.class).getByPsiMiRef(database);

        if (identity == null){
            identity = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF, CvXrefQualifier.IDENTITY);
            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(identity);
        }
        if (db == null){
            db = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvDatabase.class, database, database);
            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(db);
        }
        // create identity xref
        cvXref = XrefUtils.createIdentityXref(term, identifier, identity, db);
        term.addXref(cvXref);

        return cvXref;
    }

    public static CvObjectXref createSecondaryXref(CvDagObject term, String database, String identifier) {
        DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

        CvObjectXref cvXref;
        CvXrefQualifier secondary = factory.getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.SECONDARY_AC_MI_REF);
        CvDatabase db = factory.getCvObjectDao(CvDatabase.class).getByPsiMiRef(database);

        if (secondary == null){
            secondary = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvXrefQualifier.class, CvXrefQualifier.SECONDARY_AC_MI_REF, CvXrefQualifier.SECONDARY_AC);
            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(secondary);
        }
        if (db == null){
            db = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvDatabase.class, database, database);
            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(db);
        }
        // create identity xref
        cvXref = XrefUtils.createIdentityXref(term, identifier, secondary, db);
        term.addXref(cvXref);

        return cvXref;
    }

    public static Collection<CvObjectXref> extractIdentityXrefFrom(CvDagObject cv, String databaseId){
        Collection<CvObjectXref> existingIdentities = XrefUtils.getIdentityXrefs(cv);
        Collection<CvObjectXref> identities = new ArrayList<CvObjectXref>(existingIdentities.size());

        for (CvObjectXref ref : existingIdentities){
            if (ref.getCvDatabase() != null && ref.getCvDatabase().getIdentifier().equalsIgnoreCase(databaseId)){
                identities.add(ref);
            }
        }

        return identities;
    }

    public static String createSyncLabelIfNecessary(String shortLabel, Class<? extends CvDagObject> termClass){
        CvObjectDao cvDao = IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(termClass);

        AnnotatedObject existingOriginalShortLabel = cvDao.getByShortLabel(shortLabel);
        List<String> existingLabels = cvDao.getShortLabelsLike(shortLabel + "-%");

        // no existing label
        if (existingOriginalShortLabel == null){
            return shortLabel;
        }
        // the label is already used but we don't have any indexed labels in the database
        else if (existingOriginalShortLabel != null && existingLabels.isEmpty()){
            return  shortLabel + "-2";
        }
        // the label is already used and some labels may already use indexes so we need to find out which unused index we want to use
        else {
            int currentIndex = 0;

            for (String existing : existingLabels) {
                if (existing.contains("-")){
                    String strSuffix = existing.substring(existing.lastIndexOf("-") + 1, existing.length());
                    String originalLabel = existing.substring(0, existing.lastIndexOf(strSuffix));

                    Matcher matcher = decimalPattern.matcher(strSuffix);

                    if (matcher.matches() && originalLabel.equalsIgnoreCase(shortLabel)){
                        currentIndex = Math.max(currentIndex, Integer.parseInt(matcher.group()));
                    }
                }
            }

            // the shortlabel is used with some index in the database. We need to increase this index
            if (currentIndex > 0){
                return shortLabel + "-" + Integer.toString(currentIndex ++);
            }
            // the similar shortlabels are 'false positive', we can use the shortlabel with -2 because the original shortlabel is already used
            else {
                return shortLabel + "-2";
            }
        }
    }

    public static Integer extractChunkNumberFromShortLabel(String shortLabel){

        Integer currentIndex = null;

        if (shortLabel.contains("-")){
            currentIndex = 0;

            String strSuffix = shortLabel.substring(shortLabel.lastIndexOf("-") + 1, shortLabel.length());

            Matcher matcher = decimalPattern.matcher(strSuffix);

            if (matcher.matches()){
                currentIndex = Math.max(currentIndex, Integer.parseInt(matcher.group()));
            }
        }

        return currentIndex;
    }
}
