package org.campagnelab.gobyweb.artifacts.repositories;

import org.campagnelab.gobyweb.artifacts.ArtifactRepo;
import org.campagnelab.gobyweb.artifacts.Artifacts;

import java.io.IOException;

/**
 * Created by mas2182 on 8/18/15.
 */
public interface SourceRepository extends  Repository {

    boolean fetchWithLog(ArtifactRepo artifactRepo,
                      String scriptInstallPath,
                      String relativePath, String absolutePath);

    boolean fetch(String sourcePath, String targetPath) throws Exception;


}
