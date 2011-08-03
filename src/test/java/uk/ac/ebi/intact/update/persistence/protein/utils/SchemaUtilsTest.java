package uk.ac.ebi.intact.update.persistence.protein.utils;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import uk.ac.ebi.intact.update.model.utils.SchemaUtils;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02/08/11</pre>
 */

public class SchemaUtilsTest {

        @Test
        @Ignore
    public void testGenerateCreateSchemaDDLForOracle() throws Exception {
        String[] strings = SchemaUtils.generateCreateSchemaDDLForOracle();

        Assert.assertEquals(183, strings.length);
        Assert.assertEquals(183, SchemaUtils.generateCreateSchemaDDLForPostgreSQL().length);
        Assert.assertEquals(183, SchemaUtils.generateCreateSchemaDDLForHSQL().length);
        Assert.assertEquals(183, SchemaUtils.generateCreateSchemaDDLForH2().length);
    }
}
