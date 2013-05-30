package uk.ac.ebi.intact.dbupdate.bioactiveentity.importer;

import uk.ac.ebi.intact.model.SmallMolecule;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 23/05/2013
 * Time: 09:39
 */
public interface BioActiveEntityService {

    public SmallMolecule getBioEntityByChebiId(String chebiAc) throws BioActiveEntityServiceException;

    public List<SmallMolecule> getBioEntityByChebiIdList(List<String> chebiIds) throws BioActiveEntityServiceException;

}
