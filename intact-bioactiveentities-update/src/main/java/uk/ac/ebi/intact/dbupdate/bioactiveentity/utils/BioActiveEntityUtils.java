package uk.ac.ebi.intact.dbupdate.bioactiveentity.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.CvObjectDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 23/05/2013
 * Time: 13:48
 * To change this template use File | Settings | File Templates.
 */

/**
 * Utility methods for BioActive Entities
 */
public class BioActiveEntityUtils {

    /**
     * Sets up a logger for that class.
     */
    public static final Log log = LogFactory.getLog(BioActiveEntityUtils.class);

    private BioActiveEntityUtils() {
    }


    /**
     * Institution owner of the BioActive Entity
     *
     * @return the institution
     */
    public static Institution getInstitution() {
        return IntactContext.getCurrentInstance().getInstitution();
    }

    public static CvDatabase getChEBIDatabase() {
        return getOrCreateCvObjectByMi(
                CvDatabase.class,
                CvDatabase.CHEBI_MI_REF,
                CvDatabase.CHEBI
        );
    }

    public static CvInteractorType getSmallMoleculeType() {
        return getOrCreateCvObjectByMi(
                CvInteractorType.class,
                CvInteractorType.SMALL_MOLECULE_MI_REF,
                CvInteractorType.SMALL_MOLECULE
        );
    }

    public static CvInteractorType getPolysaccharidesType() {
        return getOrCreateCvObjectByMi(
                CvInteractorType.class,
                CvInteractorType.POLYSACCHARIDE_MI_REF,
                CvInteractorType.POLYSACCHARIDE
        );
    }

    public static CvXrefQualifier getPrimaryIDQualifier() {
        return getOrCreateCvObjectByMi(
                CvXrefQualifier.class,
                CvXrefQualifier.IDENTITY_MI_REF,
                CvXrefQualifier.IDENTITY
        );
    }

    public static CvXrefQualifier getSecondaryIDQualifier() {
        return getOrCreateCvObjectByMi(
                CvXrefQualifier.class,
                CvXrefQualifier.SECONDARY_AC_MI_REF,
                CvXrefQualifier.SECONDARY_AC);
    }

    public static CvAliasType getSynonymAliasType() {
        return getOrCreateCvObjectByMi(
                CvAliasType.class,
                CvAliasType.SYNONYM_MI_REF,
                CvAliasType.SYNONYM
        );
    }

    public static CvAliasType getIupacAliasType() {
        return getOrCreateCvObjectByMi(
                CvAliasType.class,
                CvAliasType.IUPAC_NAME_MI_REF,
                CvAliasType.IUPAC_NAME
        );
    }

    public static CvTopic getInchiType() {
        return getOrCreateCvObjectByMi(
                CvTopic.class,
                CvTopic.INCHI_ID_MI_REF,
                CvTopic.INCHI_ID
        );
    }

    public static CvTopic getInchiKeyType() {
        return getOrCreateCvObjectByMi(
                CvTopic.class,
                CvTopic.INCHI_KEY_MI_REF,
                CvTopic.INCHI_KEY
        );
    }

    public static CvTopic getSmilesType() {
        return getOrCreateCvObjectByMi(
                CvTopic.class,
                CvTopic.SMILES_STRING_MI_REF,
                CvTopic.SMILES_STRING
        );
    }

    private static <T extends CvObject> T getOrCreateCvObjectByMi(
            Class<T> cvClass,
            String miIdentifier,
            String shortLabel) {

        final Institution institution = getInstitution();
        final DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();

        CvObjectDao<T> cvObjectDao = daoFactory.getCvObjectDao(cvClass);
        T cv = cvObjectDao.getByIdentifier(miIdentifier);
        if (cv == null) {
            cv = CvObjectUtils.createCvObject(institution, cvClass, miIdentifier, shortLabel);
            log.debug("Could not find CvObject by MI (" + miIdentifier + ") and name (" + shortLabel + "). A new one will be created");
        }
        return cv;

    }

    // It chops a list into view sublists of length L using sublist
    // sublist returns a view of the portion of this list between the specified fromIndex,
    // inclusive, and toIndex, exclusive. (If fromIndex and toIndex are equal, the returned list is empty.)
    // The returned list is backed by this list, so non-structural changes in the returned list are reflected
    // in this list, and vice-versa. The returned list supports all of the optional
    // list operations supported by this list.
    // In this case as we are not going to modify the lists is more efficient to use sublist
    public static <T> List<List<T>> splitter(List<T> list, final int L) {
        List<List<T>> parts = new ArrayList<List<T>>();
        final int N = list.size();
        for (int i = 0; i < N; i += L) {
            parts.add(list.subList(i, Math.min(N, i + L))
            );
        }
        return parts;
    }

}
