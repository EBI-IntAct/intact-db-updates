package uk.ac.ebi.intact.update.model.utils;

import uk.ac.ebi.intact.protein.mapping.actions.ActionName;
import uk.ac.ebi.intact.protein.mapping.actions.status.StatusLabel;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentMappingReport;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Contains utility methods for the protein mapping reports (when remapping proteins to uniprot)
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>30/03/11</pre>
 */

public class MappingReportUtils {

     public static Collection<PersistentMappingReport> extractReportsHavingNameFrom(Collection<PersistentMappingReport> reports, ActionName name){

         Collection<PersistentMappingReport> extractedReport = new ArrayList<PersistentMappingReport>(reports.size());

         if (reports != null && name != null){
             for (PersistentMappingReport report : reports){
                 if (name.equals(report.getName())){
                     extractedReport.add(report);
                 }
             }
         }

         return extractedReport;
     }

    public static Collection<PersistentMappingReport> extractReportsHavingStatusFrom(Collection<PersistentMappingReport> reports, StatusLabel label){

         Collection<PersistentMappingReport> extractedReport = new ArrayList<PersistentMappingReport>(reports.size());

         if (reports != null && label != null){
             for (PersistentMappingReport report : reports){
                 if (label.equals(report.getStatusLabel())){
                     extractedReport.add(report);
                 }
             }
         }

         return extractedReport;
     }
}
