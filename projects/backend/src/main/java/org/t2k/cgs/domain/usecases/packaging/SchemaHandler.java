package org.t2k.cgs.domain.usecases.packaging;


import org.t2k.cgs.service.packaging.PackageHandlerImpl;

/**
 * Created with IntelliJ IDEA.
 * User: asaf.shochet
 * Date: 19/06/14
 * Time: 16:24
 */
public interface SchemaHandler {

      /***
     * Adds a resource with the cgs schema version
     * @param packageHandler
     * @throws Exception
     */

    public void addCgsSchemaVersionToPackage(PackageHandlerImpl packageHandler) throws Exception;


}
