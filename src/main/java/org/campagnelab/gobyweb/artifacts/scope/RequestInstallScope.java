package org.campagnelab.gobyweb.artifacts.scope;

import org.campagnelab.gobyweb.artifacts.Artifacts;

/**
 * Restricts installation scope to the artifact described in a request.
 */
public class RequestInstallScope implements InstallationScope {

    private final Artifacts.InstallationSet requests;

    public RequestInstallScope(Artifacts.InstallationSet request) {
        this.requests = request;
    }

    @Override
    public boolean isInScope(String pluginId, String artifactId, String version) {
        for (Artifacts.ArtifactDetails artifact : requests.getArtifactsList()) {
            if (artifact.getPluginId().equals(pluginId) && artifact.getVersion().equals(version)) {
                return true;
            }
        }
        return false;
    }
}
