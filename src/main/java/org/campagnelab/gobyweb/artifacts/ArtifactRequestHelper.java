package org.campagnelab.gobyweb.artifacts;

import org.apache.commons.io.FilenameUtils;
import org.campagnelab.groovySupport.ExecAndRemote;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Helps execute artifact requests.
 *
 * @author Fabien Campagne
 *         Date: 12/19/12
 *         Time: 12:15 PM
 */
public class ArtifactRequestHelper {
    private final Artifacts.InstallationSet requests;

    public ArtifactRequestHelper(File pbRequestFile) throws IOException {

        requests = Artifacts.InstallationSet.parseDelimitedFrom(new FileInputStream(pbRequestFile));
    }

    final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    public void install(File repoDir) throws IOException {
        ArtifactRepo repo = new ArtifactRepo(repoDir);
        repo.load();
        for (Artifacts.ArtifactDetails request : requests.getArtifactsList()) {
            final String scriptInstallPath = request.getScriptInstallPath();
            final String localFilename = FilenameUtils.concat(TEMP_DIR, FilenameUtils.getBaseName(scriptInstallPath));

            String username=System.getProperty("user.name");
            String server=request.getSshWebAppHost();
            executor.scp(String.format("%s@%s:%s",username, server, scriptInstallPath),localFilename);

            repo.install(request.getPluginId(), request.getArtifactId(), localFilename, request.getVersion());

            // remove the local script from $TEMP_DIR:
            new File(localFilename).delete();
        }
        repo.save();
    }
    private ExecAndRemote executor=new ExecAndRemote();


}
