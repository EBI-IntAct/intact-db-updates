package uk.ac.ebi.intact.dbupdate.feature.mutation;


import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.model.impl.*;
import uk.ac.ebi.intact.jami.model.extension.ExperimentalRange;
import uk.ac.ebi.intact.jami.model.extension.IntactFeatureEvidence;

@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GlobalMutationUpdateTest{


    public void test (){
        //EBI-1153050
//        IntactFeatureEvidence intactFeatureEvidence = new IntactFeatureEvidence();
//        intactFeatureEvidence.setShortName("test11-22test");
//        Interactor interactor = new DefaultInteractor("testInteractor");
//        ((Polymer) interactor).setSequence("METLSYSQIKKRKADFDEDISKRARQLPVGEQLPLSRLLQYSDKQQLFTILLQCVEKHPDLARDIRGILPAPSMDTCVETLRKLLINLNDSFPYGGDKRGDYAFNRIREKYMAVLHALNDMVPCYLPPYSTCFEKNITFLDAATNVVHELPEFHNPNHNVYKSQAYYELTGAWLVVLRQLEDRPVVPLLPLEELEEHNKTSQNRMEEALNYLKQLQKNEPLVHERSHTFQQTNPQNNFHRHTNSMNIGNDNGMGWHSMHQYI");
//        CvTerm cvTerm = new DefaultCvTerm("protein");
//        interactor.setInteractorType(cvTerm);
//        Position start1 = new DefaultPosition(11);
//        Position end1 = new DefaultPosition(11);
//        ResultingSequence resultingSequence1 = new DefaultResultingSequence("K", "R");
//        Range range1 = new DefaultRange(start1, end1, resultingSequence1);
//
    }

}