package org.campagnelab.gobyweb.artifacts;


import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertNotNull;

/**
 * Unit test for simple App.
 */
public class ArtifactManagerTest {


    @Test
    public void testInstall() throws IOException {
        ArtifactManager manager = new ArtifactManager("REPO");
        final ArtifactRepo repo = manager.getRepo();
        repo.load();
        assertNull(repo.find("PLUGIN", "ARTIFACT"));
        repo.install("PLUGIN", "ARTIFACT");
        assertNotNull(repo.find("PLUGIN", "ARTIFACT"));
        repo.save();
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
    public void testInstallScript1() throws IOException {
        ArtifactManager manager = new ArtifactManager("REPO");
        final ArtifactRepo repo = manager.getRepo();
        repo.load();
        assertNull(repo.find("PLUGIN", "FILE1"));
        assertFalse(new File("REPO/PLUGIN/FILE1/installed-file-1").exists());
        assertFalse(new File("REPO/PLUGIN/FILE1/installed-file-2").exists());

        repo.install("PLUGIN", "FILE1", "test-data/install-scripts/install-script1.sh");
        assertNotNull(repo.find("PLUGIN", "FILE1"));
        assertEquals(Artifacts.InstallationState.INSTALLED, repo.find("PLUGIN", "FILE1").getState());
        repo.save();
        assertTrue(new File("REPO/PLUGIN/FILE1/VERSION/installed-file-1").exists());
        assertFalse(new File("REPO/PLUGIN/FILE1/VERSION/installed-file-2").exists());

        repo.install("PLUGIN", "FILE2", "test-data/install-scripts/install-script1.sh");
        assertTrue(new File("REPO/PLUGIN/FILE2/VERSION/installed-file-2").exists());
        repo.remove("PLUGIN", "FILE1");
        assertFalse(new File("REPO/PLUGIN/FILE1/VERSION/installed-file-1").exists());
        repo.remove("PLUGIN", "FILE2");
        assertFalse(new File("REPO/PLUGIN/FILE2/VERSION/installed-file-2").exists());

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

    @Before
    public void cleanRepo() throws IOException {
        FileUtils.deleteDirectory(new File("REPO"));

    }

}
