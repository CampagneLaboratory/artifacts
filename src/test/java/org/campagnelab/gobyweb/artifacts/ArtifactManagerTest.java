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
        assertFalse(new File("REPO/PLUGIN/INDEX/HOMO_SAPIENS/HG19/index-installed").exists());

        repo.install("PLUGIN", "INDEX", "test-data/install-scripts/install-script3.sh", avp);
        assertNotNull(repo.find("PLUGIN", "INDEX", avp));
        assertEquals(Artifacts.InstallationState.INSTALLED, repo.find("PLUGIN", "INDEX", avp).getState());
        repo.save();
        assertTrue(new File("REPO/PLUGIN/INDEX/VERSION/HOMO_SAPIENS/HG19/index-installed").exists());

        repo.remove("PLUGIN", "INDEX", avp);
        assertFalse(new File("REPO/PLUGIN/INDEX/VERSION/HOMO_SAPIENS/HG19/index-installed").exists());

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
        assertNotNull(artifact );
        assertEquals(1, new File("REPO/PLUGIN/RANDOM/VERSION/VA/VB").listFiles().length);
        assertEquals(Artifacts.InstallationState.INSTALLED, artifact.getState());

    }




    @Before
    public void cleanRepo() throws IOException {
        FileUtils.deleteDirectory(new File("REPO"));

    }

}
