package uk.ac.ebi.intact.dbupdate.dataset.selectors.protein;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.dataset.DatasetException;
import uk.ac.ebi.intact.model.CvAliasType;
import uk.ac.ebi.intact.model.ProteinImpl;

import javax.persistence.Query;
import java.io.IOException;
import java.util.*;

/**
 * An InteractorAliasSelector is a DatasetSelector which is collecting proteins in IntAct with a specific
 * alias (gene name, orf,...) and organism. Usually the list of aliases as well as the organisms and the dataset value are in a configuration file
 * but we can use the methods setListOfProteins, setDatasetValue and setPossibleTaxId if the data are not taken from a file.
 * The configuration file of this selector must have 3 columns : one for the name of the parameter (ex : dataset), one for the mi number of this parameter (ex : MI:0875)
 * and one for the value of the parameter (ex : Interactions of proteins with an established role in the presynapse.).
 * We must have a line containing the dataset value, another containing the list of organism taxIds (separated by semi-colon) which can
 * be empty and the other lines contain gene names, orfs and/or other interactor aliases (Ex of line : gene name    MI:0301 AMPH).
 * All the columns are tab separated.
 * You can add an extra line containing publication to exclude (column name = publication and the pubmed ids are seperated by semi-colon)).
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02-Jun-2010</pre>
 */

public class InteractorAliasSelector extends ProteinDatasetSelectorImpl{

    /**
     * The log for this class
     */
    private static final Log log = LogFactory.getLog( InteractorAliasSelector.class );

    /**
     * The map containing the type of the alias (ex : gene name) associated with a set of names (ex : AMPH, APBA1, APBA2, ...)
     */
    private Map<CvAliasType, Set<String>> listOfProteins = new HashMap<CvAliasType, Set<String>>();

    /**
     * Create a new InteractorAliasSelector with no dataset value and no intact context. These two variables must be initialised using the set methods
     */
    public InteractorAliasSelector(){
        super();
    }

    /**
     * Create a new InteractorAliasSelector with no dataset value and an intact context. The dataset must be initialised using the set method
     * @param context
     */
    public InteractorAliasSelector(IntactContext context){
        super(context);
    }

    /**
     * Clear the list of proteins, the list of possible taxIds, the list of publication excluded and the dataset value of this object
     */
    public void clearDatasetContent(){
        this.listOfProteins.clear();
        super.clearDatasetContent();
    }

    /**
     *
     * @return the map containing the list of aliases
     */
    public Map<CvAliasType, Set<String>> getListOfProteins() {
        return listOfProteins;
    }

    /**
     * set the map containing the list of aliases
     * @param listOfProteins
     */
    public void setListOfProteins(Map<CvAliasType, Set<String>> listOfProteins) {
        this.listOfProteins = listOfProteins;
    }

    /**
     * This method read the columns of a line in the file and initialises either the listOfPossibleTaxIds, listOfProteins or dataset value.
     * It reads the content of the file and load the list of proteins
     * @param columns : the columns of a line in a file
     * @throws uk.ac.ebi.intact.dbupdate.dataset.DatasetException : if the intact context is not set
     */
    protected void readSpecificContent(String [] columns) throws DatasetException {
        // the mi identifier of the alias type is the second column
        String mi = columns[1];
        // the name of the alias type is the first column
        String name = columns[0];

        // The alias type
        CvAliasType aliasType = null;

        // we need an Intact context
        if (this.context == null){
            throw new DatasetException("The intact context must be not null, otherwise the protein selector can't query the Intact database.");
        }

        if (columns [2].length() > 0){
            // get the aliastype instance
            if (mi != null && mi.length() > 0){
                aliasType = this.context.getDataContext().getDaoFactory().getCvObjectDao( CvAliasType.class ).getByPsiMiRef( mi );
            }
            else {
                if (name.length() > 0){
                    aliasType = this.context.getDataContext().getDaoFactory().getCvObjectDao( CvAliasType.class ).getByShortLabel( name );
                }
            }

            if (aliasType == null){
                throw new DatasetException("The alias type " + mi + ": "+name+" is not known and it is a mandatory field.");
            }

            // we have not already loaded this alias type, we add the new alias with its alias type
            if (!this.listOfProteins.containsKey(aliasType)){
                Set<String> values = new HashSet<String>();
                values.add(columns[2]);
                this.listOfProteins.put(aliasType, values);
            }
            // the alias type is already loaded, we add the new alias only
            else {
                Set<String> values = this.listOfProteins.get(aliasType);

                values.add(columns[2]);
            }
        }
    }

    /**
     * Load the list of proteins aliases contained in the file
     * @param file : the file containing the protein aliases
     * @throws uk.ac.ebi.intact.dbupdate.dataset.DatasetException : if the file is null
     */
    /* protected void extractDatasetFromFile(File file) throws DatasetException {

       BufferedReader in = null;

       if (file == null){
           throw new IllegalArgumentException("The file containing the dataset is null and we can't read the dataset. Please set a valid datasetFile of this object.");
       }
       try {

           in = new BufferedReader( new FileReader( file ) );

           String str;
           while ( ( str = in.readLine() ) != null ) {
               // remove possible "
               str = removeQuotes(str);

               // we skip the comments and empty lines
               if ( str.startsWith( "#" ) || str.length() == 0 ) {
                   continue;
               }

               // the line contains columns
               if (str.contains(separator)){
                   final String[] columns = StringUtils.splitPreserveAllTokens(str,separator);

                   // we need 3 columns
                   if (columns.length != 3){
                       throw new IllegalArgumentException("The file containing the dataset is malformed : we found "+columns.length+" columns instead of 3. We need a column for the type of data (dataset, organism (can be empty if no organism limitations), gene name, protein name), a column containing the MI " +
                               "identifier of this type of data (for instance dataset has a MI number MI:0875, gene name has a MI number MI:0301)");
                   }
                   else {
                       // remove possible quotes
                       for (String c : columns){
                           c = removeQuotes(c);
                       }
                       // if the line contains dataset information, we initialises the dataset value of this object
                       if (isDatasetLine(columns)){
                           // if the dataset value is not null, it is replaced with the new one but it can be an error in the file
                           if (this.datasetValue != null && columns[2].length() > 0){
                               log.warn("The dataset value for the file was " + this.datasetValue + " but is replaced with the new value found in the file " + columns[2]);
                               this.datasetValue = columns[2];
                           }
                           else if (columns[2].length() > 0){
                               this.datasetValue = columns[2];
                           }
                       }
                       // if the line contains organism requirements, we initialise the list of organisms taxids
                       else if (isOrganismLine(columns)){
                           // if list of organism taxIds is not empty, it is totally cleared before adding the new organism requirements, but it can be an error in the file
                           if (!this.listOfPossibleTaxId.isEmpty()){
                               log.warn("The possible organisms for the file were " + this.listOfPossibleTaxId + " but are replaced with the new value(s) found in the file " + columns[2]);
                               this.listOfPossibleTaxId.clear();
                           }
                           extractListOfIdsFrom(columns[2], this.listOfPossibleTaxId);
                       }
                       // if the line contains publication exclusions, we initialise the list of publications to exclude
                       else if (isPublicationLine(columns)){
                           // if list of organism taxIds is not empty, it is totally cleared before adding the new organism requirements, but it can be an error in the file
                           if (!this.listOfExcludedPublications.isEmpty()){
                               log.warn("The publications to exclude for the file were " + this.listOfExcludedPublications + " but are replaced with the new value(s) found in the file " + columns[2]);
                               this.listOfExcludedPublications.clear();
                           }
                           extractListOfIdsFrom(columns[2], this.listOfExcludedPublications);
                       }
                       // we don't have neither dataset information, nor organism requirements, it is a protein alias to load
                       else {
                           addNewProtein(columns);
                       }
                   }
               }
               else {
                   throw new DatasetException("The file containing the dataset is malformed. We need a column for the type of data (dataset, organism (can be empty if no organism limitations), gene name, protein name), a column containing the MI " +
                           "identifier of this type of data (for instance dataset has a MI number MI:0875, gene name has a MI number MI:0301)");
               }
           }
           in.close();
       } catch (FileNotFoundException e) {
           throw new DatasetException("The file " + file.getAbsolutePath() + " has not been found.", e);
       } catch (IOException e) {
           throw new DatasetException("The file " + file.getAbsolutePath() + " couldn't be read.", e);
       }
   } */

    /**
     *
     * @param type : alias type
     * @param name : alias name
     * @return the list of intact proteins matching the alias name for this alias type and with an organism respecting the restrictions
     */
    /*private List<String> getProteinAccessionsContainingAlias (CvAliasType type, String name){

        // get the intact datacontext and factory
        final DataContext dataContext = this.context.getDataContext();
        final DaoFactory daoFactory = dataContext.getDaoFactory();

        // we want all the interactor associated with this alias name and this alias type
        String interactorGeneQuery = "select prot.ac from InteractorAlias ia join ia.parent as prot join ia.cvAliasType as alias " +
                "where upper(ia.name) = upper(:name) and alias = :alias and prot.objClass = :objclass";

        // we add the organism restrictions
        String finalQuery = addOrganismSelection(interactorGeneQuery);

        // create the query
        final Query query = daoFactory.getEntityManager().createQuery(finalQuery);

        // set the query parameters
        query.setParameter("name", name);
        query.setParameter("alias", type);
        query.setParameter("objclass", ProteinImpl.class.getName());

        // the list of results
        final List<String> interactorAcs = query.getResultList();

        return interactorAcs;
    }*/

    /*private void addAllExperimentsFromSamePublication(List<Experiment> experiments){
        List<Experiment> otherExperiment = new ArrayList<Experiment>();
        ExperimentDao expDao = this.context.getDataContext().getDaoFactory().getExperimentDao();

        for (Experiment e : experiments){
            if (e.getPublication() != null){
                if (e.getPublication().getPublicationId() != null){
                    otherExperiment.addAll(expDao.getByPubId(e.getPublication().getPublicationId()));
                }
            }
        }

        experiments.addAll(otherExperiment);
    }*/

    /*private List<Experiment> getExperimentWithSamePubIdFrom(List<Experiment> experiments, String pubId){
        if (pubId == null){
            return null;
        }

        List<Experiment> expWithSamePubId = new ArrayList<Experiment>();

        for (Experiment e : experiments){
            if (e.getPublication() != null){
                if (pubId.equalsIgnoreCase(e.getPublication().getPublicationId())){
                    expWithSamePubId.add(e);
                }
            }
        }
        return expWithSamePubId;
    }*/

    /*private void removeExperimentsWithTooManyInteractions(List<Experiment> experiments){
        List<Experiment> experimentsToRemove = new ArrayList<Experiment>();

        for (Experiment exp : experiments){
            if (exp.getInteractions().size() >= maxNumberOfInteractions){
                if (exp.getPublication() == null){
                    experimentsToRemove.add(exp);
                }
                else {
                    experimentsToRemove.addAll(getExperimentWithSamePubIdFrom(experiments, exp.getPublication().getPublicationId()));
                }
            }
        }

        experiments.removeAll(experimentsToRemove);
    }*/

    /*private boolean hasExactDatasetAnnotation(Experiment e){
        if (e == null){
            return false;
        }
        Collection<Annotation> annotations = e.getAnnotations();

        for (Annotation a : annotations){
            CvTopic topic = a.getCvTopic();
            if (CvTopic.DATASET_MI_REF.equalsIgnoreCase(topic.getIdentifier())){
                if (a.getAnnotationText().equalsIgnoreCase(this.datasetValue)){
                    return true;
                }
            }
        }
        return false;
    }*/

    /**
     *
     * @return the Set of protein accessions matching ont of the aliases in the list and respecting the organism restrictions.
     * @throws uk.ac.ebi.intact.dbupdate.dataset.DatasetException
     */
    public Set<String> getSelectionOfProteinAccessionsInIntact() throws DatasetException {
        // we need an Intact context to query Intact
        if (this.context == null){
            throw new DatasetException("The intact context must be not null, otherwise the protein selector can't query the Intact database.");
        }

        log.info("Collect proteins in Intact...");

        // The map containing the protein aliases should be initialised before
        if (this.listOfProteins.isEmpty()){
            throw new IllegalArgumentException("The list of protein aliases has not been initialised.");
        }
        // The dataset value associated with the list of protein aliases should be initialised before
        if (this.datasetValue == null){
            throw new IllegalArgumentException("The dataset value has not been initialised.");
        }

        Set<String> proteinAccessions = new HashSet<String>();

        // get the intact datacontext and factory
        final DataContext dataContext = this.context.getDataContext();
        final DaoFactory daoFactory = dataContext.getDaoFactory();

        StringBuffer interactorGeneQuery = new StringBuffer(1064);

        // we want all the interactor associated with this alias name and this alias type
        interactorGeneQuery.append("select prot.ac from InteractorAlias ia join ia.parent as prot join ia.cvAliasType as alias " +
                "where (");

        try {

            // For each alias type in the file
            for (Map.Entry<CvAliasType, Set<String>> entry : this.listOfProteins.entrySet()){
                // For each alias name associated with this alias type
                for (String name : entry.getValue()){

                    interactorGeneQuery.append(" (upper(name) = upper('"+name+"') and alias.ac = '"+entry.getKey().getAc()+"') or");
                }
            }

            interactorGeneQuery.delete(interactorGeneQuery.lastIndexOf(" or"), interactorGeneQuery.length());
            interactorGeneQuery.append(") and prot.objClass = :objclass");

            // we add the organism restrictions
            String finalQuery = addOrganismSelection(interactorGeneQuery.toString());

            // create the query
            final Query query = daoFactory.getEntityManager().createQuery(finalQuery);

            query.setParameter("objclass", ProteinImpl.class.getName());

            // the list of results
            final List<String> interactorAcs = query.getResultList();

            proteinAccessions.addAll(interactorAcs);

            // write protein report if file enabled
            writeProteinReport(proteinAccessions);

        } catch (IOException e) {
            throw new DatasetException("We can't write the results of the protein selection.", e);
        }

        return proteinAccessions;

    }
}
