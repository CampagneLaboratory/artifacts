package org.campagnelab.gobyweb.artifacts;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

/**
 * Helper class to prepare artifact installation requests for installation on the server.
 *
 * @author Fabien Campagne
 *         Date: 12/19/12
 *         Time: 10:44 AM
 */
public class BuildArtifactRequest {
    private static final org.apache.log4j.Logger LOG = Logger.getLogger(ArtifactManager.class);


    private String webServerHostname;
    private String webServerUsername;
    private Artifacts.InstallationSet.Builder installationSetBuilder;

    public BuildArtifactRequest(String webServerHostname) {
        if (webServerHostname.contains("@")) {
            final String[] split = webServerHostname.split("[@]");
            this.webServerUsername = split[0];
            this.webServerHostname = split[1];
        } else {
            this.webServerUsername = null;
            this.webServerHostname = webServerHostname;

        }
        installationSetBuilder = Artifacts.InstallationSet.newBuilder();
    }

    public void addArtifact(String pluginId, String artifactId, String version, String installScript) {
        addArtifact(pluginId, artifactId, version, installScript, Artifacts.RetentionPolicy.KEEP_UNTIL_EXPLICIT_REMOVE
                );
    }

    public void addArtifact(String pluginId, String artifactId, String version, String installScript,
                             Artifacts.AttributeValuePair ... attributes) {
            addArtifact(pluginId, artifactId, version, installScript, Artifacts.RetentionPolicy.KEEP_UNTIL_EXPLICIT_REMOVE,
                    attributes);
        }

    public void addArtifact(String pluginId, String artifactId, String version, String installScript,
                            Artifacts.RetentionPolicy retention , Artifacts.AttributeValuePair ... attributes) {

        Artifacts.ArtifactDetails.Builder detailsBuilder = Artifacts.ArtifactDetails.newBuilder();
        detailsBuilder.setArtifactId(artifactId);
        detailsBuilder.setPluginId(pluginId);
        detailsBuilder.setVersion(version);
        detailsBuilder.setScriptInstallPath(new File(installScript).getAbsolutePath());
        detailsBuilder.setSshWebAppHost(webServerHostname);
        detailsBuilder.setRetention(retention);

        detailsBuilder.addAllAttributes(ObjectArrayList.wrap(attributes));
        if (webServerUsername != null) detailsBuilder.setSshWebAppUserName(webServerUsername);
        installationSetBuilder.addArtifacts(detailsBuilder);
    }


    public void save(File output) throws IOException {

        final FileOutputStream output1 = new FileOutputStream(output);
        installationSetBuilder.build().writeDelimitedTo(output1);
        output1.close();
    }

}
