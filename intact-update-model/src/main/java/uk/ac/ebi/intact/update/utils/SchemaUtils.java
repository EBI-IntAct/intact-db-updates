package uk.ac.ebi.intact.update.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.dialect.*;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import uk.ac.ebi.intact.update.context.IntactUpdatePersistenceProvider;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * This class contains methods to generate database schemas for the annotated elements in this project
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>18-May-2010</pre>
 */

public class SchemaUtils {

    private static final Log log = LogFactory.getLog(SchemaUtils.class);
    private static final IntactUpdatePersistenceProvider persistence = new IntactUpdatePersistenceProvider();

    private SchemaUtils(){}

    /**
     * Generates the DDL schema
     * @param dialect the dialect to use (complete class name for the hibernate dialect object)
     * @return an array containing the SQL statements
     */
    public static String[] generateCreateSchemaDDL(DataSource dataSource, String dialect) {
        return exportSchema(
                "create",
                persistence.getBasicMetaDataBuilder(dataSource, dialect).build(),
                EnumSet.of(TargetType.SCRIPT),
                SchemaExport.Action.CREATE);
    }

    private static String[] exportSchema(
            String tempFileName,
            Metadata metadata,
            EnumSet<TargetType> targetTypes,
            SchemaExport.Action action) {

        String[] sqls;
        try {
            File file = File.createTempFile(tempFileName, ".sql");
            new SchemaExport().setDelimiter(";")
                    .setOutputFile(file.getAbsolutePath())
                    .execute(targetTypes, action, metadata);
            try (Stream<String> lines = Files.lines(file.toPath())) {
                sqls = lines.toArray(String[]::new);
            }
            if (file.delete()) log.debug("Temp file deleted");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sqls;
    }

    /**
     * Generates the DDL schema for Oracle 9i.
     * @return an array containing the SQL statements
     */
    public static String[] generateCreateSchemaDDLForOracle(DataSource dataSource) {
        return generateCreateSchemaDDL(dataSource, Oracle10gDialect.class.getName());
    }

    /**
     * Generates the DDL schema for PostgreSQL.
     * @return an array containing the SQL statements
     */
    public static String[] generateCreateSchemaDDLForPostgreSQL(DataSource dataSource) {
        return generateCreateSchemaDDL(dataSource, PostgreSQL82Dialect.class.getName());
    }

    /**
     * Generates the DDL schema for HSQL DB.
     * @return an array containing the SQL statements
     */
    public static String[] generateCreateSchemaDDLForHSQL(DataSource dataSource) {
        return generateCreateSchemaDDL(dataSource, HSQLDialect.class.getName());
    }

    /**
     * Generates the DDL schema for HSQL DB.
     * @return an array containing the SQL statements
     */
    public static String[] generateCreateSchemaDDLForH2(DataSource dataSource) {
        return generateCreateSchemaDDL(dataSource, H2Dialect.class.getName());
    }

    public static String[] getTableNames(DataSource dataSource) {
        return persistence.getBasicMetaDataBuilder(dataSource, Oracle10gDialect.class.getName()).build()
                .getEntityBindings()
                .stream()
                .map(persistentClass -> {
                    List<String> tableNames = new ArrayList<>();
                    if (!persistentClass.isAbstract()) {
                        tableNames.add(persistentClass.getTable().getName());
                    }
                    Iterator propertiesIterator = persistentClass.getPropertyIterator();
                    while (propertiesIterator.hasNext()) {
                        Property property = (Property) propertiesIterator.next();
                        if (property.getValue().getType().isCollectionType()) {
                            Table collectionTable = ((Collection) property.getValue()).getCollectionTable();
                            if (collectionTable != null) {
                                tableNames.add(collectionTable.getName());
                            }
                        }
                    }
                    return tableNames;
                })
                .flatMap(List::stream)
                .distinct()
                .toArray(String[]::new);
    }
}
