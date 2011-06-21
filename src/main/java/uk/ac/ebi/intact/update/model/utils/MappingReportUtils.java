package uk.ac.ebi.intact.update.model.utils;

import uk.ac.ebi.intact.update.model.protein.mapping.actions.ActionName;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.MappingReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.status.StatusLabel;

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

     public static Collection<MappingReport> extractReportsHavingNameFrom(Collection<MappingReport> reports, ActionName name){

         Collection<MappingReport> extractedReport = new ArrayList<MappingReport>(reports.size());

         if (reports != null && name != null){
             for (MappingReport report : reports){
                 if (name.equals(report.getName())){
                     extractedReport.add(report);
                 }
             }
         }

         return extractedReport;
     }

    public static Collection<MappingReport> extractReportsHavingStatusFrom(Collection<MappingReport> reports, StatusLabel label){

         Collection<MappingReport> extractedReport = new ArrayList<MappingReport>(reports.size());

         if (reports != null && label != null){
             for (MappingReport report : reports){
                 if (label.equals(report.getStatusLabel())){
                     extractedReport.add(report);
                 }
             }
         }

         return extractedReport;
     }
}
