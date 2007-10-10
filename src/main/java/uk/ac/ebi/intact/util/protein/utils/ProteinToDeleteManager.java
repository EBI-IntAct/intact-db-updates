/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.protein.utils;

import java.util.Collection;
import java.util.ArrayList;

/**
 * TODO comment this
 *
 * @author Catherine Leroy (cleroy@ebi.ac.uk)
 * @version $Id$
 * @since TODO
 */
public class ProteinToDeleteManager {
    private static ThreadLocal<Collection<String>> threadLocal = new ThreadLocal<Collection<String>>();


    static{
        Collection<String> acsToDelete = new ArrayList<String>();
        threadLocal.set(acsToDelete);
    }

    public static void addProteinAc(String ac){
        Collection<String> acsToDelete = threadLocal.get();
        acsToDelete.add(ac);
        threadLocal.set(acsToDelete);
    }

    public static Collection<String> getAcToDelete(){
        return threadLocal.get();
    }


}