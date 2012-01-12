package uk.ac.ebi.intact.dbupdate.cv.utils;

import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.XrefUtils;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Utility class for cv update
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>14/11/11</pre>
 */

public class CvUpdateUtils {

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
}
