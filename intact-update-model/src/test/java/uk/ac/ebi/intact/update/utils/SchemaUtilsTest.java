package uk.ac.ebi.intact.update.utils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sql.DataSource;

/**
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02/08/11</pre>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath*:/META-INF/db-update-test.spring.xml",
        "classpath*:/META-INF/intact.spring.xml"
})
@TestPropertySource(locations="classpath:/retry.properties")
public class SchemaUtilsTest {

    @Autowired
    @Qualifier("intactUpdateDataSource")
    DataSource dataSource;

    @Test
    public void testGenerateCreateSchemaDDLForPostgres() {
        String[] strings = SchemaUtils.generateCreateSchemaDDLForPostgreSQL(dataSource);

        Assert.assertEquals(44, strings.length);
        Assert.assertEquals(44, SchemaUtils.generateCreateSchemaDDLForOracle(dataSource).length);
        Assert.assertEquals(44, SchemaUtils.generateCreateSchemaDDLForHSQL(dataSource).length);
        Assert.assertEquals(44, SchemaUtils.generateCreateSchemaDDLForH2(dataSource).length);

        Assert.assertEquals(22, SchemaUtils.getTableNames(dataSource).length);
    }
}
