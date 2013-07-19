package uk.ac.ebi.intact.dbupdate.gene.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.CvObjectDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 11/07/2013
 * Time: 16:23
 * To change this template use File | Settings | File Templates.
 */
public class GeneUtils {

	/**
	 * Sets up a logger for that class.
	 */
	public static final Log log = LogFactory.getLog(GeneUtils.class);

	/**
	 * Institution owner of the Gene
	 *
	 * @return the institution
	 */
	public static Institution getInstitution() {
		return IntactContext.getCurrentInstance().getInstitution();
	}

	public static CvDatabase getEnsemblDatabase() {
		return getOrCreateCvObjectByMi(
				CvDatabase.class,
				CvDatabase.ENSEMBL_MI_REF,
				CvDatabase.ENSEMBL
		);
	}


//    public static CvInteractorType getGeneType() {
//        return getOrCreateCvObjectByMi(
//                CvInteractorType.class,
//                CvInteractorType.GENE_MI_REF,
//                CvInteractorType.GENE
//        );    }

	public static CvInteractorType getGeneType() {
		return getOrCreateCvObjectByMi(
				CvInteractorType.class,
				"MI:0250",
				"gene"
		);
	}

	public static CvXrefQualifier getPrimaryIDQualifier() {
		return getOrCreateCvObjectByMi(
				CvXrefQualifier.class,
				CvXrefQualifier.IDENTITY_MI_REF,
				CvXrefQualifier.IDENTITY
		);
	}

	public static CvAliasType getSynonymAliasType() {
		return getOrCreateCvObjectByMi(
				CvAliasType.class,
				CvAliasType.SYNONYM_MI_REF,
				CvAliasType.SYNONYM
		);
	}

	public static CvAliasType getGeneNameAliasType() {
		return getOrCreateCvObjectByMi(
				CvAliasType.class,
				CvAliasType.GENE_NAME_MI_REF,
				CvAliasType.GENE_NAME
		);
	}

	public static CvAliasType getGeneNameSynonymAliasType() {
		return getOrCreateCvObjectByMi(
				CvAliasType.class,
				CvAliasType.GENE_NAME_SYNONYM_MI_REF,
				CvAliasType.GENE_NAME_SYNONYM
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

	public static String generateShortLabel(String entryName) {
		return (entryName + "_gene").toLowerCase();
	}

}
