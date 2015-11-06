package uk.ac.ebi.intact.dbupdate.feature.utils;

import uk.ac.ebi.intact.bridges.olslight.OlsLightService;
import uk.ac.ebi.intact.bridges.olslight.OntologyId;
import uk.ac.ebi.intact.bridges.olslight.OntologyService;
import uk.ac.ebi.intact.bridges.olslight.OntologyServiceException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class OntologyServiceHelper {

    private static OntologyServiceHelper ontologyServiceHelper = new OntologyServiceHelper();
    private static OntologyService ontologyService = new OlsLightService();

    public OntologyServiceHelper() {
    }

    public static OntologyServiceHelper getOntologyServiceHelper() {
        return ontologyServiceHelper;
    }

    public List<String> getAssociatedMITerms(String miTerm, int depth) {
        List<String> associatedMITerms = new ArrayList<String>();
        Map<String, String> psiMITerms = null;
        try {
            psiMITerms = ontologyService.getTermChildren(OntologyId.PSI_MI, miTerm, depth);
        } catch (OntologyServiceException e) {
            e.printStackTrace();
        }
        if (psiMITerms == null) {
            System.err.println("Could not retrieve PSIMI-Terms from OLS");
            System.exit(0);
        }
        for (Map.Entry<String, String> entry : psiMITerms.entrySet()) {
            associatedMITerms.add(entry.getKey());
        }
        associatedMITerms.add(miTerm);
        return associatedMITerms;
    }
}
