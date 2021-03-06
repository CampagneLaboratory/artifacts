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

import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.artifacts.ArtifactRepo;

import java.io.IOException;
import java.nio.file.*;

/**
 * A local plugin repository to use for artifact installations.
 *
 * Created by mas2182 on 8/18/15.
 */
public class LocalSourceRepository implements SourceRepository {

    private static final org.apache.log4j.Logger LOG = Logger.getLogger(LocalSourceRepository.class);


    public LocalSourceRepository() {
    }

    @Override
    public boolean fetchWithLog(ArtifactRepo artifactRepo,
                             String scriptInstallPath,
                             String sourcePath, String targetPath) {
            final String format = String.format("Unable to locally retrieve install script for plugin %s %n", sourcePath);
            try {
                this.fetch(scriptInstallPath,targetPath);
                return true;
            } catch (IOException e) {
                final String message = format;
                LOG.error(message, e);
                artifactRepo.getStepsLogger().error(format);
            }
           return false;
    }

    @Override
    public boolean fetch(String sourcePath, String targetPath) throws IOException {
        Path source = Paths.get(sourcePath);
        Path target = Paths.get(targetPath);
        //overwrite existing file, if exists
        CopyOption[] options = new CopyOption[]{
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES
        };
        Files.copy(source, target, options);

        return true;
    }
}
