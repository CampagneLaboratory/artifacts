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
