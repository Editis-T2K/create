package org.t2k.cgs.domain.usecases.packaging;

import org.t2k.cgs.service.packaging.PackageHandlerImpl;

/**
 * Created by IntelliJ IDEA.
 * User: ophir.barnea
 * Date: 25/11/12
 * Time: 13:46
 */
public interface ManifestHandler {

    /*
         handles the manifest json for the package managed by the package handler.
         Writes the package manifest into manifest.json file in the package folder.
     */
    void handlePackageManifest(PackageHandlerImpl packageHandler) throws Exception; // fixme change dependency to PackageHandler
}