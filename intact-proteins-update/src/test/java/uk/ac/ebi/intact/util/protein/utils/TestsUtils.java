package uk.ac.ebi.intact.util.protein.utils;

import uk.ac.ebi.intact.model.*;

import java.util.Collection;

public class TestsUtils {

    public static boolean hasXRef(Protein p, String primaryAc, String databaseName, String qualifierName) {
        final Collection<InteractorXref> refs = p.getXrefs();
        boolean hasXRef = false;

        for ( InteractorXref ref : refs ) {
            if (databaseName.equalsIgnoreCase(ref.getCvDatabase().getShortLabel())){
                if ((qualifierName == null && ref.getCvXrefQualifier() == null) ||
                        (qualifierName != null && ref.getCvXrefQualifier() != null && qualifierName.equalsIgnoreCase(ref.getCvXrefQualifier().getShortLabel()))) {
                    if (primaryAc.equalsIgnoreCase(ref.getPrimaryId())){
                        hasXRef = true;
                    }
                }
            }
        }
        return hasXRef;
    }

    public static boolean hasXRef(Protein p, String primaryAc, String databaseName, String secondaryId, String qualifierName) {
        final Collection<InteractorXref> refs = p.getXrefs();
        boolean hasXRef = false;

        for (InteractorXref ref : refs) {
            if (databaseName.equalsIgnoreCase(ref.getCvDatabase().getShortLabel())) {
                if ((qualifierName == null && ref.getCvXrefQualifier() == null) ||
                        (qualifierName != null && ref.getCvXrefQualifier() != null && qualifierName.equalsIgnoreCase(ref.getCvXrefQualifier().getShortLabel()))) {
                    if (primaryAc.equalsIgnoreCase(ref.getPrimaryId())) {
                        if ((secondaryId == null && ref.getSecondaryId() == null) || (secondaryId != null && secondaryId.equalsIgnoreCase(ref.getSecondaryId()))) {
                            hasXRef = true;
                        }
                    }
                }
            }
        }

        return hasXRef;
    }

    public static boolean hasAlias( Protein p, String aliasLabel, String aliasName ) {
        final Collection<InteractorAlias> aliases = p.getAliases();

        boolean hasFoundAlias = false;

        for ( InteractorAlias alias : aliases ) {
            if (alias.getCvAliasType().getShortLabel().equals(aliasLabel)){
                if (aliasName.equals(alias.getName())){
                    hasFoundAlias = true;
                }
            }
        }

        return hasFoundAlias;
    }

    public static boolean hasAnnotation(Annotated p, String text, String cvTopic) {
        final Collection<Annotation> annotations = p.getAnnotations();
        boolean hasAnnotation = false;

        for ( Annotation a : annotations ) {
            if (cvTopic.equalsIgnoreCase(a.getCvTopic().getShortLabel())){
                if (text == null){
                    hasAnnotation = true;
                }
                else if (text != null && a.getAnnotationText() != null){
                    if (text.equalsIgnoreCase(a.getAnnotationText())){
                        hasAnnotation = true;
                    }
                }
            }
        }

        return hasAnnotation;
    }
}