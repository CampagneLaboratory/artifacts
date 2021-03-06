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

package org.campagnelab.gobyweb.artifacts.repositories;

import org.campagnelab.gobyweb.artifacts.ArtifactRepo;
import org.campagnelab.gobyweb.artifacts.Artifacts;

import java.io.IOException;

/**
 * Created by mas2182 on 8/18/15.
 */
public interface SourceRepository extends  Repository {

    boolean fetchWithLog(ArtifactRepo artifactRepo,
                      String scriptInstallPath,
                      String relativePath, String absolutePath);

    boolean fetch(String sourcePath, String targetPath) throws Exception;


}
