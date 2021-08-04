package uk.ac.ebi.intact.dbupdate.prot.actions.deleters;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.ProteinImpl;

/**
 * Removes a protein from the database
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class ProteinDeleter {

    /**
     * The logger of this class
     */
    private static final Log log = LogFactory.getLog( ProteinDeleter.class );

    /**
     * Delete the protein from the database
     * @param evt : contains the protein to delete
     * @return true if the protein is deleted from the database, false otherwise
     * @throws ProcessorException
     */
    public boolean delete(ProteinEvent evt) throws ProcessorException {
        boolean isDeleted = deleteProtein(evt.getProtein(), evt);

        if (isDeleted && evt.getSource() instanceof ProteinUpdateProcessor){
            // log in 'deleted.csv'
            ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
            processor.fireOnDelete(evt);
        }

        // delete the protein
        return isDeleted;
    }

    /**
     * Delete the protein if it is not already done
     * @param protein
     * @param evt
     * @return true if the protein is deleted from the database, false otherwise
     */
    private boolean deleteProtein(Protein protein, ProteinEvent evt) {
        if (log.isDebugEnabled()) log.debug("Deleting protein: "+protein.getAc());

        ProteinDao proteinDao = evt.getDataContext().getDaoFactory().getProteinDao();

        if (protein.getAc() != null) {

            if (!proteinDao.isTransient((ProteinImpl) protein)) {

                proteinDao.delete((ProteinImpl) protein);
            } else {
                proteinDao.deleteByAc(protein.getAc());
            }
            return true;
        }
        return false;
    }
}