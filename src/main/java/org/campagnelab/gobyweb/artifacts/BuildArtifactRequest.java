package org.campagnelab.gobyweb.artifacts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Helper class to prepare artifact installation requests for installation on the server.
 * @author Fabien Campagne
 *         Date: 12/19/12
 *         Time: 10:44 AM
 */
public class BuildArtifactRequest {
    private String webServerHostname;
    Artifacts.InstallationSet.Builder installationSetBuilder;

    public BuildArtifactRequest(String webServerHostname) {
        this.webServerHostname = webServerHostname;
        installationSetBuilder = Artifacts.InstallationSet.newBuilder();
    }

    public void addArtifact(String pluginId, String artifactId, String version, String installScript) {

        Artifacts.ArtifactDetails.Builder detailsBuilder = Artifacts.ArtifactDetails.newBuilder();
        detailsBuilder.setArtifactId(artifactId);
        detailsBuilder.setPluginId(pluginId);
        detailsBuilder.setVersion(version);
        detailsBuilder.setScriptInstallPath(new File(installScript).getAbsolutePath());
        detailsBuilder.setSshWebAppHost(webServerHostname);
        installationSetBuilder.addArtifacts(detailsBuilder);
    }

    public void save(File output) throws IOException {
        final FileOutputStream output1 = new FileOutputStream(output);
        installationSetBuilder.build().writeDelimitedTo(output1);
        output1.close();
    }

}
