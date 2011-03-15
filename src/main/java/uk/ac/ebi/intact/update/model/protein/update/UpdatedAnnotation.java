package uk.ac.ebi.intact.update.model.protein.update;

import uk.ac.ebi.intact.update.model.HibernatePersistentImpl;

import javax.persistence.*;

/**
 * UpdatedAnnotation of a protein
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@Entity
@Table(name = "ia_updated_annotation")
public class UpdatedAnnotation extends HibernatePersistentImpl {

    private String topic;
    private String text;

    private UpdateStatus status;

    public UpdatedAnnotation(){
        super();
        this.topic = null;
        this.text = null;
        this.status = UpdateStatus.none;
    }

    public UpdatedAnnotation(String topic, String text, UpdateStatus status){
        super();
        this.topic = topic;
        this.text = text;
        this.status = status != null ? status : UpdateStatus.none;
    }

    public UpdatedAnnotation(uk.ac.ebi.intact.model.Annotation annotation, UpdateStatus status){
        super();
        if (annotation != null){

            topic = annotation.getCvTopic() != null ? annotation.getCvTopic().getAc() : null;

            this.text = annotation.getAnnotationText();
        }
        else {
            this.topic = null;
            this.text = null;
        }
        this.status = status != null ? status : UpdateStatus.none;
    }

    @Column(name="topic_ac", nullable = false)
    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    @Column(name = "text", nullable = true)
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    public UpdateStatus getStatus() {
        return status;
    }

    public void setStatus(UpdateStatus status) {
        this.status = status;
    }
}
