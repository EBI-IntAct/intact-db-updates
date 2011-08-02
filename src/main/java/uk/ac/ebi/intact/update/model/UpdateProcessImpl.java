package uk.ac.ebi.intact.update.model;

import org.apache.commons.collections.CollectionUtils;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * Base implementation for update events
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>18/07/11</pre>
 */
@MappedSuperclass
public abstract class UpdateProcessImpl<T extends UpdateEventImpl> extends HibernateUpdatePersistentImpl implements UpdateProcess<T>{

    private Date date;
    private String userStamp;
    private UpdateProcessName updateName;
    protected Collection<T> updateEvents = new ArrayList<T>();

    public UpdateProcessImpl(){
        super();
        this.date = null;
        this.userStamp = null;
        this.updateName = UpdateProcessName.none;
    }

    public UpdateProcessImpl(Date date, String userStamp, UpdateProcessName updateName){
        super();
        this.date = date;
        this.userStamp = userStamp;
        this.updateName = updateName;
    }

    @Override
    @Temporal( value = TemporalType.TIMESTAMP )
    @Column(name = "date", nullable = false)
    public Date getDate() {
        return this.date;
    }

    @Override
    @Column(name = "userstamp", nullable = false)
    public String getUserStamp() {
        return this.userStamp;
    }

    @Override
    @Column(name = "name", nullable = false)
    @Enumerated(EnumType.STRING)
    public UpdateProcessName getUpdateName() {
        return this.updateName;
    }

    @Override
    @OneToMany(mappedBy="parent", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
    public Collection<T> getUpdateEvents() {
        return this.updateEvents;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setUpdateName(UpdateProcessName updateName) {
        this.updateName = updateName;
    }

    public void setUserStamp(String userStamp) {
        this.userStamp = userStamp;
    }

    public void setUpdateEvents(Collection<T> updateEvents) {
        this.updateEvents = updateEvents;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final UpdateProcess process = ( UpdateProcess ) o;

        if ( date != null ) {
            if (!date.equals( process.getDate())){
                return false;
            }
        }
        else if (process.getDate()!= null){
            return false;
        }

        if ( userStamp != null ) {
            if (!userStamp.equals( process.getUserStamp())){
                return false;
            }
        }
        else if (process.getUserStamp()!= null){
            return false;
        }

        if ( updateName != null ) {
            if (!updateName.equals( process.getUpdateName())){
                return false;
            }
        }
        else if (process.getUpdateName()!= null){
            return false;
        }

        return true;
    }

    public boolean addEvent(T event){
        if (updateEvents.add(event)){
            event.setParent(this);
            return true;
        }
        return false;
    }

    public boolean removeEvent(T event){
        if (updateEvents.remove(event)){
            event.setParent(null);
            return true;
        }
        return false;
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

        if ( date != null ) {
            code = 29 * code + date.hashCode();
        }

        if ( userStamp != null ) {
            code = 29 * code + userStamp.hashCode();
        }

        if ( updateName != null ) {
            code = 29 * code + updateName.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final UpdateProcess process = ( UpdateProcess ) o;

        if ( date != null ) {
            if (!date.equals( process.getDate())){
                return false;
            }
        }
        else if (process.getDate()!= null){
            return false;
        }

        if ( userStamp != null ) {
            if (!userStamp.equals( process.getUserStamp())){
                return false;
            }
        }
        else if (process.getUserStamp()!= null){
            return false;
        }

        if ( updateName != null ) {
            if (!updateName.equals( process.getUpdateName())){
                return false;
            }
        }
        else if (process.getUpdateName()!= null){
            return false;
        }

        if (!CollectionUtils.isEqualCollection(updateEvents, process.getUpdateEvents())){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("Update process : [ date = " + date != null ? date.toString() : "none" + ", name = "+updateName != null ? updateName.toString() : "none"+", userstamp = "+userStamp != null ? userStamp : "none"+"] \n");

        return buffer.toString();
    }

}
