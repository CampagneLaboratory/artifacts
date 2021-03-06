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

package org.campagnelab.gobyweb.artifacts;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import org.apache.log4j.Logger;

import java.io.*;

/**
 * Main class of the GobyWeb artifact manager.
 */
public class ArtifactManager {
    private static final org.apache.log4j.Logger LOG = Logger.getLogger(ArtifactManager.class);

    ArtifactRepo repo;

    public ArtifactManager(String repoDir) throws IOException {
        this(new File(repoDir));

    }

    public ArtifactManager(File repoDir) throws IOException {
        repo = new ArtifactRepo(repoDir);
        repo.load(repoDir);
    }

    public static JSAPResult loadJsapConfig(String commandLine) throws IOException, JSAPException {
        JSAP jsap = new JSAP(ArtifactManager.class.getResource("ArtifactManager.jsap"));
        return jsap.parse(commandLine);

    }

    public static void main(String[] args) throws Exception {
        JSAP jsap = new JSAP(ArtifactManager.class.getResource("ArtifactManager.jsap"));

        JSAPResult config = jsap.parse(args);

        if (!config.success() || config.getBoolean("help") || hasError(config)) {

            // print out the help, then specific error messages describing the problems
            // with the command line, THEN print usage.
            //  This is called "beating the user with a clue stick."

            System.err.println(jsap.getHelp());

            for (java.util.Iterator errs = config.getErrorMessageIterator();
                 errs.hasNext(); ) {
                System.err.println("Error: " + errs.next());
            }

            System.err.println();
            System.err.println("Usage: java "
                    + ArtifactManager.class.getName());
            System.err.println("                "
                    + jsap.getUsage());
            System.err.println();

            System.exit(1);
        }
        File repoDir = config.getFile("repository");
        ArtifactManager processor = new ArtifactManager(repoDir);
        processor.process(config, repoDir);
        System.exit(0);
    }

    private static boolean hasError(JSAPResult config) {
        return !(config.getBoolean("install") || config.getBoolean("remove") || config.getBoolean("get-path") ||
                config.getBoolean("bash-exports") || config.getBoolean("show") || config.getBoolean("show-repo") ||
                config.getBoolean("fail-installing"));
    }

    private void process(JSAPResult config, File repoDir) throws IOException {
        long quota = config.getLong("repo-dir-quota");
        repo.setSpaceRepoDirQuota(quota);
        repo.setStepLogDir(config.getFile("log-dir"));
        String[] artifacts = config.getStringArray("artifacts");
        File sshRequests = config.getFile("ssh-requests");
        try {
            if (config.getBoolean("fail-installing")) {
                failInstalling();
                return;
            }
            repo.load(repoDir);
            if (sshRequests != null) {
                ArtifactRequestHelper helper = new ArtifactRequestHelper(sshRequests);
                helper.setRepo(repo);
                if (config.getBoolean("install")) {

                    if (config.userSpecified("installation-type")
                            && config.getString("installation-type").equals("only-mandatory")) {
                        helper.install(repoDir,true);
                    } else
                        helper.install(repoDir, false);
                    if (helper.isEarlyStopRequested()) {
                        repo.writeLog();
                        System.exit(10);
                    }
                } else if (config.getBoolean("remove")) {
                    helper.remove(repoDir);
                } else if (config.getBoolean("bash-exports")) {
                    String output = config.getString("output");
                    if (output == null) {
                        output = "./exports.sh";
                    }
                    helper.printBashExports(repoDir, new PrintWriter(new FileWriter(output)));
                } else if (config.getBoolean("show")) {
                    helper.show();
                } else if (config.getBoolean("show-repo")) {
                    helper.showRepo(repoDir);
                }
            }
            if (artifacts == null) {
                artifacts = new String[0];
            }
            {
                if (config.getBoolean("show")) {
                    repo.show();
                }
                for (String a : artifacts) {
                    String tokens[] = a.split(":");
                    if (tokens.length < 3) {
                        System.err.println("Error parsing artifact description, format must be PLUGIN_ID:ARTIFACT_ID:VERSION:path-to-install-script, instead got " + a);
                        System.exit(1);
                    }

                    String pluginId = tokens[0];
                    String artifactId = tokens[1];
                    String version = tokens[2];
                    String installScript = tokens.length >= 4 ? tokens[3] : null;

                    if (config.getBoolean("install")) {
                        repo.install(pluginId, artifactId, installScript, version);
                    } else if (config.getBoolean("remove")) {
                        repo.remove(pluginId, artifactId, version);
                    } else if (config.getBoolean("get-path")) {
                        System.out.println(repo.getInstalledPath(pluginId, artifactId, version));
                    }
                }
                repo.save(repoDir);
            }
        } finally {
            repo.writeLog();
        }
    }

    public void failInstalling() throws IOException {
        // do not update export statements upon loading, we are not installing:
        repo.load(false);
        for (Artifacts.Artifact artifact : repo.getArtifacts()) {
            if (artifact.getState() == Artifacts.InstallationState.INSTALLING) {
                try {
                    final Artifacts.Artifact revisedArtifact = artifact.toBuilder().setState(Artifacts.InstallationState.FAILED).build();
                    repo.updateArtifact(revisedArtifact);
                    System.out.println("failed plugin: " + repo.toTextShort(artifact));
                } catch (IOException e) {
                    LOG.error("An IO exception occurred when failing " + repo.toText(artifact));
                }
            }
        }
    }


    public ArtifactRepo getRepo() {
        return repo;
    }


}
