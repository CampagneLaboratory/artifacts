package org.campagnelab.gobyweb.artifacts;

import com.google.protobuf.TextFormat;
import edu.cornell.med.icb.net.SyncPipe;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.lang.MutableString;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.artifacts.locks.ExclusiveLockRequest;
import org.campagnelab.gobyweb.artifacts.locks.ExclusiveLockRequestWithFile;

import java.io.*;
import java.net.InetAddress;
import java.util.*;

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
    /**
     * The number of locks acquired on the repository.  When >0, at least one lock has been granted.
     * When 0, no lock has been granted.
     */
    int lockCount = 0;

    File repoDir;
    /**
     * Sort artifacts by increasing installation date
     */
    private static final Comparator<Artifacts.Artifact> SORT_BY_INSTALLED_DATE = new Comparator<Artifacts.Artifact>() {
        public int compare(Artifacts.Artifact artifact1, Artifacts.Artifact artifact2) {
            return (int) (artifact1.getInstallationTime() - artifact2.getInstallationTime());
        }
    };
    /**
     * Percentage that determines when to prune artifacts in the repo. Pruning may occur when free space is less
     * than PERCENT_SPACE_THRESHOLD % of the available space in the file system that holds the repo.
     */
    private static final float PERCENT_SPACE_THRESHOLD = 10.0f;
    private String metaDataFilename = "metadata.pb";
    /**
     * The number of bytes still available in the filesystem that holds the repository directory.
     */
    private long spaceAvailableInRepoDir;
    /**
     * The maximum number of bytes available in the filesystem that holds the repository directory.
     */
    private long spaceMaxAvailableInRepoDir;
    private MutableString currentBashExports = new MutableString();
    private MutableString preInstalledPluginExports = new MutableString();
    private boolean quiet = true;
    private Object2ObjectMap<String, String> pluginIdToInstallScriptPath = new Object2ObjectOpenHashMap<String, String>();

    public long getSpaceRepoDirQuota() {
        return spaceRepoDirQuota;
    }

    public void setSpaceRepoDirQuota(long spaceRepoDirQuota) {
        this.spaceRepoDirQuota = spaceRepoDirQuota;
        LOG.info("Quota set to " + spaceRepoDirQuota + " bytes");
    }

    /**
     * Check if the repository has grown too large. If it has, prune by removing artifacts that can be removed (see
     * artifact retention policies), starting oldest first.
     */
    public void prune() throws IOException {
        boolean done = false;
        while (!done) {
            spaceMaxAvailableInRepoDir = repoDir.getTotalSpace();
            spaceAvailableInRepoDir = repoDir.getUsableSpace();
            long currentUsedRepoSpace = 0;
            for (Artifacts.Artifact artifact : index.values()) {
                currentUsedRepoSpace += artifact.getInstalledSize();
            }

            float freeSpacePercent = 100.0f * spaceAvailableInRepoDir / spaceMaxAvailableInRepoDir;
            LOG.debug(String.format("Available free space as percentage of total (repo dir filesystem): %f %% %n",
                    freeSpacePercent));
            if (currentUsedRepoSpace > spaceRepoDirQuota || freeSpacePercent < PERCENT_SPACE_THRESHOLD) {
                LOG.warn("Pruning must remove some artifacts because either the quota has been exceeded, or there is not enough available space in the repository.");
                LOG.warn(String.format("(currentUsedRepoSpace=%d) > (spaceRepoDirQuota=%d) = %b %n", currentUsedRepoSpace,
                        spaceRepoDirQuota, currentUsedRepoSpace > spaceRepoDirQuota));
                LOG.warn(String.format("(freeSpacePercent=%f) < (PERCENT_SPACE_THRESHOLD=%f) = %b %n",
                        freeSpacePercent, PERCENT_SPACE_THRESHOLD, freeSpacePercent < PERCENT_SPACE_THRESHOLD));
                boolean removed = removeOldestArtifact();
                if (!removed) {
                    LOG.error("Could not remove any artifact, despite exceed quota. Aborting..");
                    done = true;
                }
            } else {
                done = true;
            }
        }
    }

    /**
     * Remove the oldest artifact, returns true when an artifact was removed.
     *
     * @return
     * @throws IOException
     */
    private boolean removeOldestArtifact() throws IOException {

        Artifacts.Artifact[] sortedArtifacts = new Artifacts.Artifact[index.size()];
        sortedArtifacts = index.values().toArray(sortedArtifacts);
        Arrays.sort(sortedArtifacts, SORT_BY_INSTALLED_DATE);
        for (Artifacts.Artifact artifact : sortedArtifacts) {
            switch (artifact.getRetention()) {
                case REMOVE_OLDEST:
                    remove(artifact);
                    return true;
                default:
                    // keep other artifacts.
            }
        }
        return false;
    }

    /**
     * The repository directory quota. The repository will try not to use more storage than indicated in this
     * quota, even when the filesystem that contains the repository directory has more available space.
     */
    private long spaceRepoDirQuota;

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

        if (pluginScript != null && !new File(pluginScript).exists()) {
            throw new IOException("Install script not found: " + pluginScript);
        }
        // get attributes before anything else:
        if (hasUndefinedAttributes(avp)) {
            avp = getAttributeValues(null, artifactId, version, avp, pluginScript, pluginId);
        }
        Artifacts.Artifact artifact = find(pluginId, artifactId, version, avp);
        if (artifact != null && artifact.getState() == Artifacts.InstallationState.INSTALLING) {

            LOG.info("Found an artifact that failed to finished installing (was state=INSTALLING).. This likely results from the repo being killed/(or crash) during an installation. Removing the artifact to start installation over. ");

            remove(artifact);
            save();
            // reload the plugin info from disk:
            load();
            artifact = find(pluginId, artifactId, version, avp);


        }

        if (artifact != null && artifact.getState() == Artifacts.InstallationState.INSTALLED) {
            LOG.info(String.format("Artifact %s was found and was installed.", toText(artifact)));
            // even when already installed, scan for possible env script
            registerPossibleEnvironmentCollection(artifact);
            return;
        }
        if (artifact != null) {
            LOG.warn(String.format("Found artifact in state %s, removing and starting over.. ", artifact.getState()));
            remove(artifact);
            artifact = null;
        }
        if (artifact == null) {

            LOG.info(String.format("Artifact %s was not found, proceeding to install..", toText(pluginId, artifactId, version, avp)));

            // create the new artifact, register in the index:
            Artifacts.Artifact.Builder artifactBuilder = Artifacts.Artifact.newBuilder();
            artifactBuilder.setId(artifactId);
            artifactBuilder.setPluginId(pluginId);
            artifactBuilder.setState(Artifacts.InstallationState.INSTALLING);
            artifactBuilder.setInstallationTime(new Date().getTime());
            final String installScriptDir = FilenameUtils.concat(FilenameUtils.concat(pluginId, artifactId), version);
            artifactBuilder.setRelativePath(appendKeyValuePairs(installScriptDir, avp));
            artifactBuilder.setVersion(version);

            for (AttributeValuePair valuePair : avp) {
                final Artifacts.AttributeValuePair.Builder avpBuilder = Artifacts.AttributeValuePair.newBuilder().setName(valuePair.name);
                if (valuePair.value != null) {
                    avpBuilder.setValue(valuePair.value);
                }
                artifactBuilder.addAttributes(avpBuilder.build());
            }
            Artifacts.Host.Builder hostBuilder = Artifacts.Host.newBuilder();

            hostBuilder.setHostName(InetAddress.getLocalHost().getHostName());
            hostBuilder.setOsArchitecture(System.getProperty("os.arch"));
            hostBuilder.setOsName(System.getProperty("os.name"));
            hostBuilder.setOsVersion(System.getProperty("os.version"));
            artifactBuilder.setInstallationHost(hostBuilder);
            artifactBuilder.setRetention(Artifacts.RetentionPolicy.REMOVE_OLDEST);
            artifact = artifactBuilder.build();
            index.put(makeKey(artifact), artifact);

            save();
            try {

                runInstallScript(pluginId, artifactId, pluginScript, version, avp);
                updateInstalledSize(artifact);

                updateInstallScriptLocation(artifact, pluginScript);
                artifact = changeState(artifact, Artifacts.InstallationState.INSTALLED);
                updateExportStatements(artifact, avp, currentBashExports);
                registerPossibleEnvironmentCollection(artifact);
            } catch (InterruptedException e) {
                changeState(artifact, Artifacts.InstallationState.FAILED);
            } catch (RuntimeException e) {
                changeState(artifact, Artifacts.InstallationState.FAILED);
            } catch (Exception e) {
                changeState(artifact, Artifacts.InstallationState.FAILED);
            } catch (Error e) {
                changeState(artifact, Artifacts.InstallationState.FAILED);
            }
            LOG.debug("Exiting ArtifactRepo.install");
            save();
        }
    }

    /**
     * List of script paths for environment collection scripts. This collection holds local absolute paths.
     * We use a list because order of the scripts is important, but we discard repeated scripts, keeping only
     * the first registration.
     */
    private ObjectArrayList<String> environmentCollectionScripts = new ObjectArrayList<String>();


    protected void registerPossibleEnvironmentCollection(Artifacts.Artifact artifact) {
        if (artifact.getPluginId().startsWith(BuildArtifactRequest.ARTIFACTS_ENVIRONMENT_COLLECTION_SCRIPT)) {
            String cachedInstallationScript = getCachedInstallationScript(artifact.getPluginId());
            LOG.info(String.format("Registering environment script %s",cachedInstallationScript));
            if (!environmentCollectionScripts.contains(cachedInstallationScript)) {
                environmentCollectionScripts.add(cachedInstallationScript);
            }
        }
    }

    /**
     * Unregister all environment collection scripts registered so far.
     */
    public void unregisterAllEnvironmentCollectionScripts() {
        environmentCollectionScripts.clear();
    }

    private boolean updateInstallScriptLocation(Artifacts.Artifact artifact, String pluginScript) throws IOException {

        if (pluginScript == null || artifact.hasInstallScriptRelativePath()) {
            //success
            return true;
        }
        artifact = index.get(makeKey(artifact));
        Artifacts.Artifact.Builder artifactBuilder = artifact.toBuilder();

        String pluginId = artifact.getPluginId();
        String artifactId = artifact.getId();
        String version = artifact.getVersion();
        final String installScriptDir = FilenameUtils.concat(pluginId, version);
        boolean failed = false;
        final File installScriptFinalLocation = new File(FilenameUtils.concat(installScriptDir, "install.sh"));
        final File installInRepoAbsolute = getCachedScriptLocation(installScriptFinalLocation);

        try {

            installInRepoAbsolute.getParentFile().mkdirs();
            if (pluginScript.equals(installInRepoAbsolute.getAbsolutePath())) {
                // script already cached
                return true;
            }

            FileUtils.copyFile(new File(pluginScript), installInRepoAbsolute);
            LOG.info("LOCAL_COPY: Copied install script to " + installInRepoAbsolute);
            artifact = artifactBuilder.setInstallScriptRelativePath(installScriptFinalLocation.getPath()).build();
            index.put(makeKey(artifact), artifact);
            pluginIdToInstallScriptPath.put(artifact.getPluginId(),
                    absolutePathInRepo("scripts", artifactBuilder.getInstallScriptRelativePath()));

            save();
        } catch (IOException e) {
            LOG.error("LOCAL_COPY: Failed to cache install script " + installInRepoAbsolute, e);

            throw e;
        }
        return !failed;

    }

    private File getCachedScriptLocation(String installScriptFinalLocation) {
        return getCachedScriptLocation(new File(installScriptFinalLocation));
    }

    private File getCachedScriptLocation(File installScriptFinalLocation) {
        return new File(FilenameUtils.concat(FilenameUtils.concat(repoDir.getAbsolutePath(),
                "scripts"),
                installScriptFinalLocation.getPath()));
    }

    protected String toText(Artifacts.Artifact artifact) {
        if (artifact != null) {
            return toText(artifact.getPluginId(), artifact.getId(), artifact.getVersion(),
                    convert(artifact.getAttributesList()));
        }
        {
            return "null";
        }
    }

    protected String toText(String pluginId, String artifactId, String version, AttributeValuePair[] avp) {
        return String.format("%s:%s:%s(%s)", pluginId, artifactId, version, ObjectArrayList.wrap(avp).toString());
    }


    private void updateExportStatements(Artifacts.Artifact artifact, AttributeValuePair[] avp,
                                        MutableString destination) throws IOException {
        LOG.debug("printBashExports");


        //repo.convert(request.getAttributesList()
        if (artifact != null && artifact.getState() == Artifacts.InstallationState.INSTALLED) {
            List<Artifacts.AttributeValuePair> list = artifact.getAttributesList();
            boolean attributesInRepoMatchEnvironment = true;
            if (!list.isEmpty()) {
                // we must verify that the artifact attributes recorded in the repo match those
                // needed in the specific runtime environment we are in.
                AttributeValuePair[] avpEnvironment = convert(artifact.getAttributesList());
                AttributeValuePair[] avpRepo = convert(artifact.getAttributesList());
                for (AttributeValuePair attributeValuePair : avpEnvironment) {
                    // we null the value to collect it from the script/environment:
                    attributeValuePair.value = null;
                }
                Properties properties = readAttributeValues(artifact, avpEnvironment);
                if (properties == null) {
                    // we could not obtain avpEnvironment
                    return;
                }
                attributesInRepoMatchEnvironment = Arrays.equals(avpEnvironment, avpRepo);
            }

            if (attributesInRepoMatchEnvironment) {

                // only write exports when the attribute values obtained from the runtime env match those in the repo:
                final AttributeValuePair[] avpPluginInRepo = convert(artifact.getAttributesList());
                final String exportLine1 = String.format("export RESOURCES_ARTIFACTS_%s_%s%s=%s%n", artifact.getPluginId(),
                        artifact.getId(), listAttributeValues(artifact.getAttributesList()),
                        getInstalledPath(artifact.getPluginId(), artifact.getId(), artifact.getVersion(),
                                avpPluginInRepo));
                destination.append(exportLine1);
                LOG.debug(exportLine1);
                // also write each attribute value:
                for (Artifacts.AttributeValuePair attribute : artifact.getAttributesList()) {
                    if (attribute.getValue() != null) {
                        final String exportLine2 = String.format("export RESOURCES_ARTIFACTS_%s_%s_%s=%s%n",
                                artifact.getPluginId(),
                                artifact.getId(),
                                normalize(attribute.getName()),
                                attribute.getValue());
                        destination.append(exportLine2);
                        LOG.debug(exportLine2);
                    }
                }
            }
        }


    }

    protected Properties readAttributeValues(Artifacts.Artifact artifact, AttributeValuePair[] avpEnvironment) throws IOException {
        LOG.debug("readAttributeValues(artifact, avpEnvironment)");

        assert artifact.getState() == Artifacts.InstallationState.INSTALLED : "Artifact must be installed to call readAttributeValues(artifact). ";
        if (!hasCachedInstallationScript(artifact.getPluginId())) {
            LOG.error("Cached install script must be found for plugin " + toText(artifact));
            return null;
        }
        String installScript = getCachedInstallationScript(artifact.getPluginId());
        return readAttributeValues(artifact.getPluginId(), artifact.getId(), artifact.getVersion(),
                avpEnvironment, installScript);

    }

    private Properties readAttributeValues(String pluginId, String artifactId, String version, AttributeValuePair[] avp, String pluginInstallScript) {
        File attributeFile = null;
        try {
            attributeFile = runAttributeValuesFunction(pluginId, artifactId, version, avp, pluginInstallScript);
            if (attributeFile != null) {
                // parse properties and value attributes that were not defined:
                Properties p = new Properties();
                p.load(new FileReader(attributeFile));
                for (AttributeValuePair attributeValuePair : avp) {
                    if (attributeValuePair.value == null) {
                        final String scriptValue = p.getProperty(attributeValuePair.name);
                        if (scriptValue == null) {
                            LOG.error("Could not obtain attribute value from install script for attribute=" + attributeValuePair.name);
                            return null;
                        }
                        attributeValuePair.value = normalize(scriptValue);
                    }
                }
                LOG.debug("readAttributeValues() returned: " + ObjectArrayList.wrap(avp).toString());

                return p;
            }
        } catch (InterruptedException e) {
            return null;
        } catch (RuntimeException e) {
            return null;
        } catch (Exception e) {
            return null;
        } catch (Error e) {
            return null;
        } finally {
            if (attributeFile != null) {
                attributeFile.delete();
            }
        }
        return null;
    }

    protected AttributeValuePair[] getAttributeValues(Artifacts.Artifact artifact,
                                                      String artifactId, String version,
                                                      AttributeValuePair[] avp, String pluginScript,
                                                      String pluginId) throws IOException {
        LOG.debug("getAttributeValues(artifact, artifactId,version, avp, pluginScript, pluginId)");
        boolean failed = false;
        if (pluginScript == null) {
            return new AttributeValuePair[0];
        }
        Properties p = readAttributeValues(pluginId, artifactId, version, avp, pluginScript);
        if (p == null) {
            failed = true;
        }
        if (failed) {
            LOG.error("Unable to retrieve attributes for plugin " + toText(artifact));
            if (artifact != null) {
                changeState(artifact, Artifacts.InstallationState.FAILED);
            }
        }
        save();
        return avp;
    }

    private File runAttributeValuesFunction(String pluginId, String artifactId, String version, AttributeValuePair[] avp, String pluginScript) throws IOException, InterruptedException {
        pluginScript = new File(pluginScript).getAbsolutePath();
        String tmpDir = System.getProperty("java.io.tmpdir");
        final long time = new Date().getTime();
        LOG.debug("Attempting to execute runAttributeValuesFunction for script= " + pluginScript);

        MutableString sourceEnvCollectionScripts = getEnvCollectionSourceStatements();
        File result = new File(String.format("%s/%s-%s-%d/artifact.properties", tmpDir, pluginId, artifactId, time));
        String wrapperTemplate = "( set -e ; set +xv ; DIR=%s/%s-%s-%d ; script=%s; echo $DIR; mkdir -p ${DIR}; %s  " +
                " chmod +x $script ;  . $script ; get_attribute_values %s $DIR/artifact.properties ; cat $DIR/artifact.properties; set -xv )%n";

        String cmds[] = {"/bin/bash", "-c", String.format(wrapperTemplate, tmpDir,
                pluginId, artifactId,
                time,
                pluginScript,
                sourceEnvCollectionScripts, artifactId
        )};

        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(cmds);
        new Thread(new SyncPipe(pr.getErrorStream(), System.err, LOG)).start();
        new Thread(new SyncPipe(quiet, pr.getInputStream(), System.out, LOG)).start();

        int exitVal = pr.waitFor();
        LOG.debug("Install script get_attribute_values() exited with error code " + exitVal);
        if (exitVal != 0) {
            throw new IllegalStateException();
        } else {

            return result;
        }
    }

    private MutableString getEnvCollectionSourceStatements() {
        String jobDir = System.getenv("JOB_DIR");
        MutableString sourceEnvCollectionScripts = new MutableString();

        for (String envScript : environmentCollectionScripts) {
            if (jobDir != null) {
                sourceEnvCollectionScripts.append(String.format(" export JOB_DIR=%s; chmod +x %s; source %s; ", jobDir, envScript, envScript));
            } else {
                sourceEnvCollectionScripts.append(String.format(" chmod +x %s; source %s; ", envScript, envScript));

            }
        }
        LOG.info("Returning EnvCollectionSourceStatements: "+sourceEnvCollectionScripts);
        return sourceEnvCollectionScripts;
    }

    /**
     * Returns true if any of the attributes have underfined value.
     *
     * @param avp
     * @return
     */
    private boolean hasUndefinedAttributes(AttributeValuePair[] avp) {
        for (AttributeValuePair a : avp) {
            if (a.value == null) return true;
        }
        return false;
    }


    private Artifacts.Artifact changeState(Artifacts.Artifact artifact, Artifacts.InstallationState newState) throws IOException {
        artifact = index.get(makeKey(artifact));
        Artifacts.Artifact.Builder artifactBuilder = artifact.toBuilder().setState(newState);
        artifact = artifactBuilder.build();
        index.put(makeKey(artifact), artifact);
        save();
        return artifact;
    }

    private void updateInstalledSize(Artifacts.Artifact artifact) throws IOException {
        artifact = index.get(makeKey(artifact));
        Artifacts.Artifact.Builder artifactBuilder = artifact.toBuilder();
        final long artifactInstalledSize = FileUtils.sizeOfDirectory(new File(getPluginInstallDir(artifact)));
        artifactBuilder.setInstalledSize(artifactInstalledSize);
        artifact = artifactBuilder.build();
        index.put(makeKey(artifact), artifact);
        save();
    }

    private String getPluginInstallDir(Artifacts.Artifact artifact) {
        return FilenameUtils.concat(FilenameUtils.concat(repoDir.getAbsolutePath(), "artifacts"),
                artifact.getRelativePath());
    }

    /**
     * Remove an artifact from the repository.
     *
     * @param pluginId
     * @param artifactId
     */
    public void remove(String pluginId, String artifactId, String version, AttributeValuePair... avp) throws IOException {

        Artifacts.Artifact artifact = find(pluginId, artifactId, version, avp);
        if (artifact == null) {
            LOG.warn(String.format("Could not find artifact %s:%s with attributes, removing while ignoring attributes.",
                    pluginId, artifactId));
            List<Artifacts.Artifact> list = findIgnoringAttributes(pluginId, artifactId, version);
            for (Artifacts.Artifact a : list) {
                removeArtifactInternal(a.getPluginId(), a.getId(), a.getVersion(), a, convert(a.getAttributesList()));
            }
            if (list.isEmpty()) {
                LOG.warn(String.format("Could not find any artifact matching %s:%s. Ignoring remove request.",
                        pluginId, artifactId));
            }

            return;
        } else {
            removeArtifactInternal(pluginId, artifactId, version, artifact, avp);
        }
        save();
    }

    private void removeArtifactInternal(String pluginId, String artifactId, String version, Artifacts.Artifact artifact, AttributeValuePair[] avp) throws IOException {
        FileUtils.deleteDirectory(getArtifactDir(pluginId, artifactId, version, avp));
        LOG.info(String.format("Removing artifact %s:%s.",
                pluginId, artifactId));
        index.remove(makeKey(artifact));
    }

    private String appendKeyValuePairs(String artifactInstallDir, AttributeValuePair... avp) {
        if (avp == null) return artifactInstallDir;
        String result = artifactInstallDir;
        for (AttributeValuePair valuePair : avp) {
            if (valuePair.value != null) {
                result = FilenameUtils.concat(result, normalize(valuePair.value));
            }
        }
        return result;
    }

    private File getArtifactDir(String pluginId, String artifactId, String version, AttributeValuePair... avp) {

        return new File(appendKeyValuePairs(FilenameUtils.concat(
                FilenameUtils.concat(
                        FilenameUtils.concat(FilenameUtils.concat(
                                repoDir.getAbsolutePath(), "artifacts"),
                                pluginId), artifactId), version),
                avp));
    }

    private void runInstallScript(String pluginId, String artifactId, String pluginScript, String version, AttributeValuePair[] avp)
            throws IOException, InterruptedException {

        String installationPath = mkDirs(repoDir, pluginId, artifactId, version, avp);
        if (pluginScript == null) {
            return;
        }
        pluginScript = new File(pluginScript).getAbsolutePath();
        File tmpExports = File.createTempFile("exports", ".sh");
        MutableString exportString = add(preInstalledPluginExports, currentBashExports);
        FileUtils.write(tmpExports, exportString != null ? exportString.toString() : "", true);
        String tmpDir = System.getProperty("java.io.tmpdir");

        final long time = new Date().getTime();
        try {
            String wrapperTemplate =
                    " dieIfError() {\n" +
                            " S=$?; \n" +
                            " if [ ! \"$S\" = \"0\" ]; then \n" +
                            "    exit $S; \n" +
                            " fi \n" +
                            "} \n" +
                            "( set -e ; set -x ; exports=%s ; cat $exports ; DIR=%s/%d ; script=%s; echo $DIR; mkdir -p ${DIR}; cd ${DIR}; ls -l ; " +
                            " chmod +x $script ; %s . $exports; . $script ; dieIfError; plugin_install_artifact %s %s %s; dieIfError; ls -l ; rm -fr ${DIR}); %n";

            MutableString sourceEnvCollectionScripts = getEnvCollectionSourceStatements();
            String cmds[] = {"/bin/bash", "-c", String.format(wrapperTemplate, tmpExports.getCanonicalPath(),
                    tmpDir, time,
                    pluginScript,
                    sourceEnvCollectionScripts,
                    artifactId,
                    installationPath, formatForCommandLine(avp))};

            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(cmds);

            new Thread(new SyncPipe(pr.getErrorStream(), System.err, LOG)).start();
            new Thread(new SyncPipe(pr.getInputStream(), System.out, LOG)).start();

            int exitVal = pr.waitFor();

            // System.out.println("Install script exited with error code " + exitVal);
            tmpExports.delete();
            if (exitVal != 0) {
                LOG.info("Install script exited with error code " + exitVal);
                throw new IllegalStateException();
            }
        } finally {
            // delete the install directory in case it was left behind:

            new File(String.format("%s/%d", tmpDir, time)).delete();
            tmpExports.delete();
        }

    }

    private MutableString add(MutableString preInstalledPluginExports, MutableString currentBashExports) {
        MutableString result = new MutableString();
        result.append(preInstalledPluginExports);
        result.append(currentBashExports);
        return result;
    }

    private MutableString formatForCommandLine(AttributeValuePair[] avp) {
        MutableString buffer = new MutableString();
        for (AttributeValuePair valuePair : avp) {
            buffer.append("\"");
            buffer.append(normalize(valuePair.value));
            buffer.append("\" ");
        }
        return buffer;
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

    public Artifacts.Artifact find(String pluginId, String artifactId, String version, AttributeValuePair... avp) {

        return index.get(makeKey(pluginId, artifactId, version, avp));
    }

    /**
     * Find artifacts, ignoring any possible attributes.
     *
     * @param pluginId
     * @param artifactId
     * @param version
     * @return list of attributes with suitable pluginId, artifactIds and version.
     */
    public List<Artifacts.Artifact> findIgnoringAttributes(String pluginId, String artifactId, String version) {
        List<Artifacts.Artifact> result = new ObjectArrayList<Artifacts.Artifact>();
        for (MutableString key : index.keySet()) {
            if (key.startsWith(makeKey(pluginId, artifactId, version).toString())) {
                result.add(index.get(key));
            }
        }
        return result;
    }

    public synchronized void load(File repoDir) throws IOException {
        load(repoDir, true);
    }

    public synchronized void load(boolean updateExportStatements) throws IOException {
        load(this.repoDir, updateExportStatements);
    }

    public synchronized void load(File repoDir, boolean updateExportStatements) throws IOException {
        FileInputStream input = null;
        try {
            acquireExclusiveLock();

            File repoFile = new File(getMetaDataFilename());
            Artifacts.Repository repo;
            if (repoFile.exists() && FileUtils.sizeOf(repoFile) != 0) {
                LOG.trace(String.format("Loading from %s%n", repoDir.getAbsolutePath()));
                input = new FileInputStream(getMetaDataFilename());
                repo = Artifacts.Repository.parseDelimitedFrom(input);
                LOG.trace(String.format("Loaded repo with %d artifacts. %n", repo.getArtifactsCount()));
            } else {
                // creating new repo:
                repo = Artifacts.Repository.getDefaultInstance();

            }
            scan(repo);
            preInstalledPluginExports.setLength(0);
            if (updateExportStatements) {
                // pre-set export statements with exports for all pre-installed tools:
                for (Artifacts.Artifact installedArtifact : this.index.values()) {
                    if (installedArtifact.getState() == Artifacts.InstallationState.INSTALLED)

                        updateExportStatements(installedArtifact, convert(installedArtifact.getAttributesList()),
                                preInstalledPluginExports);
                }
            }
        } finally {
            releaseLock();
            if (input != null) {
                input.close();
            }
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
            if (artifact.hasInstallScriptRelativePath()) {
                String cachedPath = absolutePathInRepo("scripts", artifact.getInstallScriptRelativePath());
                pluginIdToInstallScriptPath.put(artifact.getPluginId(), cachedPath);
            }
        }
    }


    /**
     * Calculate an absolute path for a relative path inside the current repo.
     *
     * @param relativePath
     * @param kind         type of path (i.e. scripts or artifacts)
     * @return An absolute path corresponding to the relative path.
     */
    protected String absolutePathInRepo(String kind, String relativePath) {
        return FilenameUtils.concat(repoDir.getAbsolutePath(), FilenameUtils.concat(kind, relativePath));
    }

    /**
     * Return the location of the cached installation script. Please note that the file at that location may not exist.
     *
     * @param pluginId Id of the plugin for which the installation script is sought.
     * @return Absolute path of the cached installation script.
     */
    public String getCachedInstallationScript(String pluginId) {
        return pluginIdToInstallScriptPath.get(pluginId);
    }

    /**
     * Determine whether a plugin has a valid cached installation script.
     *
     * @param pluginId Id of the plugin for which the installation script is sought.
     * @return True or False.
     */
    public boolean hasCachedInstallationScript(String pluginId) {
        LOG.debug("hasCachedInstallationScript " + pluginId);
        final String cachedInstallationScript = getCachedInstallationScript(pluginId);
        if (cachedInstallationScript != null && !new File(cachedInstallationScript).exists()) {
            // the cache was removed to trigger reinstallation. Do it here for all the artifacts of this plugin:
            for (Artifacts.Artifact artifact : findArtifacts(pluginId)) {
                ArtifactRequestHelper.fetchInstallScript(artifact, artifact.getInstallationRequest(), this);
            }
        }
        return cachedInstallationScript != null && new File(cachedInstallationScript).exists();
    }

    private ObjectArrayList<Artifacts.Artifact> findArtifacts(String pluginId) {
        ObjectArrayList<Artifacts.Artifact> result = new ObjectArrayList<Artifacts.Artifact>();
        for (Artifacts.Artifact artifact : index.values()) {
            if (artifact.getPluginId().equals(pluginId)) {
                result.add(artifact);
            }
        }
        return result;
    }

    private MutableString makeKey(Artifacts.Artifact artifact) {

        return makeKey(artifact.getPluginId(), artifact.getId(), artifact.getVersion(),
                convert(artifact.getAttributesList()));

    }

    static public AttributeValuePair[] convert(List<Artifacts.AttributeValuePair> attributesList) {
        if (attributesList == null) return new AttributeValuePair[0];
        AttributeValuePair[] avp = new AttributeValuePair[attributesList.size()];
        int index = 0;
        for (Artifacts.AttributeValuePair a : attributesList) {
            avp[index++] = new AttributeValuePair(a.getName(), a.hasValue() ? a.getValue() : null);
        }
        return avp;
    }

    private MutableString makeKey(String pluginId, String artifactId, String version,
                                  AttributeValuePair... avp) {
        MutableString key = new MutableString(pluginId);
        key.append('$');
        key.append(artifactId);
        key.append('$');
        key.append(version);
        if (avp != null) {
            for (AttributeValuePair valuePair : avp) {
                key.append('$');
                key.append(normalize(valuePair.name));
                key.append('=');
                key.append(normalize(valuePair.value));
            }
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
    String normalize(String attribute) {
        if (attribute == null) {
            return null;
        }
        String replacements[] = {"!", "\\$", " ", "-"};
        for (String rep : replacements) {
            attribute = attribute.replaceAll(rep, "_");

        }
        return attribute.toUpperCase();
    }

    private Object2ObjectOpenHashMap<MutableString, Artifacts.Artifact> index = new Object2ObjectOpenHashMap<MutableString, Artifacts.Artifact>();

    public void save() throws IOException {
        save(repoDir);
    }

    public synchronized void save(File repoDir) throws IOException {

        FileOutputStream output = null;
        try {
            acquireExclusiveLock();

            LOG.debug(String.format("Saving to %s %n", repoDir.getAbsolutePath()));
            output = new FileOutputStream(getMetaDataFilename());
            // recreate the ProtoBuf repo from the index:
            Artifacts.Repository.Builder repoBuilder = Artifacts.Repository.newBuilder();
            repoBuilder.addAllArtifacts(index.values());
            Artifacts.Repository repo = repoBuilder.build();

            repo.writeDelimitedTo(output);
            LOG.debug(String.format("Wrote repo with %d artifacts.%n", repo.getArtifactsCount()));

        } finally {
            if (output != null) {
                output.close();
            }
            releaseLock();

        }
    }

    private ExclusiveLockRequest request;

    public synchronized void acquireExclusiveLock() throws IOException {
        if (lockCount > 0) {
            // already locked, simply increment lock count and return:
            lockCount++;
            return;
        }
        LOG.debug("acquireExclusiveLock()");
        request = new ExclusiveLockRequestWithFile(metaDataFilename + ".lock", repoDir);
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
        lockCount++;
    }

    public RandomAccessFile getLockedRepoFile() {
        return request.getLockedFile();
    }

    public synchronized void releaseLock() throws IOException {
        if ((lockCount - 1) > 0) {
            // still locked after releasing this lock:  simply decrement lock count and return:
            lockCount--;
            return;
        }
        LOG.debug("releaseLock()");
        request.release();
        request = null;
        lockCount--;
    }

    public void load() throws IOException {
        load(repoDir);
    }

    public void remove(String plugin, String artifact, AttributeValuePair... avp) throws IOException {
        remove(plugin, artifact, "VERSION", avp);
    }

    public void remove(Artifacts.Artifact artifact) throws IOException {
        remove(artifact.getPluginId(), artifact.getId(), artifact.getVersion(),
                convert(artifact.getAttributesList()));
    }

    public String getInstalledPath(String pluginId, String artifactId) {
        return getInstalledPath(pluginId, artifactId, "VERSION");
    }

    public String getInstalledPath(String pluginId, String artifactId, String version, AttributeValuePair... avp) {
        Artifacts.Artifact artifact = find(pluginId, artifactId, version, avp);
        if (artifact == null) {
            System.err.printf("Artifact %s:%s:%s could not be found. %n ", pluginId, artifactId, version, avp);
            return null;
        } else {
            return getPluginInstallDir(artifact);
        }
    }

    public void show() throws IOException {
        load();
        Artifacts.Repository.Builder repoBuilder = Artifacts.Repository.newBuilder();
        repoBuilder.addAllArtifacts(index.values());
        Artifacts.Repository repo = repoBuilder.build();
        TextFormat.print(repo, System.out);
    }

    public void setRetention(String pluginId, String artifactId, String version, AttributeValuePair[] avp, Artifacts.RetentionPolicy retention) {
        Artifacts.Artifact artifact = find(pluginId, artifactId, version, avp);
        if (artifact != null) {
            // update retention and store back:
            index.put(makeKey(artifact), artifact.toBuilder().setRetention(retention).build());
        }
    }

    /**
     * Update values of an artifact in the repository.
     *
     * @param revisedArtifact
     */
    public void updateArtifact(Artifacts.Artifact revisedArtifact) throws IOException {

        // update retention and store back:
        index.put(makeKey(revisedArtifact), revisedArtifact);
        save();
        load();
    }

    /**
     * Set export variable BASH statements as a string.
     *
     * @param currentBashExports
     */
    public void setCurrentBashExports(String currentBashExports) {

        this.currentBashExports.setLength(0);
        this.currentBashExports.append(preInstalledPluginExports);
        this.currentBashExports.append(currentBashExports);

    }


    static protected String listAttributeValues(List<Artifacts.AttributeValuePair> attributesList) {
        StringBuffer sb = new StringBuffer();

        for (Artifacts.AttributeValuePair valuePairs : attributesList) {
            if (valuePairs.getValue().length() > 0) {

                sb.append("_");
                sb.append(valuePairs.getValue());
            }
        }
        return sb.toString();
    }

    /**
     * Determine if a plugin was installed.
     *
     * @param pluginId   PluginId
     * @param artifactId artifactId
     * @param version    version
     * @param avp        Attribute value pairs.
     * @return
     */
    public boolean isInstalled(String pluginId, String artifactId, String version, AttributeValuePair... avp) {
        Artifacts.Artifact found = find(pluginId, artifactId, version, avp);
        if (found == null) {
            return false;
        }
        return found.getState() == Artifacts.InstallationState.INSTALLED;
    }

    public void printBashExports(PrintWriter printWriter) {
        printWriter.print(preInstalledPluginExports);
        printWriter.print(currentBashExports);
        printWriter.flush();
    }


    public ObjectCollection<Artifacts.Artifact> getArtifacts() {
        return index.values();
    }

    public String toTextShort(Artifacts.Artifact artifact) {
        return String.format("%s:%s:%s", artifact.getId(), artifact.getId(), artifact.getVersion());
    }

    public String getMetaDataFilename() {
        return FilenameUtils.concat(repoDir.getAbsolutePath(), "metadata.pb");
    }
}
