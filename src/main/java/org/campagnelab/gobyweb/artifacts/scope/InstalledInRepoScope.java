/*
 * Copyright (c) [2012-2017] [Weill Cornell Medical College]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
