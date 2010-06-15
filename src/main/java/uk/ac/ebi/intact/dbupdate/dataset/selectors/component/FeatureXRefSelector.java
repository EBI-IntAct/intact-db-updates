package uk.ac.ebi.intact.dbupdate.dataset.selectors.component;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.dataset.DatasetException;
import uk.ac.ebi.intact.dbupdate.dataset.selectors.DatasetSelectorImpl;
import uk.ac.ebi.intact.model.CvDatabase;
import uk.ac.ebi.intact.model.CvXrefQualifier;

import javax.persistence.Query;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>15-Jun-2010</pre>
 */

public class FeatureXRefSelector extends DatasetSelectorImpl implements ComponentDatasetSelector{

    private CvDatabase database;

    private CvXrefQualifier qualifier;

    private String databaseId;

    public FeatureXRefSelector(){
        super();
        this.database = null;
        this.qualifier = null;
        this.databaseId = null;
    }

    public FeatureXRefSelector(IntactContext context){
        super(context);
        this.database = null;
        this.qualifier = null;
        this.databaseId = null;
    }

    public Set<String> getSelectionOfComponentAccessionsInIntact() throws DatasetException {
        // we need an Intact context to query Intact
        if (this.context == null){
            throw new DatasetException("The intact context must be not null, otherwise the protein selector can't query the Intact database.");
        }

        log.info("Collect proteins in Intact...");

        // The cvDatabase should be initialised before
        if (this.database == null){
            throw new IllegalArgumentException("The database of the feature cross references has not been initialised.");
        }
        // The cvXRefQualifier should be initialised before
        if (this.qualifier == null){
            throw new IllegalArgumentException("The qualifier of the feature cross references has not been initialised.");
        }
        // The identifier should be initialised before
        if (this.databaseId == null){
            throw new IllegalArgumentException("The identifier of the feature cross references has not been initialised.");
        }
        // The dataset value associated with the list of protein aliases should be initialised before
        if (this.datasetValue == null){
            throw new IllegalArgumentException("The dataset value has not been initialised.");
        }

        // create the file where to write the report
        File file = new File("proteins_selected_for_dataset_" + Calendar.getInstance().getTime().getTime()+".txt");
        Set<String> proteinAccessions = new HashSet<String>();

        try {
            Writer writer = new FileWriter(file);

            // get the intact datacontext and factory
            final DataContext dataContext = this.context.getDataContext();
            final DaoFactory daoFactory = dataContext.getDaoFactory();

            // we want all the interactor associated with this alias name and this alias type
            String interactorGeneQuery = "select c.ac from Feature f join f.component as c join f.xrefs as x " +
                    "join x.cvXrefQualifier as q join x.cvDatabase as db " +
                    "where db = :database and q = :qualifier and x.primaryId = :id";

            // we add the organism restrictions
            String finalQuery = addOrganismSelection(interactorGeneQuery);

            // create the query
            final Query query = daoFactory.getEntityManager().createQuery(finalQuery);

            // set the query parameters
            query.setParameter("database", this.database);
            query.setParameter("qualifier", this.qualifier);
            query.setParameter("id", this.databaseId);

            // get the intact proteins matching the feature cross reference
            List<String> proteinAccessionsForName = query.getResultList();
            // we add the proteins to the list
            proteinAccessions.addAll(proteinAccessionsForName);
            writer.write("Collect "+proteinAccessionsForName.size()+" proteins ("+proteinAccessionsForName+") associated with the feature cross reference " + this.databaseId + " \n");
            writer.flush();

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
        this.qualifier = null;
        this.database = null;
        this.databaseId = null;
    }

    @Override
    protected void readSpecificContent(String[] columns) throws DatasetException {
        // the mi identifier of the type is the second column
        String mi = columns[1];
        // the name of the type is the first column
        String name = columns[0];

        // we need an Intact context
        if (this.context == null){
            throw new DatasetException("The intact context must be not null, otherwise the protein selector can't query the Intact database.");
        }

        if (columns [2].length() > 0){
            // get the cvDatabase instance
            if (mi != null && mi.length() > 0){
                this.database = this.context.getDataContext().getDaoFactory().getCvObjectDao( CvDatabase.class ).getByPsiMiRef( mi );
            }
            else {
                if (name.length() > 0){
                    this.database = this.context.getDataContext().getDaoFactory().getCvObjectDao( CvDatabase.class ).getByShortLabel( name );
                }
            }

            if (this.database == null){
                throw new DatasetException("The database " + mi + ": "+name+" is not known and it is a mandatory field.");
            }

            this.databaseId = columns[2];
        }
        else {
            // get the cvXRefQualifier instance
            if (mi != null && mi.length() > 0){
                this.qualifier = this.context.getDataContext().getDaoFactory().getCvObjectDao( CvXrefQualifier.class ).getByPsiMiRef( mi );
            }
            else {
                if (name.length() > 0){
                    this.qualifier = this.context.getDataContext().getDaoFactory().getCvObjectDao( CvXrefQualifier.class ).getByShortLabel( name );
                }
            }

            if (this.qualifier == null){
                throw new DatasetException("The cross reference qualifier " + mi + ": "+name+" is not known and it is a mandatory field.");
            }
        }
    }

    public CvDatabase getDatabase() {
        return database;
    }

    public void setDatabase(CvDatabase database) {
        this.database = database;
    }

    public CvXrefQualifier getQualifier() {
        return qualifier;
    }

    public void setQualifier(CvXrefQualifier qualifier) {
        this.qualifier = qualifier;
    }

    public String getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(String databaseId) {
        this.databaseId = databaseId;
    }
}
