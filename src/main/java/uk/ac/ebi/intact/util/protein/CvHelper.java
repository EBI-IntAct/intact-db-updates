/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.protein;

import uk.ac.ebi.intact.context.IntactContext;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.persistence.dao.CvObjectDao;
import uk.ac.ebi.intact.persistence.dao.DaoFactory;

/**
 * Controlled Vocabulary Helper, provides methods for retreiving CV terms.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since <pre>08-Feb-2007</pre>
 */
public class CvHelper {

    public static CvDatabase getDatabaseByMi( String miRef ) {
        DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
        CvObjectDao<CvDatabase> cvObjectDao = daoFactory.getCvObjectDao( CvDatabase.class );
        CvDatabase db = cvObjectDao.getByPsiMiRef( miRef );
        if ( db == null ) {
            throw new IllegalStateException( "Could not find CvDatabase by MI ref: " + miRef );
        }
        return db;
    }

    public static CvXrefQualifier getQualifierByMi( String miRef ) {
        DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
        CvObjectDao<CvXrefQualifier> cvObjectDao = daoFactory.getCvObjectDao( CvXrefQualifier.class );
        CvXrefQualifier qualif = cvObjectDao.getByPsiMiRef( miRef );
        if ( qualif == null ) {
            throw new IllegalStateException( "Could not find CvXrefQualifier by MI ref: " + miRef );
        }
        return qualif;
    }

    public static CvAliasType getAliasTypeByMi( String miRef ) {
        DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
        CvObjectDao<CvAliasType> cvObjectDao = daoFactory.getCvObjectDao( CvAliasType.class );
        CvAliasType type = cvObjectDao.getByPsiMiRef( miRef );
        if ( type == null ) {
            throw new IllegalStateException( "Could not find CvAliasType by MI ref: " + miRef );
        }
        return type;
    }

    public static CvTopic getTopicByMi( String miRef ) {
        DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
        CvObjectDao<CvTopic> cvObjectDao = daoFactory.getCvObjectDao( CvTopic.class );
        CvTopic topic = cvObjectDao.getByPsiMiRef( miRef );
        if ( topic == null ) {
            throw new IllegalStateException( "Could not find CvTopic by MI topic: " + miRef );
        }
        return topic;
    }

    public static CvInteractorType getInteractorTypeByMi( String miRef ) {
        DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
        CvObjectDao<CvInteractorType> cvObjectDao = daoFactory.getCvObjectDao( CvInteractorType.class );
        CvInteractorType type = cvObjectDao.getByPsiMiRef( miRef );
        if ( type == null ) {
            throw new IllegalStateException( "Could not find CvInteractorType by MI type: " + miRef );
        }
        return type;
    }

    public static Institution getInstitution() {
        return IntactContext.getCurrentInstance().getInstitution();
    }

    public static CvInteractorType getProteinType() {
        return getInteractorTypeByMi( CvInteractorType.PROTEIN_MI_REF );
    }
}