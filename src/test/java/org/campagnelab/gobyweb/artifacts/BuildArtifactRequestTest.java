package org.campagnelab.gobyweb.artifacts;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * @author Fabien Campagne
 *         Date: 12/19/12
 *         Time: 12:01 PM
 */
public class BuildArtifactRequestTest {

    @Test
    // check that we can execute requests sent from the web server in pb format.
    public void testOneRequest() throws IOException {
        BuildArtifactRequest request = new BuildArtifactRequest("localhost");
        request.addArtifact("PLUGIN", "FILE1", "1.0", "test-data/install-scripts/install-script1.sh");
        final File output = new File("test-results/requests/request1.pb");

        request.save(output);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
        helper.install(new File("REPO"));

        assertTrue(new File("REPO/PLUGIN/FILE1/1.0/installed-file-1").exists());
        assertFalse(new File("REPO/PLUGIN/FILE2/1.0/installed-file-2").exists());

    }

    @Test
    // check that we can execute requests sent from the web server in pb format.
    public void testOneRequestWithUser() throws IOException {
        BuildArtifactRequest request = new BuildArtifactRequest("campagne@localhost");
        request.addArtifact("PLUGIN", "FILE1", "1.0", "test-data/install-scripts/install-script1.sh");
        final File output = new File("test-results/requests/request1.pb");

        request.save(output);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
        helper.install(new File("REPO"));

        assertTrue(new File("REPO/PLUGIN/FILE1/1.0/installed-file-1").exists());
        assertFalse(new File("REPO/PLUGIN/FILE2/1.0/installed-file-2").exists());

    }

    @Test
       // check that we can execute requests sent from the web server in pb format.
       public void testBashExport() throws IOException {
           BuildArtifactRequest request = new BuildArtifactRequest("campagne@localhost");
           request.addArtifact("PLUGIN", "FILE1", "1.0", "test-data/install-scripts/install-script1.sh");
           request.addArtifact("PLUGIN", "FILE2", "1.0", "test-data/install-scripts/install-script1.sh");
           final File output = new File("test-results/requests/request2.pb");

           request.save(output);

           ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
           helper.install(new File("REPO"));
           helper.printBashExports(new File("REPO"));



       }

    @Before
    public void cleanRepo() throws IOException {
        FileUtils.deleteDirectory(new File("REPO"));

    }
}
