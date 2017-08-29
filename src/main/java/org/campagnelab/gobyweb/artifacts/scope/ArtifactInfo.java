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
