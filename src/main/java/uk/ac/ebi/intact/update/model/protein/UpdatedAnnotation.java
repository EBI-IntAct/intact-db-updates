package uk.ac.ebi.intact.update.model.protein;

import uk.ac.ebi.intact.model.Annotation;
import uk.ac.ebi.intact.update.model.HibernateUpdatePersistentImpl;
import uk.ac.ebi.intact.update.model.UpdateEventImpl;
import uk.ac.ebi.intact.update.model.UpdateStatus;

import javax.persistence.*;

/**
 * ProteinUpdateAnnotation of a protein
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@MappedSuperclass
public class UpdatedAnnotation extends HibernateUpdatePersistentImpl {

    /**
     * The annotation topic
     */
    private String topic;

    /**
     * The annotation description
     */
    private String text;

    /**
     * The updateProcess
     */
    private UpdateEventImpl parent;

    /**
     * The status
     */
    private UpdateStatus status;

    public UpdatedAnnotation(){
        super();
        this.topic = null;
        this.text = null;
        this.status = UpdateStatus.none;
        this.parent = null;
    }

    public UpdatedAnnotation(String topic, String text, UpdateStatus status){
        super();
        this.topic = topic;
        this.text = text;
        this.status = status != null ? status : UpdateStatus.none;
        this.parent = null;
    }

    public UpdatedAnnotation(Annotation annotation, UpdateStatus status){
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
        this.parent = null;
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

    @Transient
    public UpdateEventImpl getParent() {
        return parent;
    }

    public void setParent(UpdateEventImpl parent) {
        this.parent = parent;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final UpdatedAnnotation updated = ( UpdatedAnnotation ) o;

        if ( topic != null ) {
            if (!topic.equals( updated.getTopic())){
                return false;
            }
        }
        else if (updated.getTopic()!= null){
            return false;
        }

        if ( text != null ) {
            if (!text.equals( updated.getText())){
                return false;
            }
        }
        else if (updated.getText()!= null){
            return false;
        }

        if ( status != null ) {
            if (!status.equals( updated.getStatus())){
                return false;
            }
        }
        else if (updated.getStatus()!= null){
            return false;
        }

        return true;
    }

    /**
     * This class overwrites equals. To ensure proper functioning of HashTable,
     * hashCode must be overwritten, too.
     *
     * @return hash code of the object.
     */
    @Override
    public int hashCode() {

        int code = 29;

        code = 29 * code + super.hashCode();

        if ( topic != null ) {
            code = 29 * code + topic.hashCode();
        }

        if ( text != null ) {
            code = 29 * code + text.hashCode();
        }

        if ( status != null ) {
            code = 29 * code + status.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final UpdatedAnnotation updated = ( UpdatedAnnotation ) o;

        if ( topic != null ) {
            if (!topic.equals( updated.getTopic())){
                return false;
            }
        }
        else if (updated.getTopic()!= null){
            return false;
        }

        if ( text != null ) {
            if (!text.equals( updated.getText())){
                return false;
            }
        }
        else if (updated.getText()!= null){
            return false;
        }

        if ( status != null ) {
            if (!status.equals( updated.getStatus())){
                return false;
            }
        }
        else if (updated.getStatus()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Annotation : [ topic = " + topic != null ? topic : "none" + ", text = " + text != null ? text : "none" + ", status = " + status != null ? status.toString() : "none");

        return buffer.toString();
    }
}
