package org.campagnelab.gobyweb.artifacts;


import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Fabien Campagne
 *         Date: 12/19/12
 *         Time: 12:01 PM
 */
public class BuildArtifactRequestTest {

    private String userName;

    @Test
    // check that we can execute requests sent from the web server in pb format.
    public void testOneRequest() throws IOException {
        BuildArtifactRequest request = new BuildArtifactRequest("localhost");
        request.addArtifact("PLUGIN", "FILE1", "1.0", "test-data/install-scripts/install-script1.sh");
        final File output = new File("test-results/requests/request1.pb");

        request.save(output);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
        helper.install(new File("REPO"));

        assertTrue(new File("REPO/artifacts/PLUGIN/FILE1/1.0/installed-file-1").exists());
        assertFalse(new File("REPO/artifacts/PLUGIN/FILE2/1.0/installed-file-2").exists());
        helper.show();
    }

    @Test
    // check that we can execute requests sent from the web server in pb format.
    public void testOneRequestWithUser() throws IOException {
        BuildArtifactRequest request = new BuildArtifactRequest(getUserName() + "@localhost");
        request.addArtifact("PLUGIN", "FILE1", "1.0", "test-data/install-scripts/install-script1.sh");
        final File output = new File("test-results/requests/request1.pb");

        request.save(output);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
        helper.install(new File("REPO"));

        assertTrue(new File("REPO/artifacts/PLUGIN/FILE1/1.0/installed-file-1").exists());
        assertFalse(new File("REPO/artifacts/PLUGIN/FILE2/1.0/installed-file-2").exists());

    }

    @Test
    // check that we can execute requests sent from the web server in pb format.
    public void testBashExport() throws IOException {
        BuildArtifactRequest request = new BuildArtifactRequest(getUserName() + "@localhost");
        request.addArtifact("PLUGIN", "FILE1", "1.0", "test-data/install-scripts/install-script1.sh");
        request.addArtifact("PLUGIN", "FILE2", "1.0", "test-data/install-scripts/install-script1.sh");
        final File output = new File("test-results/requests/request2.pb");

        request.save(output);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
        helper.install(new File("REPO"));
        helper.printBashExports(new File("REPO"));

    }

    @Test
    // check that we can execute requests sent from the web server in pb format.
    public void testUndefinedAttributes() throws IOException {
        BuildArtifactRequest request = new BuildArtifactRequest(getUserName() + "@localhost");
        Artifacts.AttributeValuePair[] attributes = new Artifacts.AttributeValuePair[]{
                Artifacts.AttributeValuePair.newBuilder().setName("key").setValue("value").build(),
                Artifacts.AttributeValuePair.newBuilder().setName("undefined-key").build()
        };
        request.addArtifact("PLUGIN", "INDEX", "1.0", "test-data/install-scripts/install-script3.sh", attributes);
        final File output = new File("test-results/requests/request3.pb");

        request.save(output);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
        final File repoDir = new File("REPO");
        helper.install(repoDir);
        helper.printBashExports(repoDir);
        helper.showRepo(repoDir);
        ArtifactRepo repo = new ArtifactRepo(repoDir);
        repo.load();
        List<Artifacts.Artifact> results = repo.findIgnoringAttributes("PLUGIN", "INDEX", "1.0");
        boolean found = false;
        for (Artifacts.Artifact index : results) {
            for (Artifacts.AttributeValuePair attribute : index.getAttributesList()) {
                if ("undefined-key".equals(attribute.getName())) {
                    // Hello world string must have been normalized when we get it back:
                    assertEquals("HELLO_WORLD_", attribute.getValue());
                    found = true;
                }
            }
        }
        assertTrue("The undefined value attribute must be recorded in the repository after artifact installation.", found);

    }

    @Test
    // check that we can execute requests sent from the web server in pb format.
    public void testRetentionPolicy() throws IOException {
        BuildArtifactRequest request = new BuildArtifactRequest(getUserName() + "@localhost");
        request.addArtifact("PLUGIN", "FILE1", "1.0", "test-data/install-scripts/install-script4.sh", Artifacts.RetentionPolicy.REMOVE_OLDEST);
        request.addArtifact("PLUGIN", "FILE2", "1.0", "test-data/install-scripts/install-script4.sh", Artifacts.RetentionPolicy.REMOVE_OLDEST);
        final File output = new File("test-results/requests/request4.pb");

        request.save(output);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
        helper.setSpaceRepoDirQuota(1000);
        final File repoDir = new File("REPO");
        helper.install(repoDir);
        helper.printBashExports(repoDir);
        helper.showRepo(repoDir);
        helper.prune(repoDir);
        helper.printBashExports(repoDir);

    }

    @Test
    // check that we can execute requests sent from the web server in pb format.
    public void testRetentionPolicy2() throws IOException {
        BuildArtifactRequest request = new BuildArtifactRequest(getUserName() + "@localhost");
        request.addArtifact("PLUGIN", "FILE1", "1.0", "test-data/install-scripts/install-script4.sh");
        request.addArtifact("PLUGIN", "FILE2", "1.0", "test-data/install-scripts/install-script4.sh");
        final File output = new File("test-results/requests/request4.pb");

        request.save(output);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
        helper.setSpaceRepoDirQuota(1000);
        final File repoDir = new File("REPO");
        helper.install(repoDir);
        helper.printBashExports(repoDir);
        helper.showRepo(repoDir);
        helper.prune(repoDir);
        helper.printBashExports(repoDir);

    }

    @Test
    // check that we can execute requests sent from the web server in pb format.
    public void testBashExportForRequests() throws IOException {
        BuildArtifactRequest request = new BuildArtifactRequest(getUserName() + "@localhost");
        Artifacts.AttributeValuePair avp1 = Artifacts.AttributeValuePair.newBuilder().setName("attribute-A").build();
        Artifacts.AttributeValuePair avp2 = Artifacts.AttributeValuePair.newBuilder().setName("attribute-B").build();

        request.addArtifact("PLUGIN", "FILE1", "1.0", "test-data/install-scripts/install-script7.sh", avp1, avp2);
        request.addArtifact("PLUGIN", "FILE2", "1.0", "test-data/install-scripts/install-script7.sh", avp2);
        request.addArtifact("PLUGIN", "NO-ATTRIBUTE", "1.0", "test-data/install-scripts/install-script7.sh");
        final File output = new File("test-results/requests/request4.pb");

        request.save(output);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
        helper.setSpaceRepoDirQuota(1000);
        final File repoDir = new File("REPO");
        helper.install(repoDir);
        final StringWriter resultRequest = new StringWriter();

        helper.printBashExports(repoDir, new PrintWriter(resultRequest));
        System.out.println(resultRequest.getBuffer());
        assertTrue(resultRequest.getBuffer().indexOf("export RESOURCES_ARTIFACTS_PLUGIN_FILE1_VA_VB=") >= 0);
        assertTrue(resultRequest.getBuffer().indexOf("export RESOURCES_ARTIFACTS_PLUGIN_FILE2_VB=") >= 0);
//        assertTrue(resultRequest.getBuffer().indexOf("export RESOURCES_ARTIFACTS_PLUGIN_NO-ATTRIBUTE=") >= 0);
        assertTrue(resultRequest.getBuffer().indexOf("export RESOURCES_ARTIFACTS_PLUGIN_FILE1_ATTRIBUTE_A=VA") >= 0);
        assertTrue(resultRequest.getBuffer().indexOf("export RESOURCES_ARTIFACTS_PLUGIN_FILE1_ATTRIBUTE_B=VB") >= 0);
        assertTrue(resultRequest.getBuffer().indexOf("export RESOURCES_ARTIFACTS_PLUGIN_FILE2_ATTRIBUTE_A=VA") < 0);
        assertTrue(resultRequest.getBuffer().indexOf("export RESOURCES_ARTIFACTS_PLUGIN_FILE2_ATTRIBUTE_B=VB") >= 0);

        helper.showRepo(repoDir);


    }

    @Test
    public void testNoDoubleInstallationsWithAttributes() throws IOException {
        final File dir = new File("REPO/artifacts/PLUGIN/RANDOM/VERSION");
        if (dir.exists()) {
            assertTrue(dir.listFiles().length <= 1);
        }
        BuildArtifactRequest request = new BuildArtifactRequest(getUserName() + "@localhost");
        Artifacts.AttributeValuePair avp1 = Artifacts.AttributeValuePair.newBuilder().setName("attribute-A").build();
        Artifacts.AttributeValuePair avp2 = Artifacts.AttributeValuePair.newBuilder().setName("attribute-B").build();

        request.addArtifact("PLUGIN", "RANDOM", "VERSION", "test-data/install-scripts/install-script8.sh", avp1, avp2);
        request.addArtifact("PLUGIN", "RANDOM", "VERSION", "test-data/install-scripts/install-script8.sh", avp1, avp2);
        request.addArtifact("PLUGIN", "RANDOM", "VERSION", "test-data/install-scripts/install-script8.sh", avp1, avp2);


        final File output = new File("test-results/requests/request4.pb");

        request.save(output);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
        helper.setSpaceRepoDirQuota(1000000000);
        final File repoDir = new File("REPO");
        helper.install(repoDir);
        ArtifactRepo repo = new ArtifactRepo(repoDir);
        repo.load();
        final Artifacts.Artifact artifact = repo.find("PLUGIN", "RANDOM", "VERSION",
                new AttributeValuePair("attribute-A", "VA"),
                new AttributeValuePair("attribute-B", "VB"));
        assertNotNull(artifact);
        assertEquals(1, new File("REPO/artifacts/PLUGIN/RANDOM/VERSION/VA/VB").listFiles().length);
        assertEquals(Artifacts.InstallationState.INSTALLED, artifact.getState());


    }

    @Test
    public void testPartialInstalls() throws IOException {
        BuildArtifactRequest request = new BuildArtifactRequest(getUserName() + "@localhost");

        request.install("A", "ARTIFACT", "test-data/install-scripts/install-script-A_ARTIFACT.sh", "VERSION");

        request.install("B", "ARTIFACT", "test-data/install-scripts/install-script-B_ARTIFACT.sh", "VERSION");
        request.install("C", "ARTIFACT", "test-data/install-scripts/install-script-C_ARTIFACT.sh", "VERSION");
        request.install("D", "ARTIFACT", "test-data/install-scripts/install-script-D_ARTIFACT.sh", "VERSION");


        final File output = new File("test-results/requests/request5.pb");

        request.save(output);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
        helper.setSpaceRepoDirQuota(1000000000);
        final File repoDir = new File("REPO");
        helper.install(repoDir);
        StringWriter stringWriter = new StringWriter();
        helper.printBashExports(repoDir, new PrintWriter(stringWriter));
        System.out.println(stringWriter.toString());
        assertTrue(stringWriter.getBuffer().indexOf("export RESOURCES_ARTIFACTS_A_ARTIFACT=") >= 0);
        assertTrue(stringWriter.getBuffer().indexOf("export RESOURCES_ARTIFACTS_B_ARTIFACT=") >= 0);
        assertTrue(stringWriter.getBuffer().indexOf("export RESOURCES_ARTIFACTS_C_ARTIFACT=") >= 0);
        assertTrue(stringWriter.getBuffer().indexOf("export RESOURCES_ARTIFACTS_D_ARTIFACT=") >= 0);

    }

    @Before
    public void cleanRepo() throws IOException {
        FileUtils.deleteDirectory(new File("REPO"));
        new File("test-results/requests").mkdirs();
    }

    public String getUserName() {
        return System.getProperty("user.name");
    }
}
