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

import edu.cornell.med.icb.net.CommandExecutor;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.artifacts.ArtifactRepo;
import org.campagnelab.gobyweb.artifacts.Artifacts;

import java.io.IOException;

/**
 * A remote plugin repository to use for artifact installations.
 *
 * Created by mas2182 on 8/18/15.
 */
public class RemoteSourceRepository implements SourceRepository {

    private static final org.apache.log4j.Logger LOG = Logger.getLogger(RemoteSourceRepository.class);

    private final String server, username;


    public RemoteSourceRepository(Artifacts.ArtifactDetails request) {
        this.username = request.hasSshWebAppUserName() ? request.getSshWebAppUserName() :
                System.getProperty("user.name");
        this.server = request.getSshWebAppHost();
    }

    public RemoteSourceRepository(String server, String username) {
        this.username = username;
        this.server = server;
    }


    @Override
    public boolean fetchWithLog(ArtifactRepo artifactRepo,
                             String scriptInstallPath,
                             String sourcePath, String targetPath) {

        final String format = String.format("Unable to retrieve install script for plugin %s@%s:%s %n", username,
                server, sourcePath);
        try {

            if (!this.fetch(scriptInstallPath,targetPath)) {
                final String message = format;
                LOG.error(message);
                artifactRepo.getStepsLogger().error(format);
                throw new IOException(message);
            }
            return true;
        } catch (Exception e) {
            final String message = format;
            LOG.error(message, e);
            artifactRepo.getStepsLogger().error(format);
        }
        return false;
    }

    @Override
    public boolean fetch(String sourcePath, String targetPath) throws Exception {
        return scp(username, server, sourcePath, targetPath) == 0;
    }


    private int scp(String username, String remoteHost, String remotePath, String localFilename) throws IOException, InterruptedException {
        final CommandExecutor commandExecutor = new CommandExecutor(username, remoteHost);
        commandExecutor.setQuiet(false);
        return commandExecutor.scpFromRemote(remotePath, localFilename);

    }
}
