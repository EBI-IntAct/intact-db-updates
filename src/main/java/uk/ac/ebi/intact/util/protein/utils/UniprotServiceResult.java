/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.protein.utils;

import uk.ac.ebi.intact.model.Protein;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * TODO comment this
 *
 * @author Catherine Leroy (cleroy@ebi.ac.uk)
 * @version $Id$
 * @since TODO
 */
public class UniprotServiceResult {

    public static final String MORE_THEN_1_PROT_MATCHING_UNIPROT_SECONDARY_AC_ERROR_TYPE = "More then one IntAct protein matching "
            + "the uniprot primaryAc.";
    public static final String MORE_THEN_1_PROT_MATCHING_UNIPROT_PRIMARY_AC_ERROR_TYPE = "More then one IntAct protein matching " +
            "the uniprot secondaryAc.";
    public static final String UNEXPECTED_NUMBER_OF_INTACT_PROT_FOUND_ERROR_TYPE = "An unexpected number of proteins was found in " +
            "IntAct searching by uniprot primaryAc and uniprot secondaryAc(s).";
    public static final String PROTEIN_FOUND_IN_INTACT_BUT_NOT_IN_UNIPROT_ERROR_TYPE = "Protein found in IntAct but not in Uniprot.";
    public static final String PROTEIN_NOT_IN_INTACT_NOT_IN_UNIPROT_ERROR_TYPE = "Protein not found in IntAct and not found" +
            " in Uniprot.";
    public static final String SEVERAL_PROT_BELONGING_TO_SAME_ORGA_ERROR_TYPE = "More then one protein found in Uniprot all " +
            "belonging to the same organism.";
    public static final String SEVERAL_PROT_BELONGING_TO_DIFFERENT_ORGA_ERROR_TYPE = "More then one protein found in Uniprot " +
                "belonging to the different organisms.";
    public static final String SPLICE_VARIANT_IN_INTACT_BUT_NOT_IN_UNIPROT = "Protein being a splice variant in IntAct " +
            "but not in Uniprot and being part of an interaction.";
    public static final String BIOSOURCE_MISMATCH = "The bioSource of the IntAct protein, is not the same then the " +
            "bioSource of the uniprotProtein.";
    public static final String SPLICE_VARIANT_WITH_MULTIPLE_IDENTITY = "Splice variants with multiple identity.";
    public static final String SPLICE_VARIANT_WITH_NO_IDENTITY = "Splice variant with no identity.";
    public static final String UNEXPECTED_EXCEPTION_ERROR_TYPE = "Unexpected exception";


    /**
     * A collection of retrieved proteins.
     */
    Collection<Protein> proteins = new ArrayList<Protein>();
    /**
     * A collection of Exception that occured.
     */
    private Collection<Exception> exceptions = new ArrayList<Exception>();
    /**
     * A collection of information messages (ex : searching for P12345, filtering on taxid 6409...).
     */
    private Collection<String> messages = new ArrayList<String>();
    /**
     * A collection of errors (ex : Could not update P12345, more than one protein found in IntAct for the uniprot
     * primary ac P12345).
     */
    private Map<String,String> errors = new HashMap<String, String>();

    /**
     * The query sent to the UniprotService for protein update(ex : P12345).
     */
    private String querySentToService = new String();

    /**
     * Constructor put private so that when you create a UniprotServiceResult you have at least to give the query sent
     * to the UniprotService for protein update.
     */
    private UniprotServiceResult() {
    }

    /**
     * Public constructor.
     * @param querySentToService the query sent to the UniprotService to update a protein (ex : P12345).
       If querySentToService is null, it will send a NullPointerException.
     */
    public UniprotServiceResult(String querySentToService) {
        if(querySentToService == null){
            throw new IllegalArgumentException("querySentToService parameter can not be null");
        }
        this.querySentToService = querySentToService;
    }

    public void addException(Exception e){
        if(e == null){
            throw new IllegalArgumentException( " e should not be null");
        }
        exceptions.add(e);
    }

    public Collection<Exception> getExceptions(){
        return exceptions;
    }

    /**
     * Add a message to the messages collection.
     * @param message
     * If message is null, it will send a NullPointerException.
     */
    public void addMessage(String message){
        if(message == null){
            throw new IllegalArgumentException("message argument can not be null");
        }
        messages.add(message);
    }

    /**
     * Get the messages collection.
     * @return
     */
    public Collection<String> getMessages() {
        return messages;
    }

    /**
     * Get the querySentToService string.
     * @return
     */
    public String getQuerySentToService() {
        return querySentToService;
    }

    /**
     * Add an error to the errors collection.
     * If error or errorType is null, it will send a NullPointerException.
     * @param error
     * @param errorType, String defining the global type of error that occured. To be filled with one of the ERROR_TYPE
     * defined in this class ( ex. = UniprotServiceResult.SEVERAL_PROT_BELONGING_TO_SAME_ORGA_ERROR_TYPE).
     */
    public void addError(String error, String errorType){
        if(error == null){
            throw new IllegalArgumentException("error argument can not be null");
        }
        if(errorType == null){
            throw new IllegalArgumentException("errorType argument can not be null");
        }
        errors.put(errorType,error);
    }

    /**
     * Get the errors collection.
     * @return
     */
    public Map<String, String> getErrors() {
        return errors;
    }

    public void addAllToProteins(Collection<Protein> proteins){
        this.proteins.addAll(proteins);
    }


    public Collection<Protein> getProteins() {
        return proteins;
    }
}