package org.campagnelab.gobyweb.artifacts;

import com.google.protobuf.TextFormat;
import org.campagnelab.gobyweb.artifacts.locks.ExclusiveLockRequest;
import org.campagnelab.gobyweb.artifacts.locks.ExclusiveLockRequestWithFile;
import it.unimi.dsi.lang.MutableString;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.io.*;
import java.net.InetAddress;
import java.util.Date;
import java.util.List;

/**
 * The artifact repository. Provides methods to install and remove artifacts from the repository, and to
 * keep metadata about the installations.
 *
 * @author Fabien Campagne
 *         Date: 12/16/12
 *         Time: 12:12 PM
 */
public class ArtifactRepo {
    private static final org.apache.log4j.Logger LOG = Logger.getLogger(ArtifactRepo.class);
    private static final long WAIT_FOR_INSTALLING_DELAY = 30 * 1000; // wait 30 secs.
    File repoDir;
    private String metaDataFilename = "metadata.pb";


    public ArtifactRepo(File repoDir) {
        this.repoDir = repoDir;
    }

    /**
     * Install an artifact in the repository. Convenience method that does not run an install script. Only useful
     * in practice to test that the system works.
     *
     * @param pluginId   Plugin Identifier.
     * @param artifactId Artifact identifier.
     */
    public void install(String pluginId, String artifactId) throws IOException {
        install(pluginId, artifactId, null, "VERSION");
    }

    /**
     * Install an artifact in the repository. Convenience method that does not run an install script. Only useful
     * in practice to test that the system works.
     *
     * @param pluginId   Plugin Identifier.
     * @param artifactId Artifact identifier.
     */
    public void install(String pluginId, String artifactId, String pluginScript, AttributeValuePair... avp) throws IOException {
        install(pluginId, artifactId, pluginScript, "VERSION", avp);
    }

    /**
     * Install an artifact in the repository.
     *
     * @param pluginId     Plugin Identifier.
     * @param artifactId   Artifact identifier.
     * @param pluginScript Path to the plugin install.sh script.
     */

    public void install(String pluginId, String artifactId, String pluginScript, String version, AttributeValuePair... avp) throws IOException {

        Artifacts.Artifact artifact = find(pluginId, artifactId, version, avp);
        while (artifact != null && artifact.getState() == Artifacts.InstallationState.INSTALLING) {
            try {
                LOG.info("waiting while other installing.. ");

                // wait a bit

                Thread.sleep(WAIT_FOR_INSTALLING_DELAY);

                // reload the plugin info from disk:
                load();
                artifact = find(pluginId, artifactId, version, avp);

            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting for other process to complete installation.");
            }
        }

        if (artifact != null && artifact.getState() == Artifacts.InstallationState.INSTALLED) {
            return;
        }
        if (artifact == null) {
            // create the new artifact, register in the index:
            Artifacts.Artifact.Builder artifactBuilder = Artifacts.Artifact.newBuilder();
            artifactBuilder.setId(artifactId);
            artifactBuilder.setPluginId(pluginId);
            artifactBuilder.setState(Artifacts.InstallationState.INSTALLING);
            artifactBuilder.setInstallationTime(new Date().getTime());
            artifactBuilder.setRelativePath(appendKeyValuePairs(FilenameUtils.concat(FilenameUtils.concat(pluginId, artifactId), version), avp));
            artifactBuilder.setVersion(version);
            for (AttributeValuePair valuePair : avp) {
                artifactBuilder.addAttributes(Artifacts.AttributeValuePair.newBuilder().setValue(valuePair.value).setAttribute(valuePair.attribute));
            }
            Artifacts.Host.Builder hostBuilder = Artifacts.Host.newBuilder();

            hostBuilder.setHostName(InetAddress.getLocalHost().getHostName());
            hostBuilder.setOsArchitecture(System.getProperty("os.arch"));
            hostBuilder.setOsName(System.getProperty("os.name"));
            hostBuilder.setOsVersion(System.getProperty("os.version"));
            artifactBuilder.setInstallationHost(hostBuilder);
            artifact = artifactBuilder.build();
            index.put(makeKey(artifact), artifact);

            save();
            try {

                runInstallScript(pluginId, artifactId, pluginScript, version, avp);
                changeState(artifact, Artifacts.InstallationState.INSTALLED);
            } catch (RuntimeException e) {
                changeState(artifact, Artifacts.InstallationState.FAILED);
            } catch (Exception e) {
                changeState(artifact, Artifacts.InstallationState.FAILED);
            } catch (Error e) {
                changeState(artifact, Artifacts.InstallationState.FAILED);
            }

            save();
        }
    }


    private void changeState(Artifacts.Artifact artifact, Artifacts.InstallationState installed) throws IOException {
        Artifacts.Artifact.Builder artifactBuilder = artifact.toBuilder().setState(Artifacts.InstallationState.INSTALLED);
        artifact = artifactBuilder.build();
        index.put(makeKey(artifact), artifact);
        save();
    }

    /**
     * Remove an artifact from the repository.
     *
     * @param pluginId
     * @param artifactId
     */
    public void remove(String pluginId, String artifactId, String version, AttributeValuePair... avp) throws IOException {
        Artifacts.Artifact artifact = find(pluginId, artifactId, version,avp);
        if (artifact == null) {
            LOG.warn(String.format("Cannot remove artifact %s:%s since it is not present in the repository.",
                    pluginId, artifactId));
            return;
        } else {
            FileUtils.deleteDirectory(getArtifactDir(pluginId, artifactId, version, avp));
            LOG.info(String.format("Removing artifact %s:%s.",
                    pluginId, artifactId));
            index.remove(makeKey(artifact));
        }
    }

    private String appendKeyValuePairs(String artifactInstallDir, AttributeValuePair[] avp) {
        String result = artifactInstallDir;
        for (AttributeValuePair valuePair : avp) {
            result = FilenameUtils.concat(result, normalize(valuePair.value));
        }
        return result;
    }

    private File getArtifactDir(String pluginId, String artifactId, String version, AttributeValuePair[] avp) {

        return new File(appendKeyValuePairs(FilenameUtils.concat(
                FilenameUtils.concat(
                        FilenameUtils.concat(
                                repoDir.getAbsolutePath(), pluginId), artifactId), version),
                avp));
    }

    private void runInstallScript(String pluginId, String artifactId, String pluginScript, String version, AttributeValuePair[] avp)
            throws IOException, InterruptedException {
        if (pluginScript == null) {
            return;
        }
        String installationPath = mkDirs(repoDir, pluginId, artifactId, version, avp);
        pluginScript = new File(pluginScript).getAbsolutePath();
        String wrapperTemplate = "( set -x ; DIR=%s/%d ; script=%s; echo $DIR; mkdir -p ${DIR}; cd ${DIR}; ls -l ; " +
                " chmod +x $script ;  . $script ; plugin_install_artifact %s %s ; ls -l )%n";
        String tmpDir = System.getProperty("java.io.tmpdir");
        String cmds[] = {"/bin/bash", "-c", String.format(wrapperTemplate, tmpDir, (new Date().getTime()),
                pluginScript,
                artifactId,
                installationPath)};

        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(cmds);
        BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        BufferedReader error = new BufferedReader(new InputStreamReader(pr.getErrorStream()));

        String line = null;

        while ((line = input.readLine()) != null) {
            System.out.println(line);
        }
        while ((line = error.readLine()) != null) {
            System.err.println(line);
        }
        int exitVal = pr.waitFor();
        LOG.error("Install script exited with error code " + exitVal);
        System.out.println("Install script exited with error code " + exitVal);
        if (exitVal != 0) {
            throw new IllegalStateException();
        }
    }


    private String mkDirs(File repoDir, String pluginId, String artifactId, String version, AttributeValuePair... avp) {
        final File dir = getArtifactDir(pluginId, artifactId, version, avp);
        dir.mkdirs();
        return dir.getAbsolutePath();
    }

    public Artifacts.Artifact find(String pluginId, String artifactId, AttributeValuePair[] avp) {
        return find(pluginId, artifactId, "VERSION", avp);
    }

    public Artifacts.Artifact find(String pluginId, String artifactId) {

        return find(pluginId, artifactId, "VERSION", new AttributeValuePair[0]);
    }

    public Artifacts.Artifact find(String pluginId, String artifactId, String version) {

        return find(pluginId, artifactId, version, new AttributeValuePair[0]);
    }

    public Artifacts.Artifact find(String pluginId, String artifactId, String version, AttributeValuePair[] avp) {

        return index.get(makeKey(pluginId, artifactId, version, avp));
    }

    public void load(File repoDir) throws IOException {
        try {
            acquireExclusiveLock();

            RandomAccessFile file = getLockedRepoFile();
            Artifacts.Repository repo;
            if (file.length() != 0) {
                LOG.trace(String.format("Loading from %s%n", repoDir.getAbsolutePath()));
                repo = Artifacts.Repository.parseDelimitedFrom(new FileInputStream(file.getFD()));
                LOG.trace(String.format("Loaded repo with %d artifacts. %n", repo.getArtifactsCount()));
            } else {
                // creating new repo:
                repo = Artifacts.Repository.getDefaultInstance();

            }
            scan(repo);
        } finally {
            releaseLock();
        }
    }

    /**
     * scan and index a freshly loaded repo
     */

    private void scan(Artifacts.Repository repo) {
        index.clear();
        for (Artifacts.Artifact artifact : repo.getArtifactsList()) {
            final MutableString key;
            key = makeKey(artifact);
            index.put(key, artifact);
        }
    }

    private MutableString makeKey(Artifacts.Artifact artifact) {

        return makeKey(artifact.getPluginId(), artifact.getId(), artifact.getVersion(), convert(artifact.getAttributesList()));

    }

    private AttributeValuePair[] convert(List<Artifacts.AttributeValuePair> attributesList) {

        AttributeValuePair[] avp = new AttributeValuePair[attributesList.size()];
        int index = 0;
        for (Artifacts.AttributeValuePair a : attributesList) {
            avp[index++] = new AttributeValuePair(a.getAttribute(), a.getValue());
        }
        return avp;
    }

    private MutableString makeKey(String pluginId, String artifactId, String version, AttributeValuePair[] avp) {
        MutableString key = new MutableString(pluginId);
        key.append('$');
        key.append(artifactId);
        key.append('$');
        key.append(version);
        for (AttributeValuePair valuePair : avp) {
            key.append('$');
            key.append(normalize(valuePair.attribute));
            key.append('=');
            key.append(normalize(valuePair.value));
        }
        key.compact();
        return key;
    }

    /**
     * Normalize a string to make it usable in directory names.
     *
     * @param attribute
     * @return
     */
    private String normalize(String attribute) {
        return attribute.replaceAll(" ", "_").toUpperCase();
    }

    private Object2ObjectOpenHashMap<MutableString, Artifacts.Artifact> index = new Object2ObjectOpenHashMap<MutableString, Artifacts.Artifact>();

    public void save() throws IOException {
        save(repoDir);
    }

    public void save(File repoDir) throws IOException {

        FileOutputStream output = null;
        try {
            acquireExclusiveLock();
            RandomAccessFile file = getLockedRepoFile();
            LOG.info(String.format("Saving to %s %n", repoDir.getAbsolutePath()));
            output = new FileOutputStream(file.getFD());
            // recreate the ProtoBuf repo from the index:
            Artifacts.Repository.Builder repoBuilder = Artifacts.Repository.newBuilder();
            repoBuilder.addAllArtifacts(index.values());
            Artifacts.Repository repo = repoBuilder.build();

            repo.writeDelimitedTo(output);
            LOG.info(String.format("Wrote repo with %d artifacts.%n", repo.getArtifactsCount()));

        } finally {
            releaseLock();
            if (output != null) {
                output.close();
            }
        }
    }

    private ExclusiveLockRequest request;

    public void acquireExclusiveLock() throws IOException {

        request = new ExclusiveLockRequestWithFile(metaDataFilename, repoDir);
        boolean done = false;
        do {
            request.waitAndLock();

            if (request.granted()) {
                done = true;
            } else {
                // wait a bit.
                try {
                    Thread.currentThread().sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } while (!done);
    }

    public RandomAccessFile getLockedRepoFile() {
        return request.getLockedFile();
    }

    public void releaseLock() throws IOException {
        request.release();
    }

    public void load() throws IOException {
        load(repoDir);
    }

    public void remove(String plugin, String artifact, AttributeValuePair ... avp) throws IOException {
        remove(plugin, artifact, "VERSION",avp);
    }

    public String getInstalledPath(String pluginId, String artifactId) {
        return getInstalledPath(pluginId, artifactId, "VERSION");
    }

    public String getInstalledPath(String pluginId, String artifactId, String version) {
        Artifacts.Artifact artifact = find(pluginId, artifactId, version);
        if (artifact == null) {
            System.err.printf("Artifact %s:%s:%s could not be found. %n ", pluginId, artifactId, version);
            return null;
        } else {
            return FilenameUtils.concat(repoDir.getAbsolutePath(), artifact.getRelativePath());
        }
    }

    public void show() throws IOException {
        load();
        Artifacts.Repository.Builder repoBuilder = Artifacts.Repository.newBuilder();
        repoBuilder.addAllArtifacts(index.values());
        Artifacts.Repository repo = repoBuilder.build();
        TextFormat.print(repo, System.out);
    }
}
