package org.campagnelab.gobyweb.artifacts.scope;

import org.campagnelab.gobyweb.artifacts.ArtifactRepo;
import org.campagnelab.gobyweb.artifacts.Artifacts;

import java.util.List;

/**
 * Include in the installation scope any artifact currently installed in the repository.
 */
public class InstalledInRepoScope implements InstallationScope {
    private ArtifactRepo repo;

    public InstalledInRepoScope(ArtifactRepo repo) {
        this.repo = repo;
    }

    @Override
    public boolean isInScope(String pluginId, String artifactId, String version) {

        List<Artifacts.Artifact> artifacts = repo.findIgnoringAttributes(pluginId, artifactId, version);
        boolean installed = false;
        for (Artifacts.Artifact artifact : artifacts) {
            if (artifact.getState() == Artifacts.InstallationState.INSTALLED) installed = true;
        }
        if (installed) {
            return true;
        } else {
            return false;
        }


    }
}
