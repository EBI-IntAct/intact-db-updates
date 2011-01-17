package uk.ac.ebi.intact.dbupdate.prot.actions.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessorConfig;
import uk.ac.ebi.intact.dbupdate.prot.UpdateError;
import uk.ac.ebi.intact.dbupdate.prot.actions.UniprotProteinMapper;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinRemappingEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateErrorEvent;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.XrefUtils;
import uk.ac.ebi.intact.protein.mapping.model.contexts.UpdateContext;
import uk.ac.ebi.intact.protein.mapping.strategies.StrategyForProteinUpdate;
import uk.ac.ebi.intact.protein.mapping.strategies.exceptions.StrategyException;
import uk.ac.ebi.intact.protein.mapping.update.ProteinUpdateException;
import uk.ac.ebi.intact.uniprot.service.IdentifierChecker;
import uk.ac.ebi.intact.update.model.proteinmapping.results.IdentificationResults;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Remap dead proteins and 'no-uniprot-update' proteins to a single uniprot entry :
 * If successful :
 * - remove no-uniprot-update
 * - remove caution : sequence has become obsolete
 * - add a cross reference uniprot identity
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>05-Jan-2011</pre>
 */

public class UniprotProteinMapperImpl implements UniprotProteinMapper{

    /**
     * Sets up a logger for that class.
     */
    public static final Log log = LogFactory.getLog( UniprotProteinMapperImpl.class );

    /**
     * the strategy used to update the proteins
     */
    private StrategyForProteinUpdate strategy;

    /**
     * the context of the protein to update
     */
    private UpdateContext context;

    /**
     * create a new ProteinUpdate manager.The strategy for update doesn't take into account the isoforms and keep the canonical sequence.
     */
    public UniprotProteinMapperImpl(){
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();

        this.strategy = new StrategyForProteinUpdate();
        this.strategy.enableIsoforms(false);
        this.strategy.setBasicBlastProcessRequired(config.isBlastEnabled());
        this.context = new UpdateContext();
    }

    /**
     *
     * @param protein
     * @return True if the protein is allowed to be remapped to uniprot (meaning it is a protein without uniprot identities or other uniprot cross references
     * other than uniprot-removed-ac)
     */
    private boolean isProteinMappingAllowed(Protein protein){

        if (protein != null){
            Collection<InteractorXref> refs = protein.getXrefs();

            for (InteractorXref ref : refs){
                // we can remapp excepted if there are uniprot xrefs others than uniprot-removed-ac
                if (CvDatabase.UNIPROT_MI_REF.equals(ref.getCvDatabase().getIdentifier())){
                    if (ref.getCvXrefQualifier() != null){
                        // if uniprot xref different from uniprot removed ac : doesn't remap
                        if (!CvXrefQualifier.UNIPROT_REMOVED_AC.equalsIgnoreCase(ref.getCvXrefQualifier().getShortLabel())){
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     *
     * @param protein
     * @return true if the protein has uniprot cross references other than uniprot identity and uniprot removed-ac
     */
    private boolean isProteinMappingPossibleButNotAllowed(Protein protein){

        if (protein != null){
            Collection<InteractorXref> refs = protein.getXrefs();

            for (InteractorXref ref : refs){
                // we can remapp excepted if there are uniprot xrefs others than uniprot-removed-ac
                if (CvDatabase.UNIPROT_MI_REF.equals(ref.getCvDatabase().getIdentifier())){
                    if (ref.getCvXrefQualifier() != null){
                        // if uniprot xref different from uniprot removed ac : doesn't remap
                        if (!CvXrefQualifier.UNIPROT_REMOVED_AC.equalsIgnoreCase(ref.getCvXrefQualifier().getShortLabel()) && !CvXrefQualifier.IDENTITY_MI_REF.equals(ref.getCvXrefQualifier().getIdentifier())){
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        return false;
    }

    /**
     * Remap proteins to a single uniprot entry if the protein is allowed to be remapped to. 
     * @param evt : evt with the protein to remap
     * @return true if the protein has been remapped, false otherwise
     */
    public boolean processProteinRemappingFor(ProteinEvent evt){
        Protein protein = evt.getProtein();
        if (protein != null){
            if (isProteinMappingAllowed(protein)){

                // enable the update
                this.strategy.setUpdateEnabled(true);

                this.context.clean();
                String accession = protein.getAc();
                String shortLabel = protein.getShortLabel();
                log.info("Protein AC = " + accession + " shortLabel = " + shortLabel);

                Collection<InteractorXref> refs = protein.getXrefs();
                Collection<Annotation> annotations = protein.getAnnotations();
                String sequence = protein.getSequence();
                BioSource organism = protein.getBioSource();

                // context
                context.setSequence(sequence);
                context.setOrganism(organism);
                context.setIntactAccession(accession);
                addIdentityCrossreferencesToContext(refs, context);

                // result
                IdentificationResults result = null;
                try {
                    result = this.strategy.identifyProtein(context);

                    DaoFactory factory = evt.getDataContext().getDaoFactory();
                    // update
                    if (result != null && result.getFinalUniprotId() != null){
                        Annotation a = collectNo_Uniprot_UpdateAnnotation(annotations);

                        if (a != null){
                            log.info("annotation no_uniprot_update removed from the annotations of " + accession);
                            protein.removeAnnotation(a);
                            factory.getAnnotationDao().delete(a);
                        }

                        Annotation a2 = collectObsoleteAnnotation(annotations);

                        if (a2 != null){
                            log.info("caution removed from the annotations of " + accession);
                            protein.removeAnnotation(a2);
                            factory.getAnnotationDao().delete(a2);
                        }

                        Annotation a3 = collectFeatureObsoleteAnnotation(annotations);

                        if (a3 != null){
                            log.info("caution removed from the annotations of " + accession);
                            protein.removeAnnotation(a3);
                            factory.getAnnotationDao().delete(a3);
                        }
                        
                        addUniprotCrossReferenceTo((ProteinImpl) protein, result.getFinalUniprotId(), factory);

                        if (!IdentifierChecker.isSpliceVariantId(result.getFinalUniprotId()) && !IdentifierChecker.isFeatureChainId(result.getFinalUniprotId())){
                            removeParentCrossReferenceTo((ProteinImpl) protein, evt.getDataContext(), (ProteinUpdateProcessor) evt.getSource());
                        }

                        factory.getProteinDao().update( (ProteinImpl) protein );

                        return true;
                    }
                } catch (StrategyException e) {
                    log.error("Impossible to remap the protein " + accession, e);

                    if (evt.getSource() instanceof ProteinUpdateProcessor){
                        ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                        processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), "Impossible to remap the protein " + accession + " " + e.getMessage(), UpdateError.impossible_protein_remapping, protein));
                    }
                }

                if (evt.getSource() instanceof ProteinUpdateProcessor){
                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                    processor.fireOnProteinToBeRemapped(new ProteinRemappingEvent(processor, evt.getDataContext(), context, result, evt.getMessage()));
                }
            }
            else if (isProteinMappingPossibleButNotAllowed(protein)){

                // enable the update
                this.strategy.setUpdateEnabled(false);

                this.context.clean();
                String accession = protein.getAc();
                String shortLabel = protein.getShortLabel();
                log.info("Protein AC = " + accession + " shortLabel = " + shortLabel);

                Collection<InteractorXref> refs = protein.getXrefs();
                Collection<Annotation> annotations = protein.getAnnotations();
                String sequence = protein.getSequence();
                BioSource organism = protein.getBioSource();

                // context
                context.setSequence(sequence);
                context.setOrganism(organism);
                context.setIntactAccession(accession);
                addIdentityCrossreferencesToContext(refs, context);

                IdentificationResults result = null;
                try {
                    result = this.strategy.identifyProtein(context);

                } catch (StrategyException e) {
                    log.error("Impossible to remap the protein " + accession, e);

                    if (evt.getSource() instanceof ProteinUpdateProcessor){
                        ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                        processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), "Impossible to remap the protein " + accession + " " + e.getMessage(), UpdateError.impossible_protein_remapping, protein));
                    }
                }

                if (evt.getSource() instanceof ProteinUpdateProcessor){
                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                    processor.fireOnProteinToBeRemapped(new ProteinRemappingEvent(processor, evt.getDataContext(), context, result, evt.getMessage()));
                }
            }
            else {
                if (evt.getSource() instanceof ProteinUpdateProcessor){
                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                    processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), "Impossible to remap the protein " + protein.getAc() + " because a uniprot identity already exists for this protein.", UpdateError.impossible_protein_remapping, protein));
                }
            }
        }

        return false;
    }

    /*protected Query getProteinsWithoutUniprotXrefs(DataContext dataContext){
        // get all the intact entries without any uniprot cross reference or with uniprot cross reference with a qualifier different from 'identity' and which can only be uniprot-removed-ac
        final DaoFactory daoFactory = dataContext.getDaoFactory();
        final Query query = daoFactory.getEntityManager().createQuery("select distinct p from InteractorImpl p "+
                "left join p.sequenceChunks as seq " +
                "left join p.xrefs as xrefs " +
                "left join p.annotations as annotations " +
                "where p.objClass = 'uk.ac.ebi.intact.model.ProteinImpl' "+
                "and p not in ( "+
                "select p2 "+
                "from InteractorImpl p2 join p2.xrefs as xrefs "+
                "where p2.objClass = 'uk.ac.ebi.intact.model.ProteinImpl' "+
                "and xrefs.cvDatabase.ac = 'EBI-31' " +
                "and xrefs.cvXrefQualifier.shortLabel <> 'uniprot-removed-ac' )");

        return query;
    }

    protected Query getProteinsWithUniprotXrefsWithoutIdentity(DataContext dataContext){
        // get all the intact entries without any uniprot cross reference or with uniprot cross reference with a qualifier different from 'identity' and which can only be uniprot-removed-ac
        final DaoFactory daoFactory = dataContext.getDaoFactory();
        final Query query = daoFactory.getEntityManager().createQuery("select distinct p from InteractorImpl p "+
                "left join p.sequenceChunks as seq " +
                "left join p.xrefs as xrefs " +
                "left join p.annotations as annotations " +
                "where p.objClass = 'uk.ac.ebi.intact.model.ProteinImpl' "+
                "and p not in ( "+
                "select p2 "+
                "from InteractorImpl p2 join p2.xrefs as xrefs "+
                "where p2.objClass = 'uk.ac.ebi.intact.model.ProteinImpl' "+
                "and xrefs.cvDatabase.ac = 'EBI-31' " +
                "and xrefs.cvXrefQualifier.shortLabel = 'identity') " +
                "and p in ( " +
                "select p2 " +
                "from InteractorImpl p2 join p2.xrefs as xrefs " +
                "where p2.objClass = 'uk.ac.ebi.intact.model.ProteinImpl' " +
                "and xrefs.cvDatabase.ac = 'EBI-31' " +
                "and xrefs.cvXrefQualifier.shortLabel <> 'uniprot-removed-ac')");

        return query;
    }*/

    /**
     * This method query IntAct to get the list of protein to update and for each one create an updateContext
     * Write the results of the protein update process
     * @return the list of UpdateContext created from the protein to update
     * @throws uk.ac.ebi.intact.protein.mapping.update.ProteinUpdateException
     * @throws uk.ac.ebi.intact.protein.mapping.strategies.exceptions.StrategyException
     */
    /*public void writeResultsOfProteinUpdate() throws ProteinUpdateException, StrategyException {
        // disable the update
        this.strategy.setUpdateEnabled(false);

        try {
            IntactContext intactContext = IntactContext.getCurrentInstance();

            File file = new File("updateReport_"+ Calendar.getInstance().getTime().getTime() +".txt");
            Writer writer = new FileWriter(file);

            // set the intact data context
            final DataContext dataContext = intactContext.getDataContext();
            TransactionStatus transactionStatus = dataContext.beginTransaction();

            // get all the intact entries without any uniprot cross reference or with uniprot cross reference with a qualifier different from 'identity' and which can only be uniprot-removed-ac
            final Query query = getProteinsWithoutUniprotXrefs(dataContext);

            proteinToUpdate = query.getResultList();
            log.info(proteinToUpdate.size());

            for (ProteinImpl prot : proteinToUpdate){
                this.context.clean();

                String accession = prot.getAc();
                Collection<InteractorXref> refs = prot.getXrefs();
                String sequence = prot.getSequence();
                BioSource organism = prot.getBioSource();

                context.setSequence(sequence);
                context.setOrganism(organism);
                context.setIntactAccession(accession);
                addIdentityCrossreferencesToContext(refs, context);

                log.info("protAc = " + accession);
                IdentificationResults result = this.strategy.identifyProtein(context);
                writeResultReports(accession, result, writer);

            }
            dataContext.commitTransaction(transactionStatus);
            writer.close();
        } catch (IntactTransactionException e) {
            throw new ProteinUpdateException(e);
        } catch (IOException e) {
            throw new ProteinUpdateException(e);
        }
    }*/

    /**
     *
     * @param qualifier : the qualifier of the cross reference
     * @return true if the qualifier is 'identity'
     */
    private boolean isIdentityCrossReference(CvXrefQualifier qualifier){
        if (qualifier.getIdentifier() != null){
            if (qualifier.getIdentifier().equals(CvXrefQualifier.IDENTITY_MI_REF)){
                return true;
            }

        }
        else {
            if (qualifier.getShortLabel().equals(CvXrefQualifier.IDENTITY)){
                return true;
            }
        }
        return false;
    }

    /**
     * Add all the cross references with qualifier 'identity' to the list of identifiers of the protein (intact cross references are ignored)
     * @param refs : the refs of the protein
     * @param context : the context of the protein
     */
    private void addIdentityCrossreferencesToContext(Collection<InteractorXref> refs, UpdateContext context){
        for (InteractorXref ref : refs){
            if (ref.getPrimaryId() != null){
                if (ref.getCvXrefQualifier() != null){
                    CvXrefQualifier qualifier = ref.getCvXrefQualifier();

                    if (isIdentityCrossReference(qualifier)){
                        CvDatabase database = ref.getCvDatabase();
                        if (database != null){
                            if (database.getIdentifier() != null && !CvDatabase.INTACT_MI_REF.equals(database.getIdentifier())){
                                context.addIdentifier(database.getIdentifier(), ref.getPrimaryId());
                            }
                            else if (database.getShortLabel() != null && !CvDatabase.INTACT.equals(database.getShortLabel())) {
                                context.addIdentifier(database.getShortLabel(), ref.getPrimaryId());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * get the annotation 'no-uniprot-update' if there is one in the list of annotations
     * @param annotations : the annotations of the protein
     * @return the annotation 'no-uniprot-update' if there is one in the list of annotations, null otherwise
     */
    private Annotation collectNo_Uniprot_UpdateAnnotation(Collection<Annotation> annotations){
        for (Annotation a : annotations){
            if (a.getCvTopic() != null){
                CvTopic topic = a.getCvTopic();

                if (topic.getShortLabel() != null){
                    if (topic.getShortLabel().equals(CvTopic.NON_UNIPROT)){
                        return a;
                    }
                }
            }
        }
        return null;
    }

    private Annotation collectObsoleteAnnotation(Collection<Annotation> annotations){
        for (Annotation a : annotations){
            if (a.getCvTopic() != null){
                CvTopic topic = a.getCvTopic();

                if (a.getAnnotationText() != null){
                    if (a.getAnnotationText().equalsIgnoreCase(DeadUniprotProteinFixerImpl.CAUTION_OBSOLETE)){
                        if (topic.getIdentifier() != null){
                            if (topic.getIdentifier().equals(CvTopic.CAUTION_MI_REF)){
                                return a;
                            }
                        }
                        else if (topic.getShortLabel() != null){
                            if (topic.getShortLabel().equals(CvTopic.CAUTION)){
                                return a;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private Annotation collectFeatureObsoleteAnnotation(Collection<Annotation> annotations){
        for (Annotation a : annotations){
            if (a.getCvTopic() != null){
                CvTopic topic = a.getCvTopic();

                if (a.getAnnotationText() != null){
                    if (a.getAnnotationText().equalsIgnoreCase(OutOfDateParticipantFixerImpl.FEATURE_OBSOLETE)){
                        if (topic.getIdentifier() != null){
                            if (topic.getIdentifier().equals(CvTopic.CAUTION_MI_REF)){
                                return a;
                            }
                        }
                        else if (topic.getShortLabel() != null){
                            if (topic.getShortLabel().equals(CvTopic.CAUTION)){
                                return a;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Create a new InteractorXref for the protein
     * @param uniprotAc : the uniprot accession
     * @return the InteractorXref with the uniprot ac and qualifier identity
     */
    private InteractorXref createIdentityInteractorXrefForUniprotAc(String uniprotAc, Protein parent, DaoFactory factory){

        if (uniprotAc == null){
            return null;
        }

        CvDatabase uniprot = factory.getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.UNIPROT_MI_REF);

        if (uniprot == null){
            uniprot = CvObjectUtils.createCvObject(parent.getOwner(), CvDatabase.class, CvDatabase.UNIPROT_MI_REF, CvDatabase.UNIPROT);
            factory.getCvObjectDao(CvDatabase.class).persist(uniprot);
        }

        CvXrefQualifier identity = factory.getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.IDENTITY_MI_REF);

        if (identity == null){
            identity = CvObjectUtils.createCvObject(parent.getOwner(), CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF, CvXrefQualifier.IDENTITY);
            factory.getCvObjectDao(CvXrefQualifier.class).persist(identity);
        }

        InteractorXref xRef = new InteractorXref(parent.getOwner(), uniprot, uniprotAc, identity);

        return xRef;
    }

    /**
     * add a new uniprot cross reference with qualifier identity to the list of cross references of the protein
     * @param prot : the protein
     * @param uniprotAc : the uniprot accession
     * @param factory : the Intact factory (not used)
     */
    private void addUniprotCrossReferenceTo(ProteinImpl prot, String uniprotAc, DaoFactory factory){
        InteractorXref ref = createIdentityInteractorXrefForUniprotAc(uniprotAc, prot, factory);

        if (ref != null){
            log.info("cross reference to uniprot "+ uniprotAc +" added to the cross references of " + prot.getAc());
            factory.getXrefDao(InteractorXref.class).persist( ref );
            prot.addXref(ref);
        }
    }

    private void removeParentCrossReferenceTo(ProteinImpl prot, DataContext context, ProteinUpdateProcessor processor){

        Collection<InteractorXref> intRef = new ArrayList(prot.getXrefs());
        for (InteractorXref ref : intRef){
            if (ref.getCvDatabase().getIdentifier().equals(CvDatabase.INTACT_MI_REF)){
                if (ref.getCvXrefQualifier() != null){
                    if (ref.getCvXrefQualifier().getIdentifier().equals(CvXrefQualifier.ISOFORM_PARENT_MI_REF) || ref.getCvXrefQualifier().getIdentifier().equals(CvXrefQualifier.CHAIN_PARENT_MI_REF)){
                        ProteinTools.deleteInteractorXRef(prot, context, ref, processor);
                    }
                }
            }
        }
    }

    /**
     * Update the proteins with no uniprot cross references and the proteins with uniprot cross references set to 'uniprot-removed-ac'
     * @throws ProteinUpdateException
     */
    /*public void updateProteins() throws ProteinUpdateException {
        IntactContext intactContext = IntactContext.getCurrentInstance();

        // enable the update
        this.strategy.setUpdateEnabled(true);

        // get the data context
        final DataContext dataContext = intactContext.getDataContext();
        TransactionStatus transactionStatus = dataContext.beginTransaction();
        try {

            // create a new file where the results are stored in
            File file = new File("updateReport_"+ Calendar.getInstance().getTime().getTime() +".txt");

            Writer writer = new FileWriter(file);

            final DaoFactory daoFactory = dataContext.getDaoFactory();
            final Query query = getProteinsWithoutUniprotXrefs(dataContext);

            proteinToUpdate = query.getResultList();
            log.info(proteinToUpdate.size());

            ArrayList<String> accessionsToUpdate = new ArrayList<String>();

            for (ProteinImpl prot : proteinToUpdate){

                this.context.clean();
                String accession = prot.getAc();
                String shortLabel = prot.getShortLabel();
                log.info("Protein AC = " + accession + " shortLabel = " + shortLabel);

                Collection<InteractorXref> refs = prot.getXrefs();
                Collection<Annotation> annotations = prot.getAnnotations();
                String sequence = prot.getSequence();
                BioSource organism = prot.getBioSource();

                // context
                context.setSequence(sequence);
                context.setOrganism(organism);
                context.setIntactAccession(accession);
                addIdentityCrossreferencesToContext(refs, context);

                // result
                IdentificationResults result = this.strategy.identifyProtein(context);
                writeResultReports(accession, result, writer);

                // update
                if (result != null && result.getFinalUniprotId() != null){
                    Annotation a = collectNo_Uniprot_UpdateAnnotation(annotations);

                    if (a != null){
                        log.info("annotation no_uniprot_update removed from the annotations of " + accession);
                        prot.removeAnnotation(a);
                        daoFactory.getAnnotationDao().delete(a);
                    }

                    Annotation a2 = collectObsoleteAnnotation(annotations);

                    if (a2 != null){
                        log.info("caution removed from the annotations of " + accession);
                        prot.removeAnnotation(a2);
                        daoFactory.getAnnotationDao().delete(a2);
                    }
                    addUniprotCrossReferenceTo(prot, result.getFinalUniprotId(), daoFactory);
                    daoFactory.getProteinDao().update( prot );
                    accessionsToUpdate.add(accession);
                }
            }

            // commit the changes
            log.info("commit the change in the database.");
            log.info(accessionsToUpdate.size() + " proteins have been modified.");
            dataContext.commitTransaction(transactionStatus);
            writer.close();

            // update the database
            log.info("Processing the update of the proteins in Intact");
            //UpdateReportHandler reportHandler = new FileReportHandler(new File("target"));
            //ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig(reportHandler);

            //ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);
            //ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor();
            //protUpdateProcessor.updateByACs(accessionsToUpdate);

        } catch (IntactTransactionException e) {
            throw new ProteinUpdateException(e);
        } catch (StrategyException e) {
            throw new ProteinUpdateException("There is a problem when executing the protein update strategy. Check the protein contexts.", e);
        } catch (IOException e) {
            throw new ProteinUpdateException("We can't write the results in a file.", e);
        } catch (Exception e){
            throw new ProteinUpdateException( e);
        }
    }*/

    /**
     * Write a report for the identification of the proteins without any uniprot cross references set to identity but another uniprot cross reference
     * @throws ProteinUpdateException
     */
    /*public void writeUpdateReportForProteinsWithUniprotCrossReferences() throws ProteinUpdateException {
        IntactContext intactContext = IntactContext.getCurrentInstance();

        // disable the update
        this.strategy.setUpdateEnabled(false);

        // create the data context
        final DataContext dataContext = intactContext.getDataContext();
        TransactionStatus transactionStatus = dataContext.beginTransaction();
        try {

            // create the file where to write the report
            File file = new File("updateReportForProteinWithUniprotCrossReferences_"+Calendar.getInstance().getTime().getTime()+".txt");
            Writer writer = new FileWriter(file);

            final Query query = getProteinsWithUniprotXrefsWithoutIdentity(dataContext);

            proteinToUpdate = query.getResultList();
            log.info(proteinToUpdate.size());

            ArrayList<String> accessionsToUpdate = new ArrayList<String>();

            for (ProteinImpl prot : proteinToUpdate){
                this.context.clean();
                String accession = prot.getAc();
                String shortLabel = prot.getShortLabel();
                log.info("Protein AC = " + accession + " shortLabel = " + shortLabel);

                Collection<InteractorXref> refs = prot.getXrefs();
                String sequence = prot.getSequence();
                BioSource organism = prot.getBioSource();

                // context
                context.setSequence(sequence);
                context.setOrganism(organism);
                context.setIntactAccession(accession);
                addIdentityCrossreferencesToContext(refs, context);

                // result
                IdentificationResults result = this.strategy.identifyProtein(context);
                writeResultReports(accession, result, writer);

                // update
                if (result != null && result.getFinalUniprotId() != null){
                    accessionsToUpdate.add(accession);
                }
            }
            log.info(accessionsToUpdate.size() + " proteins have been identified and could be updated.");
            dataContext.commitTransaction(transactionStatus);
            writer.close();

        } catch (IntactTransactionException e) {
            throw new ProteinUpdateException(e);
        } catch (StrategyException e) {
            throw new ProteinUpdateException("There is a problem when executing the protein update strategy. Check the protein contexts.", e);
        } catch (IOException e) {
            throw new ProteinUpdateException("We can't write the results in a file.", e);
        } catch (Exception e){
            throw new ProteinUpdateException( e);
        }
    }*/

    /**
     * write the results in a file
     * @throws ProteinUpdateException
     */
    /*private void writeResultReports(String protAc, IdentificationResults result, Writer writer) throws ProteinUpdateException {
        try {
            writer.write("************************" + protAc + "************************************ \n");

            writer.write("Uniprot accession found : " + result.getFinalUniprotId() + "\n");
            for (ActionReport report : result.getListOfActions()){
                writer.write(report.getName().toString() + " : " + report.getStatus().getLabel() + ", " + report.getStatus().getDescription() + "\n");

                for (String warn : report.getWarnings()){
                    writer.write(warn + "\n");
                }

                for (String ac : report.getPossibleAccessions()){
                    writer.write("possible accession : " + ac + "\n");
                }

                if (report instanceof PICRReport){
                    PICRReport picr = (PICRReport) report;
                    writer.write("Is a Swissprot entry : " + picr.isASwissprotEntry() + "\n");

                    for (PICRCrossReferences xrefs : picr.getCrossReferences()){
                        writer.write(xrefs.getDatabase() + " cross reference : " + xrefs.getAccessions() + "\n");
                    }
                }
                else if (report instanceof BlastReport){
                    BlastReport blast = (BlastReport) report;

                    for (BlastResults prot : blast.getBlastMatchingProteins()){
                        writer.write("BLAST Protein " + prot.getAccession() + " : identity = " + prot.getIdentity() + "\n");
                        writer.write("Query start = " + prot.getStartQuery() + ", end = " + prot.getEndQuery() + "\n");
                        writer.write("Match start = " + prot.getStartMatch() + ", end = " + prot.getEndMatch() + "\n");
                    }
                }
            }
            writer.flush();
        } catch (IOException e) {
            throw new ProteinUpdateException("We can't write the results of the protein " + protAc, e);
        }
    }*/
}
