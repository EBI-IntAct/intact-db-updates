package uk.ac.ebi.intact.dbupdate.bioactiveentity.importer;

import uk.ac.ebi.intact.model.SmallMolecule;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 23/05/2013
 * Time: 09:39
 */
public interface BioActiveEntityService {

    public SmallMolecule getBioEntityByChebiId(String chebiAc) throws BioActiveEntityServiceException;

//    It should work in the feature, apparently there is a bug in the chebi web service that they need to fix to have the same logic
//    when we query by one object and when we query several object at the same time.
//    public List<SmallMolecule> getBioEntityByChebiIdList(List<String> chebiIds) throws BioActiveEntityServiceException;

}
