package org.campagnelab.gobyweb.artifacts;

import edu.cornell.med.icb.net.CommandExecutor;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.artifacts.scope.RequestInstallScope;

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
    private boolean earlyStopRequested;

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
          install(repoDir,false);
    }
    /**
     * Install artifacts described in a request, obtaining install scripts as needed from remote web app server.
     *
     * @param repoDir Repository directory.
     * @param onlyMandatory if true, only mandatory artifacts are installed.
     * @throws IOException
     */
    public void install(File repoDir, boolean onlyMandatory) throws IOException {
        ArtifactRepo repo = getRepo(repoDir);
        repo.unregisterAllEnvironmentCollectionScripts();
        StringWriter currentExports = new StringWriter();
        final String message1 = "Preparing to install from request: " + getPluginNames(requests);
        repo.getStepsLogger().step(message1);
        LOG.info(message1);
        try {
            repo.acquireExclusiveLock();
            // restricts exports used during installation to the artifacts that are part of this request:
            repo.setInstallationScope(new RequestInstallScope(requests));
            for (Artifacts.ArtifactDetails request : requests.getArtifactsList()) {
                repo.load();
                //LOG.info("Processing install request: " + request.toString());
                repo.getStepsLogger().step("Processing install request: " + request.toString());
                if (onlyMandatory && !request.getMandatory()) {
                    repo.getStepsLogger().step("Skipping non-mandatory install request: " + request.toString());
                    continue;
                }
                String username = request.hasSshWebAppUserName() ? request.getSshWebAppUserName() :
                        System.getProperty("user.name");
                final String remoteScriptInstallPath = request.getScriptInstallPath();
                File tmpLocalInstallScript = null;
                try {
                    tmpLocalInstallScript = getCachedInstallFile(remoteScriptInstallPath, request.getPluginId(), request.getVersion(),
                            username, request.getSshWebAppHost());

                } catch (InterruptedException e) {

                    final String message = "Unable to retrieve cached install file for plugin: " + repo.toText(request.getPluginId(), request.getArtifactId(),
                            request.getVersion(), repo.convert(request.getAttributesList()));
                    LOG.error(message);
                    repo.getStepsLogger().error(message);
                    tmpLocalInstallScript = null;
                }
                final AttributeValuePair[] avp = repo.convert(request.getAttributesList());
                Artifacts.Artifact artifact = repo.find(request.getPluginId(), request.getArtifactId(), request.getVersion(),
                        avp);
                if (artifact != null && artifact.getState() == Artifacts.InstallationState.INSTALLED) {

                    LOG.info(String.format("Artifact already installed, skipping %s:%s:%s ",
                            request.getPluginId(), request.getArtifactId(), request.getVersion()));
                    // even when already installed, scan for possible env script
                    repo.registerPossibleEnvironmentCollection(artifact);
                    continue;
                }


                try {


                    final String localFilename = tmpLocalInstallScript.getAbsolutePath();
                    repo.install(request.getPluginId(), request.getArtifactId(), localFilename, request.getVersion(), avp);
                    repo.setRetention(request.getPluginId(), request.getArtifactId(), request.getVersion(),
                            avp, request.getRetention());

                    repo.save();

                    final String text = repo.toText(request.getPluginId(), request.getArtifactId(), request.getVersion(), avp);

                    Artifacts.Artifact installedArtifact = repo.find(request.getPluginId(), request.getArtifactId(), request.getVersion(), avp);
                    repo.updateArtifact(installedArtifact.toBuilder().setInstallationRequest(request).build());

                    if (installedArtifact.getState() != Artifacts.InstallationState.INSTALLED) {
                        LOG.error("Early stop: unable to install previous artifact: " +
                                text);
                        earlyStopRequested = true;

                        return;
                    } else {
                        LOG.info("Artifact successfully installed: " + text);
                        repo.getStepsLogger().step("Artifact successfully installed: " + text);
                    }

                } finally {
                    if (tmpLocalInstallScript != null) {
                        tmpLocalInstallScript.delete();
                    }
                }
            }

        } finally {
            repo.save();
            repo.releaseLock();
        }
    }



    public boolean isEarlyStopRequested() {
        return earlyStopRequested;
    }

    public File getCachedInstallFile(String remoteScriptInstallPath, String pluginId, String version,
                                     String username, String server)
            throws IOException, InterruptedException {
        if (repo.hasCachedInstallationScript(pluginId, version)) {
            return new File(repo.getCachedInstallationScript(pluginId, version));
        } else {
            final File tempInstallFile = File.createTempFile("install-script-" + pluginId,
                    FilenameUtils.getBaseName(remoteScriptInstallPath));

            final String localFilename = tempInstallFile.getAbsolutePath();

            int status = scp(username, server, remoteScriptInstallPath, localFilename);
            if (status != 0) {
                final String message = String.format("Unable to retrieve install script for plugin %s@%s:%s %n", username,
                        server, remoteScriptInstallPath);
                LOG.error(message);
                repo.getStepsLogger().error(message);
                throw new IOException(message);
            }
            return tempInstallFile;
        }
    }

    public static void fetchInstallScript(Artifacts.Artifact artifact, Artifacts.ArtifactDetails request,
                                          ArtifactRepo artifactRepo) {


        String relativePath = artifact.getInstallScriptRelativePath();
        String absolutePath = artifactRepo.absolutePathInRepo("scripts", relativePath);
        LOG.info(String.format("Refetching install script for %s:%s, will install to %s  %n", artifact.getPluginId(),
                artifact.getId(), absolutePath));
        String username = request.hasSshWebAppUserName() ? request.getSshWebAppUserName() :
                System.getProperty("user.name");
        String server = request.getSshWebAppHost();
        final String format = String.format("Unable to retrieve install script for plugin %s@%s:%s %n", username,
                server, relativePath);
        try {
            int status = scp(username, server, request.getScriptInstallPath(), absolutePath);
            if (status != 0) {
                final String message = format;
                LOG.error(message);
                artifactRepo.getStepsLogger().error(format);
                throw new IOException(message);
            }
        } catch (InterruptedException e) {
            final String message = format;
            LOG.error(message, e);
            artifactRepo.getStepsLogger().error(format);
        } catch (IOException e) {
            final String message = format;
            LOG.error(message, e);
            artifactRepo.getStepsLogger().error(format);
        }
    }

    private String getPluginNames(Artifacts.InstallationSet requests) {
        StringBuffer sb = new StringBuffer();
        for (Artifacts.ArtifactDetails request : requests.getArtifactsList()) {
            sb.append(repo.toText(request.getPluginId(), request.getArtifactId(),
                    request.getVersion(), repo.convert(request.getAttributesList())));
            sb.append(" ");
        }
        return sb.toString();
    }

    private static int scp(String username, String remoteHost, String remotePath, String localFilename) throws IOException, InterruptedException {
        final CommandExecutor commandExecutor = new CommandExecutor(username, remoteHost);
        commandExecutor.setQuiet(false);
        return commandExecutor.scpFromRemote(remotePath, localFilename);

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
        getRepo(repoDir).printBashExports(new PrintWriter(new OutputStreamWriter(System.out)));
        // printBashExports(repoDir, new PrintWriter(new OutputStreamWriter(System.out)));
    }

    /**
     * Print BASH export statements for all artifacts in the request that are INSTALLED in the repository.
     *
     * @param repoDir Directory where the repository is kept.
     * @throws IOException
     */
    public void printBashExports(File repoDir, PrintWriter output) throws IOException {
      //  getRepo(repoDir).printBashExports(output);
        LOG.debug("printBashExports");
        ArtifactRepo repo = getRepo(repoDir);
        repo.load();
        List<Artifacts.ArtifactDetails> artifactsList = requests.getArtifactsList();
        for (Artifacts.ArtifactDetails request : artifactsList) {
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
            repo.setInstallationScope(new RequestInstallScope(requests));
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
