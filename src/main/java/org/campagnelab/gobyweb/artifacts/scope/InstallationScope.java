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
