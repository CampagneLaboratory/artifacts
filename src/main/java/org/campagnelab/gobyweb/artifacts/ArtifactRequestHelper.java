package org.campagnelab.gobyweb.artifacts;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.artifacts.util.SyncPipe;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

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
        LOG.info("Preparing to install from request: "+getPluginNames(requests));
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
            final File tempInstallFile = File.createTempFile(request.getPluginId(),
                    FilenameUtils.getBaseName(scriptInstallPath));

            try {
                final String localFilename = tempInstallFile.getAbsolutePath();
                String username = request.hasSshWebAppUserName() ? request.getSshWebAppUserName() :
                        System.getProperty("user.name");

                String server = request.getSshWebAppHost();

                int status = scp(username, server, scriptInstallPath, localFilename);
                if (status != 0) {
                    final String message = String.format("Unable to retrieve install script for plugin %s@%s:%s %n", username,
                            server, scriptInstallPath);
                    LOG.error(message);
                    throw new IOException(message);
                }
                repo.install(request.getPluginId(), request.getArtifactId(), localFilename, request.getVersion(), avp);
                repo.setRetention(request.getPluginId(), request.getArtifactId(), request.getVersion(),
                        avp, request.getRetention());

               // printBashExports(repoDir, new PrintWriter(currentExports));
                //repo.setCurrentBashExports(currentExports.toString());
                repo.save();

                final String text = repo.toText(request.getPluginId(), request.getArtifactId(), request.getVersion(), avp);

                Artifacts.Artifact installedArtifact = repo.find(request.getPluginId(), request.getArtifactId(), request.getVersion(), avp);
                if (installedArtifact.getState() != Artifacts.InstallationState.INSTALLED) {
                    LOG.error("Early stop: unable to install previous artifact: " +
                            text);
                    return;
                } else {
                    LOG.info("Artifact successfully installed: " + text);
                }
            } catch (InterruptedException e) {
                LOG.error("An error occurred when transferring install script.", e);
            } finally {
                tempInstallFile.delete();
            }
        }
        repo.save();
    }

    private String getPluginNames(Artifacts.InstallationSet requests) {
        StringBuffer sb=new StringBuffer();
        for (Artifacts.ArtifactDetails request: requests.getArtifactsList()) {
            sb.append(repo.toText(request.getPluginId(), request.getPluginId(),
                    request.getVersion(), repo.convert(request.getAttributesList())));
            sb.append(" ");
        }
        return sb.toString();
    }

    private int scp(String username, String remoteHost, String remotePath, String localFilename) throws IOException, InterruptedException {
        return exec(String.format("scp -o StrictHostKeyChecking=no %s@%s:%s %s", username, remoteHost, remotePath, localFilename));
    }

    private int exec(String command) throws IOException, InterruptedException {
        Runtime rt = Runtime.getRuntime();
        String[] commands = command.split(" ");
        Process pr = rt.exec(commands);
        LOG.trace("executing command: " + command);
        new Thread(new SyncPipe(pr.getErrorStream(), System.err, LOG)).start();
        new Thread(new SyncPipe(pr.getInputStream(), System.out, LOG)).start();

        int exitVal = pr.waitFor();
        LOG.info("Install script exited with error code " + exitVal);
        return exitVal;
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
        LOG.debug("printBashExports");
        ArtifactRepo repo = getRepo(repoDir);
        repo.load();
        for (Artifacts.ArtifactDetails request : requests.getArtifactsList()) {
            List<Artifacts.Artifact> artifacts = repo.findIgnoringAttributes(request.getPluginId(),
                    request.getArtifactId(), request.getVersion()
            );
            for (Artifacts.Artifact artifact : artifacts) {
                //repo.convert(request.getAttributesList()
                if (artifact != null && artifact.getState() == Artifacts.InstallationState.INSTALLED) {
                    List<Artifacts.AttributeValuePair> list = artifact.getAttributesList();
                    boolean attributesInRepoMatchEnvironment = true;
                    if (!list.isEmpty()) {
                        // we must verify that the artifact attributes recorded in the repo match those
                        // needed in the specific runtime environment we are in.
                        AttributeValuePair[] avpEnvironment = repo.convert(artifact.getAttributesList());
                        AttributeValuePair[] avpRepo = repo.convert(artifact.getAttributesList());
                        for (AttributeValuePair attributeValuePair : avpEnvironment) {
                            // we null the value to collect it from the script/environment:
                            attributeValuePair.value = null;
                        }
                        Properties properties = repo.readAttributeValues(artifact, avpEnvironment);
                        if (properties == null) {
                            // we could not obtain avpEnvironment
                            continue;
                        }
                        attributesInRepoMatchEnvironment = Arrays.equals(avpEnvironment, avpRepo);
                    }

                    if (attributesInRepoMatchEnvironment) {

                        // only write exports when the attribute values obtained from the runtime env match those in the repo:
                        final AttributeValuePair[] avpPluginInRepo = repo.convert(artifact.getAttributesList());
                        output.printf("export RESOURCES_ARTIFACTS_%s_%s%s=%s%n", request.getPluginId(),
                                request.getArtifactId(), repo.listAttributeValues(artifact.getAttributesList()),
                                repo.getInstalledPath(request.getPluginId(), request.getArtifactId(), request.getVersion(),
                                        avpPluginInRepo));
                        // also write each attribute value:
                        for (Artifacts.AttributeValuePair attribute : artifact.getAttributesList()) {
                            if (attribute.getValue() != null) {
                                output.printf("export RESOURCES_ARTIFACTS_%s_%s_%s=%s%n",
                                        artifact.getPluginId(),
                                        artifact.getId(),
                                        repo.normalize(attribute.getName()),
                                        attribute.getValue());
                            }
                        }
                    }
                }
            }
        }
        output.flush();
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
     * Return the installation requests.
     *
     * @return
     */
    public Artifacts.InstallationSet getRequests() {
        return requests;
    }

    /**
     * Show the content of the request(s).
     */
    public void show() {
        System.out.println(requests.toString());
    }

    @Override
    public String toString() {
        return requests.toString();
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
