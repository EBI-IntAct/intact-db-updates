package uk.ac.ebi.intact.update.model.protein.update.protein;

import uk.ac.ebi.intact.update.model.HibernatePersistentImpl;
import uk.ac.ebi.intact.model.CvTopic;

import javax.persistence.*;
import java.util.Date;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@Entity
@Table(name = "ia_protein_annotation")
public class Annotation extends HibernatePersistentImpl {

    private String annotationAc;
    private String topicAc;
    private String text;

    private IntactProtein intactProtein;

    public Annotation(){
        super();
        this.topicAc = null;
        this.text = null;
        this.intactProtein = null;
        this.annotationAc = null;
    }

    public Annotation(String annotationAc, String topic, String text){
        super();
        this.annotationAc = annotationAc;
        this.topicAc = topic;
        this.text = text;
        this.intactProtein = null;
        setCreated(new Date(System.currentTimeMillis()));
    }

    public Annotation(uk.ac.ebi.intact.model.Annotation annotation, Date created){
        super();
        setCreated(created);
        if (annotation != null){
            this.annotationAc = annotation.getAc();
            
            CvTopic topic = annotation.getCvTopic();
            if (topic != null){
                this.topicAc = topic.getAc();
            }
            else {
                this.topicAc = null;
            }

            this.text = annotation.getAnnotationText();
        }
        else {
            this.topicAc = null;
            this.text = null;
            this.intactProtein = null;
        }
    }

    @Column(name = "topic_ac", nullable = false)
    public String getTopicAc() {
        return topicAc;
    }

    public void setTopicAc(String topicAc) {
        this.topicAc = topicAc;
    }

    @Column(name = "text", nullable = true)
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @ManyToOne
    @JoinColumn( name = "protein_id", nullable = true)
    public IntactProtein getIntactProtein() {
        return intactProtein;
    }

    public void setIntactProtein(IntactProtein intactProtein) {
        this.intactProtein = intactProtein;
    }

    @Column(name = "annotation_ac", nullable = false)
    public String getAnnotationAc() {
        return annotationAc;
    }

    public void setAnnotationAc(String annotationAc) {
        this.annotationAc = annotationAc;
    }
}
