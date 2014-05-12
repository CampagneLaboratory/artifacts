package org.campagnelab.gobyweb.artifacts.scope;

/**
 * Determines if an artifact is part of the installation scope.
 *
 */
public interface InstallationScope {
    /**
     * Returns true when the artifact identified by id and version is in the installation scope and false otherwise.
     * Artifacts in the installation scope will be presented to artifact install scripts via export statements.
     * @param pluginId Id of the plugin that requires the artifact.
     * @param artifactId Id of the artifact
     * @param version version of the artifact
     * @return True when in the installation scope.
     */
    public boolean isInScope(String pluginId, String artifactId, String version);

}
