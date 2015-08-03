package uk.ac.ebi.intact.dbupdate.dataset.selectors.protein;

import org.springframework.transaction.TransactionStatus;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.dataset.DatasetException;
import uk.ac.ebi.intact.model.CvDatabase;
import uk.ac.ebi.intact.model.ProteinImpl;

import javax.persistence.Query;
import java.io.IOException;
import java.util.*;

/**
 * An InteractorXRefsSelector is a DatasetSelector which is collecting proteins in IntAct with a specific
 * cross reference and organism. Usually the list of cross references as well as the organisms and the dataset value are in a configuration file
 * but we can use the methods setListOfXRefs, setDatasetValue and setPossibleTaxId if the data are not taken from a file.
 * The configuration file of this selector must have 3 columns : one for the name of the parameter (ex : dataset, go, interpro), one for the mi number of this parameter (ex : MI:0875)
 * and one for the value of the parameter (ex : Interactions of proteins with an established role in the presynapse, database identifiers).
 * We must have a line containing the dataset value, another containing the list of organism taxIds (separated by semi-colon) which can
 * be empty and the other lines contain database cross references (Ex of line : interpro	MI:0449	IPR001564).
 * All the columns are tab separated.
 * You can add an extra line containing publication to exclude (column name = publication and the pubmed ids are seperated by semi-colon)).
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>15-Jun-2010</pre>
 */

public class InteractorXRefSelector extends ProteinDatasetSelectorImpl {

    /**
     * The map containing the database associated with a set of identifiers
     */
    private Map<CvDatabase, Set<String>> listOfXRefs = new HashMap<CvDatabase, Set<String>>();

    /**
     * Create a new InteractorXRefSelector instance. The intact context should be set with setIntactContext.
     */
    public InteractorXRefSelector(){
        super();
    }

    public InteractorXRefSelector(String report){
        super(report);
    }

    /**
     *
     * @return the Set of protein accessions containing at least one of the cross references stored in the listOfXRefs of this object
     * @throws DatasetException
     */
    public Set<String> collectSelectionOfProteinAccessionsInIntact() throws DatasetException {

        log.info("Collect proteins in Intact...");

        // The cross references should be initialised before
        if (this.listOfXRefs.isEmpty()){
            throw new IllegalArgumentException("The list of protein cross references has not been initialised.");
        }

        // The dataset value associated with the list of protein aliases should be initialised before
        if (this.datasetValue == null){
            throw new IllegalArgumentException("The dataset value has not been initialised.");
        }

        Set<String> proteinAccessions = new HashSet<String>();

        // get the intact datacontext and factory
        final DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();
        final DaoFactory daoFactory = dataContext.getDaoFactory();

        StringBuffer interactorXRefQuery = new StringBuffer(1064);

        // we want all the interactor associated with this database and this identifier
        interactorXRefQuery.append("select i.ac from InteractorImpl i join i.xrefs as x " +
                "join x.cvDatabase as db " +
                "where (");

        try {

            // For each database in the file
            for (Map.Entry<CvDatabase, Set<String>> entry : this.listOfXRefs.entrySet()){
                // For each identifier associated with this database
                for (String id : entry.getValue()){

                    interactorXRefQuery.append(" (db.ac = '"+entry.getKey().getAc()+"' and x.primaryId = '"+id+"') or");
                }
            }

            interactorXRefQuery.delete(interactorXRefQuery.lastIndexOf(" or"), interactorXRefQuery.length());
            interactorXRefQuery.append(") and i.objClass = :class");

            // we add the organism restrictions
            String finalQuery = addOrganismSelection(interactorXRefQuery.toString());

            TransactionStatus status = dataContext.beginTransaction();

            // create the query
            final Query query = daoFactory.getEntityManager().createQuery(finalQuery);

            // set the query parameters
            query.setParameter("class", ProteinImpl.class.getName());

            // get the intact proteins matching the cross reference
            List<String> listOfAcs = query.getResultList();

            dataContext.commitTransaction(status);

            // we add the proteins to the list
            proteinAccessions.addAll(listOfAcs);

            // write protein report
            writeProteinReport(proteinAccessions);

        } catch (IOException e) {
            throw new DatasetException("We can't write the results of the protein selection.", e);
        }

        return proteinAccessions;
    }

    /**
     * Clear the listOfXRefs and the datasetValue
     */
    public void clearDatasetContent() {
        super.clearDatasetContent();
        this.listOfXRefs.clear();
    }

    /**
     * Collect the cross reference contained in the columns
     * @param columns : the columns of a line in the file
     * @throws DatasetException
     */
    @Override
    protected void readSpecificContent(String[] columns) throws DatasetException {
        // the mi identifier of the database is the second column
        String mi = columns[1];
        // the name of the database is the first column
        String name = columns[0];

        // the database
        CvDatabase database = null;

        IntactContext context = IntactContext.getCurrentInstance();

        if (columns [2].length() > 0){
            // get the cvDatabase instance
            if (mi != null && mi.length() > 0){
                database = context.getDataContext().getDaoFactory().getCvObjectDao( CvDatabase.class ).getByPsiMiRef( mi );
            }
            else {
                if (name.length() > 0){
                    database = context.getDataContext().getDaoFactory().getCvObjectDao( CvDatabase.class ).getByShortLabel( name );
                }
            }

            if (database == null){
                throw new DatasetException("The database " + mi + ": "+name+" is not known and it is a mandatory field.");
            }

            // we have not already loaded this database, we add the new identifier with its database
            if (!this.listOfXRefs.containsKey(database)){
                Set<String> values = new HashSet<String>();
                values.add(columns[2]);
                this.listOfXRefs.put(database, values);
            }
            // the database is already loaded, we add the new identifier only
            else {
                Set<String> values = this.listOfXRefs.get(database);

                values.add(columns[2]);
            }
        }
    }

    /**
     *
     * @return the map containing all the cross references
     */
    public Map<CvDatabase, Set<String>> getListOfXRefs() {
        return listOfXRefs;
    }
}
