package org.campagnelab.gobyweb.artifacts;

import org.campagnelab.gobyweb.artifacts.locks.ExclusiveLockRequest;
import org.campagnelab.gobyweb.artifacts.locks.ExclusiveLockRequestWithFile;
import it.unimi.dsi.lang.MutableString;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.io.*;
import java.util.Date;

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
    public void install(String pluginId, String artifactId, String pluginScript) throws IOException {
        install(pluginId, artifactId, pluginScript, "VERSION");
    }

    /**
     * Install an artifact in the repository.
     *
     * @param pluginId     Plugin Identifier.
     * @param artifactId   Artifact identifier.
     * @param pluginScript Path to the plugin install.sh script.
     */

    public void install(String pluginId, String artifactId, String pluginScript, String version) throws IOException {
        Artifacts.Artifact artifact = find(pluginId, artifactId, version);
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
            artifactBuilder.setRelativePath(FilenameUtils.concat(pluginId, artifactId));
            artifactBuilder.setVersion(version);

            artifact = artifactBuilder.build();
            index.put(makeKey(artifact), artifact);

            save();
            try {

                runInstallScript(pluginId, artifactId, pluginScript, version);
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
    public void remove(String pluginId, String artifactId, String version) throws IOException {
        Artifacts.Artifact artifact = find(pluginId, artifactId, version);
        if (artifact == null) {
            LOG.warn(String.format("Cannot remove artifact %s:%s since it is not present in the repository.",
                    pluginId, artifactId));
            return;
        } else {
            FileUtils.deleteDirectory(getArtifactDir(pluginId, artifactId, version));
            LOG.info(String.format("Removing artifact %s:%s.",
                    pluginId, artifactId));
            index.remove(makeKey(artifact));
        }
    }

    private File getArtifactDir(String pluginId, String artifactId, String version) {
        return new File(FilenameUtils.concat(
                FilenameUtils.concat(
                        FilenameUtils.concat(
                                repoDir.getAbsolutePath(), pluginId), artifactId), version));
    }

    private void runInstallScript(String pluginId, String artifactId, String pluginScript, String version)
            throws IOException, InterruptedException {
        if (pluginScript == null) {
            return;
        }
        String installationPath = mkDirs(repoDir, pluginId, artifactId, version);
        pluginScript = new File(pluginScript).getAbsolutePath();
        String wrapperTemplate = "( set -x ; DIR=%s/%d ; script=%s; echo $DIR; mkdir -p ${DIR}; cd ${DIR}; ls -l ; " +
                " chmod +x $script ;  . $script ; plugin_install_artifact %s %s ; ls -l )%n";
        String tmpDir = System.getProperty("java.io.tmpdir");
        String cmds[] = {"/bin/bash", "-c", String.format(wrapperTemplate, tmpDir, (new Date().getTime()),
                pluginScript,
                artifactId,
                installationPath)};

        Runtime rt = Runtime.getRuntime();
        //Process pr = rt.exec("cmd /c dir");
        Process pr = rt.exec(cmds);
        BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));

        String line = null;

        while ((line = input.readLine()) != null) {
            System.out.println(line);
        }

        int exitVal = pr.waitFor();
        LOG.error("Install script exited with error code " + exitVal);
        if (exitVal != 0) {
            throw new IllegalStateException();
        }
    }

    private String mkDirs(File repoDir, String pluginId, String artifactId, String version) {
        final File dir = getArtifactDir(pluginId, artifactId, version);
        dir.mkdirs();
        return dir.getAbsolutePath();
    }

    public Artifacts.Artifact find(String pluginId, String artifactId) {
        return find(pluginId, artifactId, "VERSION");
    }

    public Artifacts.Artifact find(String pluginId, String artifactId, String version) {

        return index.get(makeKey(pluginId, artifactId, version));
    }

    public void load(File repoDir) throws IOException {
        try {
            acquireExclusiveLock();

            RandomAccessFile file = getLockedRepoFile();
            Artifacts.Repository repo;
            if (file.length() != 0) {
                LOG.info(String.format("Loading from %s%n", repoDir.getAbsolutePath()));
                repo = Artifacts.Repository.parseDelimitedFrom(new FileInputStream(file.getFD()));
                LOG.info(String.format("Loaded repo with %d artifacts. %n", repo.getArtifactsCount()));
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

        return makeKey(artifact.getPluginId(), artifact.getId(), artifact.getVersion());

    }

    private MutableString makeKey(String pluginId, String artifactId, String version) {
        MutableString key = new MutableString(pluginId);
        key.append('$');
        key.append(artifactId);
        key.append('$');
        key.append(version);
        key.compact();
        return key;
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
            LOG.info(String.format("Saving to %s %n" ,repoDir.getAbsolutePath()));
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

    public void remove(String plugin, String artifact) throws IOException {
        remove(plugin,artifact,"VERSION");
    }
}
