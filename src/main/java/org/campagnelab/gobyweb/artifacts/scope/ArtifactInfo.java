package org.campagnelab.gobyweb.artifacts.scope;

/**
 * Uniquely identified an artifact by ids and version.
 */
public class ArtifactInfo {
    String pluginId;
    String artifactId;
    String version;

    public ArtifactInfo(String pluginId, String artifactId, String version) {
        this.pluginId = pluginId;
        this.artifactId = artifactId;

        this.version = version;
    }

    @Override
    public int hashCode() {
        return artifactId.hashCode() ^ pluginId.hashCode() ^ version.hashCode();


    }

    @Override
    public boolean equals(Object o) {
        ArtifactInfo other = (ArtifactInfo) o;
        return pluginId.equals(other.pluginId) && artifactId.equals(other.artifactId) && version.equals(other.version);
    }
}
