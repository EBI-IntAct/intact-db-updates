package uk.ac.ebi.intact.update.utils;

import org.junit.Assert;
import org.junit.Test;
import uk.ac.ebi.intact.update.utils.SchemaUtils;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02/08/11</pre>
 */

public class SchemaUtilsTest {

    @Test
    public void testGenerateCreateSchemaDDLForOracle() throws Exception {
        String[] strings = SchemaUtils.generateCreateSchemaDDLForOracle();

        for (String s : strings){
            System.out.println(s);
        }

        Assert.assertEquals(50, strings.length);
        Assert.assertEquals(50, SchemaUtils.generateCreateSchemaDDLForPostgreSQL().length);
        Assert.assertEquals(50, SchemaUtils.generateCreateSchemaDDLForHSQL().length);
        Assert.assertEquals(50, SchemaUtils.generateCreateSchemaDDLForH2().length);
    }
}
