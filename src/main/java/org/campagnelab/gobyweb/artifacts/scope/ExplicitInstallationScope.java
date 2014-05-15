package org.campagnelab.gobyweb.artifacts.scope;

import java.util.HashSet;
import java.util.Set;

/**
 * Scope useful when testing. You can add specific artifacts to the scope with the add method.
 */
public class ExplicitInstallationScope implements InstallationScope {
    private Set<ArtifactInfo> artifactsInScope = new HashSet<ArtifactInfo>();

    public void addArtifact(String pluginId, String artifactId, String version) {
        artifactsInScope.add(new ArtifactInfo(pluginId, artifactId, version));
    }

    @Override
    public boolean isInScope(String pluginId, String artifactId, String version) {
        return artifactsInScope.contains(new ArtifactInfo(pluginId, artifactId, version));
    }

}
