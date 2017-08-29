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
