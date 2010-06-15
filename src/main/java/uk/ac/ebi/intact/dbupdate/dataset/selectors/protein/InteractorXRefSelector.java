package uk.ac.ebi.intact.dbupdate.dataset.selectors.protein;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.dataset.DatasetException;
import uk.ac.ebi.intact.dbupdate.dataset.selectors.DatasetSelectorImpl;
import uk.ac.ebi.intact.model.CvDatabase;

import javax.persistence.Query;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>15-Jun-2010</pre>
 */

public class InteractorXRefSelector extends DatasetSelectorImpl implements ProteinDatasetSelector {

    /**
     * The map containing the database associated with a set of identifiers
     */
    private Map<CvDatabase, Set<String>> listOfXRefs = new HashMap<CvDatabase, Set<String>>();

    public InteractorXRefSelector(){
        super();
    }

    public InteractorXRefSelector(IntactContext context){
        super(context);
    }

    private List<String> getProteinAccessionsContainingXRefs(CvDatabase database, String identifier) throws DatasetException {
        // get the intact datacontext and factory
        final DataContext dataContext = this.context.getDataContext();
        final DaoFactory daoFactory = dataContext.getDaoFactory();

        // we want all the interactor associated with this alias name and this alias type
        String interactorGeneQuery = "select i.ac from InteractorImpl i join i.xrefs as x " +
                "join x.cvDatabase as db " +
                "where db = :database and x.primaryId = :id";

        // we add the organism restrictions
        String finalQuery = addOrganismSelection(interactorGeneQuery);

        // create the query
        final Query query = daoFactory.getEntityManager().createQuery(finalQuery);

        // set the query parameters
        query.setParameter("database", database);
        query.setParameter("id", identifier);

        // get the intact proteins matching the cross reference
        List<String> listOfAcs = query.getResultList();

        return listOfAcs;
    }

    public Set<String> getSelectionOfProteinAccessionsInIntact() throws DatasetException {
        // we need an Intact context to query Intact
        if (this.context == null){
            throw new DatasetException("The intact context must be not null, otherwise the protein selector can't query the Intact database.");
        }

        log.info("Collect proteins in Intact...");

        // The cross references should be initialised before
        if (this.listOfXRefs.isEmpty()){
            throw new IllegalArgumentException("The list of protein cross references has not been initialised.");
        }

        // The dataset value associated with the list of protein aliases should be initialised before
        if (this.datasetValue == null){
            throw new IllegalArgumentException("The dataset value has not been initialised.");
        }

        // create the file where to write the report
        File file = new File("component_selected_for_dataset_" + Calendar.getInstance().getTime().getTime()+".txt");
        Set<String> proteinAccessions = new HashSet<String>();

        try {
            Writer writer = new FileWriter(file);

            // For each database in the file
            for (Map.Entry<CvDatabase, Set<String>> entry : this.listOfXRefs.entrySet()){
                // For each identifier associated with this database
                for (String id : entry.getValue()){
                    // get the intact proteins matching the database and identifier
                    List<String> proteinAccessionsForName = getProteinAccessionsContainingXRefs(entry.getKey(), id);
                    // we add the proteins to the list
                    proteinAccessions.addAll(proteinAccessionsForName);
                    writer.write("Collect "+proteinAccessionsForName.size()+" proteins ("+proteinAccessionsForName+") associated with the cross reference " + id + " \n");
                    writer.flush();
                }
            }

            writer.close();

            if (!isFileWriterEnabled()){
                file.delete();
            }

        } catch (IOException e) {
            throw new DatasetException("We can't write the results of the protein selection.", e);
        }

        return proteinAccessions;
    }

    public void clearDatasetContent() {
        super.clearDatasetContent();
        this.listOfXRefs.clear();
    }

    @Override
    protected void readSpecificContent(String[] columns) throws DatasetException {
        // the mi identifier of the type is the second column
        String mi = columns[1];
        // the name of the type is the first column
        String name = columns[0];

        // the database
        CvDatabase database = null;

        // we need an Intact context
        if (this.context == null){
            throw new DatasetException("The intact context must be not null, otherwise the protein selector can't query the Intact database.");
        }

        if (columns [2].length() > 0){
            // get the cvDatabase instance
            if (mi != null && mi.length() > 0){
                database = this.context.getDataContext().getDaoFactory().getCvObjectDao( CvDatabase.class ).getByPsiMiRef( mi );
            }
            else {
                if (name.length() > 0){
                    database = this.context.getDataContext().getDaoFactory().getCvObjectDao( CvDatabase.class ).getByShortLabel( name );
                }
            }

            if (database == null){
                throw new DatasetException("The database " + mi + ": "+name+" is not known and it is a mandatory field.");
            }

            // we have not already loaded this alias type, we add the new alias with its alias type
            if (!this.listOfXRefs.containsKey(database)){
                Set<String> values = new HashSet<String>();
                values.add(columns[2]);
                this.listOfXRefs.put(database, values);
            }
            // the alias type is already loaded, we add the new alias only
            else {
                Set<String> values = this.listOfXRefs.get(database);

                values.add(columns[2]);
            }
        }
    }

    public Map<CvDatabase, Set<String>> getListOfXRefs() {
        return listOfXRefs;
    }

    public void setListOfXRefs(Map<CvDatabase, Set<String>> listOfXRefs) {
        this.listOfXRefs = listOfXRefs;
    }
}
