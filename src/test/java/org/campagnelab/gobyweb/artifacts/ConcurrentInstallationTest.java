package org.campagnelab.gobyweb.artifacts;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * @author Fabien Campagne
 *         Date: 1/4/13
 *         Time: 11:11 AM
 */
public class ConcurrentInstallationTest {

    @Test
    public void testConcurrentInstallations1() throws IOException {
        ArtifactManager manager = new ArtifactManager("REPO");
        final ArtifactRepo repo = manager.getRepo();
        repo.load();

        final File dir = new File("REPO/artifacts/PLUGIN/RANDOM/VERSION");
        if (dir.exists()) {
            assertTrue(dir.listFiles().length <= 1);
        }


        repo.install("PLUGIN", "RANDOM", "test-data/install-scripts/install-script2.sh");

        repo.save();
        assertNotNull(repo.find("PLUGIN", "RANDOM"));
        assertEquals(1, new File("REPO/artifacts/PLUGIN/RANDOM/VERSION/").listFiles().length);
        assertEquals(Artifacts.InstallationState.INSTALLED, repo.find("PLUGIN", "RANDOM").getState());


    }

    @Test
    public void testConcurrentInstallations2() throws IOException {
        ArtifactManager manager = new ArtifactManager("REPO");
        final ArtifactRepo repo = manager.getRepo();
        repo.load();

        final File dir = new File("REPO/artifacts/PLUGIN/RANDOM/VERSION");
        if (dir.exists()) {
            assertTrue(dir.listFiles().length <= 1);
        }


        repo.install("PLUGIN", "RANDOM", "test-data/install-scripts/install-script2.sh");
        repo.save();
        assertNotNull(repo.find("PLUGIN", "RANDOM"));
        assertEquals(1, new File("REPO/artifacts/PLUGIN/RANDOM/VERSION/").listFiles().length);
        assertEquals(Artifacts.InstallationState.INSTALLED, repo.find("PLUGIN", "RANDOM").getState());


    }

    @BeforeClass
    public static void cleanRepo() throws IOException {
        FileUtils.deleteDirectory(new File("REPO"));

    }
}
