
package uk.ac.ebi.intact.dbupdate.gene.parser.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Describes the source of the evidence, when it is not assigned by UniProt, but imported from an external database.
 * 
 * <p>Java class for importedFromType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="importedFromType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="dbReference" type="{http://uniprot.org/uniprot}dbReferenceType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "importedFromType", propOrder = {
    "dbReference"
})
public class ImportedFromType {

    @XmlElement(required = true)
    protected DbReferenceType dbReference;

    /**
     * Gets the value of the dbReference property.
     * 
     * @return
     *     possible object is
     *     {@link DbReferenceType }
     *     
     */
    public DbReferenceType getDbReference() {
        return dbReference;
    }

    /**
     * Sets the value of the dbReference property.
     * 
     * @param value
     *     allowed object is
     *     {@link DbReferenceType }
     *     
     */
    public void setDbReference(DbReferenceType value) {
        this.dbReference = value;
    }

}
