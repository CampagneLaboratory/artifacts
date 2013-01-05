package org.campagnelab.gobyweb.artifacts;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

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

    }

    private static boolean hasError(JSAPResult config) {
        return !(config.getBoolean("install") || config.getBoolean("remove") || config.getBoolean("get-path") ||
                config.getBoolean("bash-exports") || config.getBoolean("show"));
    }

    private void process(JSAPResult config, File repoDir) throws IOException {
        long quota=config.getLong("repo-dir-quota");
        repo.setSpaceRepoDirQuota(quota);
        repo.load(repoDir);
        String[] artifacts = config.getStringArray("artifacts");
        File sshRequests = config.getFile("ssh-requests");
        if (sshRequests != null) {
            ArtifactRequestHelper helper = new ArtifactRequestHelper(sshRequests);
            helper.setRepo(repo);
            if (config.getBoolean("install")) {

                helper.install(repoDir);
            } else if (config.getBoolean("remove")) {
                helper.remove(repoDir);
            } else if (config.getBoolean("bash-exports")) {
                helper.printBashExports(repoDir);
            } else if (config.getBoolean("show")) {
                helper.show();
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
    }


    public ArtifactRepo getRepo() {
        return repo;
    }


}
