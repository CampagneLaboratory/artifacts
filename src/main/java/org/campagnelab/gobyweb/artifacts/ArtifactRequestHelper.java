package org.campagnelab.gobyweb.artifacts;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.campagnelab.groovySupport.ExecAndRemote;

import java.io.*;
import java.util.List;

/**
 * Helps execute artifact requests against a repository.
 *
 * @author Fabien Campagne
 *         Date: 12/19/12
 *         Time: 12:15 PM
 */
public class ArtifactRequestHelper {
    private static final org.apache.log4j.Logger LOG = Logger.getLogger(ArtifactRequestHelper.class);

    private final Artifacts.InstallationSet requests;
    private ArtifactRepo repo;
    /**
     * The repository quota. This field is used when a new repo is created, but not if you set a repo directly.
     */
    private long spaceRepoDirQuota;

    public ArtifactRequestHelper(File pbRequestFile) throws IOException {

        requests = Artifacts.InstallationSet.parseDelimitedFrom(new FileInputStream(pbRequestFile));
    }

    final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    /**
     * Install artifacts described in a request, obtaining install scripts as needed from remote web app server.
     *
     * @param repoDir Repository directory.
     * @throws IOException
     */
    public void install(File repoDir) throws IOException {
        ArtifactRepo repo = getRepo(repoDir);

        StringWriter currentExports = new StringWriter();
        for (Artifacts.ArtifactDetails request : requests.getArtifactsList()) {
            repo.load();
            LOG.info("Processing install request: " + request.toString());

            final AttributeValuePair[] avp = repo.convert(request.getAttributesList());
            Artifacts.Artifact artifact = repo.find(request.getPluginId(), request.getArtifactId(), request.getVersion(),
                    avp);
            if (artifact != null && artifact.getState() == Artifacts.InstallationState.INSTALLED) {
                LOG.info(String.format("Artifact already installed, skipping %s:%s:%s ",
                        request.getPluginId(), request.getArtifactId(), request.getVersion()));

                continue;
            }
            final String scriptInstallPath = request.getScriptInstallPath();
            final String localFilename = FilenameUtils.concat(TEMP_DIR, FilenameUtils.getBaseName(scriptInstallPath));

            String username = request.hasSshWebAppUserName() ? request.getSshWebAppUserName() :
                    System.getProperty("user.name");

            String server = request.getSshWebAppHost();
            int status = executor.scp(String.format("%s@%s:%s", username, server, scriptInstallPath), localFilename);
            if (status != 0) {
                final String message = String.format("Unable to retrieve install script for plugin %s@%s:%s %n", username,
                        server, scriptInstallPath);
                LOG.error(message);
                throw new IOException(message);
            }
            repo.install(request.getPluginId(), request.getArtifactId(), localFilename, request.getVersion(), avp);
            repo.setRetention(request.getPluginId(), request.getArtifactId(), request.getVersion(),
                    avp, request.getRetention());
            // remove the local script from $TEMP_DIR:
            new File(localFilename).delete();
            printBashExports(repoDir, new PrintWriter(currentExports));
            repo.setCurrentBashExports(currentExports.toString());
            repo.save();
        }
        repo.save();
    }

    /**
     * Remove artifacts described in a request. Please note that this method removes artifacts irrespective of
     * attributes, because requests typically do not specify attribute values and since we currently do not store
     * the artifact install script, we cannot query the script for attribute values.
     *
     * @param repoDir Repository directory.
     * @throws IOException
     */
    public void remove(File repoDir) throws IOException {
        ArtifactRepo repo = getRepo(repoDir);
        repo.load();
        for (Artifacts.ArtifactDetails request : requests.getArtifactsList()) {
            List<Artifacts.Artifact> artifacts = repo.findIgnoringAttributes(request.getPluginId(), request.getArtifactId(), request.getVersion()
            );
            for (Artifacts.Artifact artifact : artifacts) {
                //repo.convert(request.getAttributesList()
                if (artifact != null && artifact.getState() == Artifacts.InstallationState.INSTALLED) {

                    repo.remove(request.getPluginId(), request.getArtifactId(), request.getVersion(),
                            repo.convert(artifact.getAttributesList()));
                }
                repo.save();
            }
        }
    }

    private ExecAndRemote executor = new ExecAndRemote();

    /**
     * Print BASH export statements for all artifacts in the request that are INSTALLED in the repository.
     * This method prints to standard out.
     *
     * @param repoDir Directory where the repository is kept.
     * @throws IOException
     */

    public void printBashExports(File repoDir) throws IOException {
        printBashExports(repoDir, new PrintWriter(new OutputStreamWriter(System.out)));
    }

    /**
     * Print BASH export statements for all artifacts in the request that are INSTALLED in the repository.
     *
     * @param repoDir Directory where the repository is kept.
     * @throws IOException
     */
    public void printBashExports(File repoDir, PrintWriter output) throws IOException {
        ArtifactRepo repo = getRepo(repoDir);
        repo.load();
        for (Artifacts.ArtifactDetails request : requests.getArtifactsList()) {
            List<Artifacts.Artifact> artifacts = repo.findIgnoringAttributes(request.getPluginId(), request.getArtifactId(), request.getVersion()
            );
            for (Artifacts.Artifact artifact : artifacts) {
                //repo.convert(request.getAttributesList()
                if (artifact != null && artifact.getState() == Artifacts.InstallationState.INSTALLED) {

                    output.printf("export RESOURCES_ARTIFACTS_%s_%s%s=%s%n", request.getPluginId(),
                            request.getArtifactId(), listAttributeValues(artifact.getAttributesList()),
                            repo.getInstalledPath(request.getPluginId(), request.getArtifactId(), request.getVersion(),
                                    repo.convert(artifact.getAttributesList())));
                }
            }
        }
        output.flush();
    }

    private String listAttributeValues(List<Artifacts.AttributeValuePair> attributesList) {
        StringBuffer sb = new StringBuffer();

        for (Artifacts.AttributeValuePair valuePairs : attributesList) {
            if (valuePairs.getValue().length() > 0) {

                sb.append("_");
                sb.append(valuePairs.getValue());
            }
        }
        return sb.toString();
    }

    public void setRepo(ArtifactRepo repo) {
        this.repo = repo;

    }

    /**
     * Get the respository instance for a repository stored in the argument directory.
     *
     * @param repoDir Directory where the repository is kept.
     * @throws IOException
     */
    public ArtifactRepo getRepo(File repoDir) {
        if (repo != null)
            return repo;
        else {
            repo = new ArtifactRepo(repoDir);
            repo.setSpaceRepoDirQuota(spaceRepoDirQuota);
            return repo;
        }

    }

    /**
     * Show the content of the request(s).
     */
    public void show() {
        System.out.println(requests.toString());
    }

    public void setSpaceRepoDirQuota(long spaceRepoDirQuota) {
        this.spaceRepoDirQuota = spaceRepoDirQuota;
    }

    /**
     * Prune the repository.
     *
     * @param repo Directory where the repository is kept.
     * @throws IOException
     */
    public void prune(File repo) throws IOException {
        getRepo(repo).prune();
    }

    /**
     * Show the content of the repository.
     *
     * @param repo Directory where the repository is kept.
     * @throws IOException
     */
    public void showRepo(File repo) throws IOException {
        getRepo(repo).show();
    }
}
