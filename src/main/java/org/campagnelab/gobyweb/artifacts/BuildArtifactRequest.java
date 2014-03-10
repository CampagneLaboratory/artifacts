package org.campagnelab.gobyweb.artifacts;

import com.google.protobuf.TextFormat;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Helper class to prepare artifact installation requests for installation on the server.
 *
 * @author Fabien Campagne
 *         Date: 12/19/12
 *         Time: 10:44 AM
 */
public class BuildArtifactRequest {
    private static final org.apache.log4j.Logger LOG = Logger.getLogger(ArtifactManager.class);
    public static final String ARTIFACTS_ENVIRONMENT_COLLECTION_SCRIPT = "ARTIFACTS_ENVIRONMENT_COLLECTION_SCRIPT";


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

    public void addArtifact(String pluginId, String artifactId, String version, boolean mandatory, String installScript) {
        addArtifact(pluginId, artifactId, version, mandatory, installScript, Artifacts.RetentionPolicy.KEEP_UNTIL_EXPLICIT_REMOVE
        );
    }

    public void addArtifact(String pluginId, String artifactId, String version, boolean mandatory, String installScript,
                            Artifacts.AttributeValuePair... attributes) {
        addArtifact(pluginId, artifactId, version, mandatory, installScript, Artifacts.RetentionPolicy.KEEP_UNTIL_EXPLICIT_REMOVE,
                attributes);
    }

    public void addArtifactWithList(String pluginId, String artifactId, String version, boolean mandatory, String installScript,
                                    Artifacts.RetentionPolicy retention,
                                    List<Artifacts.AttributeValuePair> attributes) {
        addArtifact(pluginId, artifactId, version, mandatory, installScript, retention,
                attributes.toArray(new Artifacts.AttributeValuePair[attributes.size()]));
    }

    public void install(String pluginId, String artifactId, String pluginScript, String version, boolean mandatory, Artifacts.AttributeValuePair... avp) {
        addArtifact(pluginId, artifactId, version, mandatory, pluginScript, Artifacts.RetentionPolicy.REMOVE_OLDEST, avp);
    }

    public void addArtifact(String pluginId, String artifactId, String version, boolean mandatory, String installScript,
                            Artifacts.RetentionPolicy retention, Artifacts.AttributeValuePair... attributes) {

        Artifacts.ArtifactDetails.Builder detailsBuilder = Artifacts.ArtifactDetails.newBuilder();
        detailsBuilder.setArtifactId(artifactId);
        detailsBuilder.setPluginId(pluginId);
        detailsBuilder.setVersion(version);
        detailsBuilder.setScriptInstallPath(new File(installScript).getAbsolutePath());
        detailsBuilder.setSshWebAppHost(webServerHostname);
        detailsBuilder.setRetention(retention);
        detailsBuilder.setMandatory(mandatory);
        detailsBuilder.addAllAttributes(ObjectArrayList.wrap(attributes));
        if (webServerUsername != null) detailsBuilder.setSshWebAppUserName(webServerUsername);
        installationSetBuilder.addArtifacts(detailsBuilder);
    }

    @Override
    public String toString() {
        return TextFormat.printToString(installationSetBuilder.build());
    }

    public void save(File output) throws IOException {

        final FileOutputStream output1 = new FileOutputStream(output);
        installationSetBuilder.build().writeDelimitedTo(output1);
        output1.close();
    }


    /**
     * Determine if the list of requests is empty.
     *
     * @return True or False.
     */
    public boolean isEmpty() {
        return this.installationSetBuilder.getArtifactsCount() == 0;
    }

    /**
     * Register an environment collection script. Environment collection scripts tie artifacts with a
     * specific runtime environment. Registered collection scripts will be run to populate the Bash
     * environment before the get_attribute function of a plugin is called.
     *
     * @param scriptFilename
     *
     */
    public void registerEnvironmentCollection(String scriptFilename) {
        environmentCollectionScripts.add(scriptFilename);
        addArtifact(ARTIFACTS_ENVIRONMENT_COLLECTION_SCRIPT+environmentCollectionScripts.size(),"ENV_SCRIPT","1.0", true, scriptFilename);
    }

    ObjectArrayList<String> environmentCollectionScripts = new ObjectArrayList<String>();
}
