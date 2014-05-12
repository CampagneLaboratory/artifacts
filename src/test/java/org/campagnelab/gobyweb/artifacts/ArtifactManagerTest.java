package org.campagnelab.gobyweb.artifacts;


import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.campagnelab.gobyweb.artifacts.scope.ExplicitInstallationScope;
import org.campagnelab.gobyweb.artifacts.scope.InstallationScope;
import org.campagnelab.stepslogger.StepsReportBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.*;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for simple App.
 */
public class ArtifactManagerTest {


    private File repoDir = new File("REPO");
    private File stepsLogDir = new File("test-results/stepslogs");

    @Test
    public void testInstall() throws IOException {
        ArtifactManager manager = new ArtifactManager("REPO");
        final ArtifactRepo repo = manager.getRepo();
        repo.load();
        assertNull(repo.find("PLUGIN", "ARTIFACT"));
        repo.install("PLUGIN", "ARTIFACT");
        assertNotNull(repo.find("PLUGIN", "ARTIFACT"));
        repo.save();
        repo.show();
    }

    @Test
    public void testRemove() throws IOException {
        ArtifactManager manager = new ArtifactManager("REPO");
        final ArtifactRepo repo = manager.getRepo();
        repo.load();
        repo.install("PLUGIN", "ARTIFACT");
        assertNotNull(repo.find("PLUGIN", "ARTIFACT"));
        repo.remove("PLUGIN", "ARTIFACT");
        assertNull(repo.find("PLUGIN", "ARTIFACT"));
        repo.save();
    }

    @Test
    public void testInstallTwice() throws IOException {
        ArtifactManager manager = new ArtifactManager("REPO");
        final ArtifactRepo repo = manager.getRepo();
        repo.load();
        assertNull(repo.find("PLUGIN", "ARTIFACT"));
        repo.install("PLUGIN", "ARTIFACT");
        repo.install("PLUGIN", "ARTIFACT");
        assertNotNull(repo.find("PLUGIN", "ARTIFACT"));
        assertEquals(Artifacts.InstallationState.INSTALLED, repo.find("PLUGIN", "ARTIFACT").getState());
        repo.save();
    }


    @Test
    // test that artifacts install scripts have access to install path export statements of artifacts installed before
    // them:
    public void testInstallChained() throws IOException {
        ArtifactManager manager = new ArtifactManager("REPO");
        final ArtifactRepo repo = manager.getRepo();
        repo.load();
        assertNull(repo.find("PLUGIN", "ARTIFACT"));
        repo.install("PLUGIN1", "A", "test-data/install-scripts/install-script5.sh");
        repo.install("PLUGIN2", "B", "test-data/install-scripts/install-script6.sh");
        assertNotNull(repo.find("PLUGIN1", "A"));
        assertNotNull(repo.find("PLUGIN2", "B"));
        assertEquals(Artifacts.InstallationState.INSTALLED, repo.find("PLUGIN1", "A").getState());
        assertEquals(Artifacts.InstallationState.INSTALLED, repo.find("PLUGIN2", "B").getState());
        repo.save();
    }

    @Test
    public void testInstallScript1() throws IOException {
        ArtifactManager manager = new ArtifactManager("REPO");
        final ArtifactRepo repo = manager.getRepo();
        repo.load();
        assertNull(repo.find("PLUGIN", "FILE1"));
        assertFalse(new File("REPO/artifacts/PLUGIN/FILE1/installed-file-1").exists());
        assertFalse(new File("REPO/artifacts/PLUGIN/FILE1/installed-file-2").exists());

        repo.install("PLUGIN", "FILE1", "test-data/install-scripts/install-script1.sh");
        assertNotNull(repo.find("PLUGIN", "FILE1"));
        assertEquals(Artifacts.InstallationState.INSTALLED, repo.find("PLUGIN", "FILE1").getState());
        repo.save();
        assertTrue(new File("REPO/artifacts/PLUGIN/FILE1/VERSION/installed-file-1").exists());
        assertFalse(new File("REPO/artifacts/PLUGIN/FILE1/VERSION/installed-file-2").exists());

        repo.install("PLUGIN", "FILE2", "test-data/install-scripts/install-script1.sh");
        assertTrue(new File("REPO/artifacts/PLUGIN/FILE2/VERSION/installed-file-2").exists());
        repo.remove("PLUGIN", "FILE1");
        assertFalse(new File("REPO/artifacts/PLUGIN/FILE1/VERSION/installed-file-1").exists());
        repo.remove("PLUGIN", "FILE2");
        assertFalse(new File("REPO/artifacts/PLUGIN/FILE2/VERSION/installed-file-2").exists());

    }


    @Test
    public void testGetPath() throws IOException {

        ArtifactManager manager = new ArtifactManager("REPO");
        final ArtifactRepo repo = manager.getRepo();
        repo.load();
        assertNull(repo.find("PLUGIN", "ARTIFACT"));
        repo.install("PLUGIN", "ARTIFACT");
        assertNotNull(repo.find("PLUGIN", "ARTIFACT"));
        Assert.assertNotNull(repo.getInstalledPath("PLUGIN", "ARTIFACT"));

    }

    @Test
    public void testInstallWithAttributes() throws IOException {
        ArtifactManager manager = new ArtifactManager("REPO");
        final ArtifactRepo repo = manager.getRepo();
        repo.load();
        AttributeValuePair[] avp = new AttributeValuePair[]{
                new AttributeValuePair("Organism", "Homo sapiens"),
                new AttributeValuePair("Reference-Build", "hg19"),
        };
        assertNull(repo.find("PLUGIN", "INDEX", avp));
        assertFalse(new File("REPO/artifacts/PLUGIN/INDEX/HOMO_SAPIENS/HG19/index-installed").exists());

        repo.install("PLUGIN", "INDEX", "test-data/install-scripts/install-script3.sh", avp);
        assertNotNull(repo.find("PLUGIN", "INDEX", avp));
        assertEquals(Artifacts.InstallationState.INSTALLED, repo.find("PLUGIN", "INDEX", avp).getState());
        repo.save();
        assertTrue(new File("REPO/artifacts/PLUGIN/INDEX/VERSION/HOMO_SAPIENS/HG19/index-installed").exists());

        repo.remove("PLUGIN", "INDEX", avp);
        assertFalse(new File("REPO/artifacts/PLUGIN/INDEX/VERSION/HOMO_SAPIENS/HG19/index-installed").exists());

    }

    @Test
    public void testNoDoubleInstallationsWithAttributes() throws IOException {
        ArtifactManager manager = new ArtifactManager("REPO");
        final ArtifactRepo repo = manager.getRepo();

        final File dir = new File("REPO/PLUGIN/RANDOM/VERSION");
        if (dir.exists()) {
            assertTrue(dir.listFiles().length <= 1);
        }
        final AttributeValuePair[] attributeValuePairs = {new AttributeValuePair("attribute-A"),
                new AttributeValuePair("attribute-B")};
        repo.install("PLUGIN", "RANDOM", "test-data/install-scripts/install-script8.sh", "VERSION", attributeValuePairs);
        repo.install("PLUGIN", "RANDOM", "test-data/install-scripts/install-script8.sh", "VERSION", attributeValuePairs);
        repo.install("PLUGIN", "RANDOM", "test-data/install-scripts/install-script8.sh", "VERSION", attributeValuePairs);
        repo.install("PLUGIN", "RANDOM", "test-data/install-scripts/install-script8.sh", "VERSION", attributeValuePairs);

        repo.save();
        final Artifacts.Artifact artifact = repo.find("PLUGIN", "RANDOM", "VERSION",
                new AttributeValuePair("attribute-A", "VA"),
                new AttributeValuePair("attribute-B", "VB"));
        assertNotNull(artifact);
        assertEquals(1, new File("REPO/artifacts/PLUGIN/RANDOM/VERSION/VA/VB").listFiles().length);
        assertEquals(Artifacts.InstallationState.INSTALLED, artifact.getState());

    }

    @Test
    public void testMultipleInstallationsWithChangingAttributes() throws IOException {
        ArtifactManager manager = new ArtifactManager("REPO");
        final ArtifactRepo repo = manager.getRepo();

        final File dir = new File("REPO/artifacts/PLUGIN/RANDOM/VERSION");
        if (dir.exists()) {
            assertTrue(dir.listFiles().length <= 1);
        }
        final AttributeValuePair[] attributeValuePairs = {new AttributeValuePair("attribute-A")};
        repo.install("PLUGIN", "RANDOM", "test-data/install-scripts/install-script9.sh", "VERSION", attributeValuePairs);
        clearValues(attributeValuePairs);
        repo.install("PLUGIN", "RANDOM", "test-data/install-scripts/install-script9.sh", "VERSION", attributeValuePairs);
        clearValues(attributeValuePairs);
        repo.install("PLUGIN", "RANDOM", "test-data/install-scripts/install-script9.sh", "VERSION", attributeValuePairs);
        clearValues(attributeValuePairs);
        repo.install("PLUGIN", "RANDOM", "test-data/install-scripts/install-script9.sh", "VERSION", attributeValuePairs);

        repo.save();
        final Artifacts.Artifact artifact = repo.find("PLUGIN", "RANDOM", "VERSION",
                new AttributeValuePair("attribute-A", "A"));
        assertNotNull(artifact);
        assertEquals(1, new File("REPO/artifacts/PLUGIN/RANDOM/VERSION/A").listFiles().length);
        assertEquals(1, new File("REPO/artifacts/PLUGIN/RANDOM/VERSION/B").listFiles().length);
        assertEquals(Artifacts.InstallationState.INSTALLED, artifact.getState());

    }


    @Test
    // test that install can install after a prior run failed.
    public void testFailThenInstall() throws IOException {
        ArtifactManager manager = new ArtifactManager("REPO");
        final ArtifactRepo repo = manager.getRepo();

        final File dir = new File("REPO/artifacts/PLUGIN/RANDOM/VERSION");
        if (dir.exists()) {
            assertTrue(dir.listFiles().length <= 1);
        }

        repo.install("PLUGIN", "RANDOM", "test-data/install-scripts/install-script10.sh", "VERSION");
        repo.install("PLUGIN", "RANDOM", "test-data/install-scripts/install-script9.sh", "VERSION");


        repo.save();
        final Artifacts.Artifact artifact = repo.find("PLUGIN", "RANDOM", "VERSION");
        assertNotNull(artifact);
        assertEquals(1, new File("REPO/artifacts/PLUGIN/RANDOM/VERSION").listFiles().length);
        assertEquals(Artifacts.InstallationState.INSTALLED, artifact.getState());

    }

    @Test
    public void testMissingInstallScript() throws IOException {
        boolean success = false;
        ArtifactRepo repo = new ArtifactRepo(repoDir);
        try {
            repo.install("A", "ARTIFACT", "test-data/install-scripts/MISSING-SCRIPT", "VERSION");
        } catch (IOException e) {
            success = true;
        }
        assertTrue(success);
        assertFalse(repo.isInstalled("A", "ARTIFACT", "VERSION", null));
    }

    @Test
    //  command not found error in an install script is an installation failure, not a success.
    public void testCommandNotFoundInScript() throws IOException {

        ArtifactRepo repo = new ArtifactRepo(repoDir);
        repo.setStepLogDir(stepsLogDir);
        repo.install("PLUGIN", "ARTIFACT", "test-data/install-scripts/install-script-COMMAND_NOT_FOUND.sh", "VERSION");

        assertFalse(repo.isInstalled("A", "ARTIFACT", "VERSION", null));
        repo.writeLog();
        StepsReportBuilder reporter = new StepsReportBuilder(stepsLogDir.listFiles()[0]);
        assertTrue(reporter.summarize().contains("line 13: SJdksjdkjs: command not found"));
    }

    @Test
    public void testPartialInstalls() throws IOException {
        ArtifactRepo repo = new ArtifactRepo(repoDir);

        repo.install("A", "ARTIFACT", "test-data/install-scripts/install-script-A_ARTIFACT.sh", "VERSION", new AttributeValuePair("attribute-A"));
        repo.save();
        repo = new ArtifactRepo(repoDir);
        repo.load(repoDir, true);
        repo.install("B", "ARTIFACT", "test-data/install-scripts/install-script-B_ARTIFACT.sh", "VERSION");
        assertTrue(repo.isInstalled("B", "ARTIFACT", "VERSION", null));

        StringWriter stringWriter = new StringWriter();
        repo.printBashExports(new PrintWriter(stringWriter));
        assertTrue(stringWriter.getBuffer().indexOf("export RESOURCES_ARTIFACTS_A_ARTIFACT_VA=") >= 0);
        assertTrue(stringWriter.getBuffer().indexOf("export RESOURCES_ARTIFACTS_B_ARTIFACT=") >= 0);

// now install C:
        repo = new ArtifactRepo(repoDir);
        repo.load();
        repo.install("C", "ARTIFACT", "test-data/install-scripts/install-script-C_ARTIFACT.sh", "VERSION");
        repo.install("D", "ARTIFACT", "test-data/install-scripts/install-script-D_ARTIFACT.sh", "VERSION");
        assertTrue(repo.isInstalled("C", "ARTIFACT", "VERSION", null));
        assertTrue(repo.isInstalled("D", "ARTIFACT", "VERSION", null));

        stringWriter = new StringWriter();
        repo.printBashExports(new PrintWriter(stringWriter));
        assertTrue(stringWriter.getBuffer().indexOf("export RESOURCES_ARTIFACTS_A_ARTIFACT_VA=") >= 0);
        assertTrue(stringWriter.getBuffer().indexOf("export RESOURCES_ARTIFACTS_B_ARTIFACT=") >= 0);
        assertTrue(stringWriter.getBuffer().indexOf("export RESOURCES_ARTIFACTS_C_ARTIFACT=") >= 0);
        assertTrue(stringWriter.getBuffer().indexOf("export RESOURCES_ARTIFACTS_D_ARTIFACT=") >= 0);

    }

    @Test
    public void testFailInstalling() throws IOException {
        ArtifactManager manager = new ArtifactManager("REPO");
        final ArtifactRepo repo = manager.getRepo();
        manager.failInstalling();

        repo.show();
    }


    @Test
    public void testMultipleLocks() throws IOException {
        ArtifactManager manager = new ArtifactManager("REPO");
        final ArtifactRepo repo = manager.getRepo();
        repo.acquireExclusiveLock();
        repo.acquireExclusiveLock();
        repo.releaseLock();
        repo.releaseLock();


    }

    @Test
    public void loadJSap() throws Exception {
        ArtifactManager manager = new ArtifactManager("REPO");
        Assert.assertNotNull(manager.loadJsapConfig());
    }


    @Test
    public void testExportWrongVersion() throws IOException {
        ArtifactRepo repo = new ArtifactRepo(repoDir);

        repo.install("A", "ARTIFACT", "test-data/install-scripts/install-script-A_ARTIFACT.sh", "1.1", new AttributeValuePair("attribute-A"));
        repo.install("A", "ARTIFACT", "test-data/install-scripts/install-script-A_ARTIFACT.sh", "1.2", new AttributeValuePair("attribute-A"));
        repo.save();
        repo = new ArtifactRepo(repoDir);
        repo.load(repoDir, true);
        repo.install("B", "ARTIFACT", "test-data/install-scripts/install-script-B_ARTIFACT.sh", "VERSION");
        assertTrue(repo.isInstalled("B", "ARTIFACT", "VERSION", null));

        StringWriter stringWriter = new StringWriter();
        repo.printBashExports(new PrintWriter(stringWriter));
        assertTrue(stringWriter.getBuffer().indexOf("export RESOURCES_ARTIFACTS_A_ARTIFACT_VA=") >= 0);
        assertTrue(stringWriter.getBuffer().indexOf("export RESOURCES_ARTIFACTS_B_ARTIFACT=") >= 0);

// now install C:
        repo = new ArtifactRepo(repoDir);
        repo.load();
        repo.install("C", "ARTIFACT", "test-data/install-scripts/install-script-C_ARTIFACT.sh", "VERSION");
        repo.install("D", "ARTIFACT", "test-data/install-scripts/install-script-D_ARTIFACT.sh", "VERSION");
        assertTrue(repo.isInstalled("C", "ARTIFACT", "VERSION", null));
        assertTrue(repo.isInstalled("D", "ARTIFACT", "VERSION", null));

        stringWriter = new StringWriter();
        repo.printBashExports(new PrintWriter(stringWriter));
        assertTrue(stringWriter.getBuffer().indexOf("export RESOURCES_ARTIFACTS_A_ARTIFACT_VA=") >= 0);
        assertTrue(stringWriter.getBuffer().indexOf("export RESOURCES_ARTIFACTS_B_ARTIFACT=") >= 0);
        assertTrue(stringWriter.getBuffer().indexOf("export RESOURCES_ARTIFACTS_C_ARTIFACT=") >= 0);
        assertTrue(stringWriter.getBuffer().indexOf("export RESOURCES_ARTIFACTS_D_ARTIFACT=") >= 0);

    }

    private void clearValues(AttributeValuePair[] attributeValuePairs) {
        for (AttributeValuePair valuePair : attributeValuePairs) {
            valuePair.value = null;
        }
    }


    @Test
    // test that export is correctly generated even when multiple versions of the same artifact were
    // are already installed in the repo.
    // them:
    public void testInstallMultipleVersions() throws IOException {
        ArtifactManager manager = new ArtifactManager("REPO");
        final ArtifactRepo repo = manager.getRepo();
        repo.load();
        assertNull(repo.find("PLUGIN", "ARTIFACT"));
        String[] allVersionsToInstall = {"1.0", "1.1", "1.2", "1.0.1", "1.0.2", "1.1.1"};
        for (String version : allVersionsToInstall) {
            repo.install("PLUGIN1", "A", "test-data/install-scripts/install-script5.sh", version);
        }
        for (String version : allVersionsToInstall) {
            assertNotNull(repo.find("PLUGIN1", "A", version));
            assertEquals(Artifacts.InstallationState.INSTALLED, repo.find("PLUGIN1", "A", version).getState());
        }
        repo.save();

        ExplicitInstallationScope scope = new ExplicitInstallationScope();
        scope.addArtifact("PLUGIN1", "A", "1.0.2");
        repo.setInstallationScope(scope);
        repo.load();
        StringWriter exports = new StringWriter();
        repo.printBashExports(new PrintWriter(exports));

        final String exportString = exports.getBuffer().toString();

        String[] allOtherVersions = {"1.0", "1.1", "1.2", "1.0.1", "1.1.1"}; // does not contain 1.0.2
        for (String version : allOtherVersions) {
            assertFalse("exports must not contain version "+version, exportString.contains("PLUGIN1/A/"+version+"\n"));
        }
        assertTrue("exports must  contain version 1.0.2", exportString.endsWith("PLUGIN1/A/1.0.2\n"));

    }

    @Before
    public void cleanRepo() throws IOException {
        FileUtils.deleteDirectory(repoDir);
        FileUtils.deleteDirectory(stepsLogDir);
        FileUtils.deleteQuietly(new File(System.getenv("TMPDIR") + "/FLAG"));
    }

}
