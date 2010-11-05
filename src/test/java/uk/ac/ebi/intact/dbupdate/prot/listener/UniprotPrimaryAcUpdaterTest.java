package uk.ac.ebi.intact.dbupdate.prot.listener;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.prot.ProteinProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.model.Protein;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>05-Nov-2010</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/jpa.test.spring.xml"} )
public class UniprotPrimaryAcUpdaterTest extends IntactBasicTestCase {

    private class DummyProcessor extends ProteinUpdateProcessor {
        protected void registerListeners() {
        }
    }

    @Test
    @DirtiesContext
    /**
     * Several unirpto entries, different organims, no organism matching the one of the protein. Cannot be updated
     */
    public void onPreProcess_several_uniprot_proteins_several_organims_different_taxId() throws Exception{
        Protein prot = getMockBuilder().createProtein("P21181", "test_several_uniprot");

        ProteinProcessor processor = new DummyProcessor();
        ProteinEvent evt = new ProteinEvent(processor, null, prot);
        evt.setUniprotIdentity("P21181");
        Assert.assertNull(evt.getUniprotProtein());

    }
}
