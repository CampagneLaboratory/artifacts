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
    public void testOneRequestWithNoServer() throws IOException {
        BuildArtifactRequest request = new BuildArtifactRequest();
        request.addArtifact("PLUGIN", "FILE1", "1.0", false, "test-data/install-scripts/install-script1.sh");
        final File output = new File("test-results/requests/request1.pb");

        request.save(output);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
        helper.install(new File("REPO"),false);

        assertTrue(new File("REPO/artifacts/PLUGIN/FILE1/1.0/installed-file-1").exists());
        assertFalse(new File("REPO/artifacts/PLUGIN/FILE2/1.0/installed-file-2").exists());
        helper.show();
    }

    @Test
    // check that we can execute requests sent from the web server in pb format.
    public void testOneRequest() throws IOException {
        BuildArtifactRequest request = new BuildArtifactRequest("localhost");
        request.addArtifact("PLUGIN", "FILE1", "1.0", false, "test-data/install-scripts/install-script1.sh");
        final File output = new File("test-results/requests/request1.pb");

        request.save(output);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
        helper.install(new File("REPO"),false);

        assertTrue(new File("REPO/artifacts/PLUGIN/FILE1/1.0/installed-file-1").exists());
        assertFalse(new File("REPO/artifacts/PLUGIN/FILE2/1.0/installed-file-2").exists());
        helper.show();
    }

    @Test
    // check that we can execute requests sent from the web server in pb format.
    public void testOneRequestWithUser() throws IOException {
        BuildArtifactRequest request = new BuildArtifactRequest(getUserName() + "@localhost");
        request.addArtifact("PLUGIN", "FILE1", "1.0", false, "test-data/install-scripts/install-script1.sh");
        final File output = new File("test-results/requests/request1.pb");

        request.save(output);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
        helper.install(new File("REPO"),false);

        assertTrue(new File("REPO/artifacts/PLUGIN/FILE1/1.0/installed-file-1").exists());
        assertFalse(new File("REPO/artifacts/PLUGIN/FILE2/1.0/installed-file-2").exists());

    }

    @Test
    // check that we can execute requests sent from the web server in pb format.
    public void testBashExport() throws IOException {
        BuildArtifactRequest request = new BuildArtifactRequest(getUserName() + "@localhost");
        request.addArtifact("PLUGIN", "FILE1", "1.0", false, "test-data/install-scripts/install-script1.sh");
        request.addArtifact("PLUGIN", "FILE2", "1.0", false, "test-data/install-scripts/install-script1.sh");
        final File output = new File("test-results/requests/request2.pb");

        request.save(output);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
        helper.install(new File("REPO"),false);
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
        request.addArtifact("PLUGIN", "INDEX", "1.0", false, "test-data/install-scripts/install-script3.sh", attributes);
        final File output = new File("test-results/requests/request3.pb");

        request.save(output);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
        final File repoDir = new File("REPO");
        helper.install(repoDir,false);
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
        request.addArtifact("PLUGIN", "FILE1", "1.0", false, "test-data/install-scripts/install-script4.sh", Artifacts.RetentionPolicy.REMOVE_OLDEST);
        request.addArtifact("PLUGIN", "FILE2", "1.0", false, "test-data/install-scripts/install-script4.sh", Artifacts.RetentionPolicy.REMOVE_OLDEST);
        final File output = new File("test-results/requests/request4.pb");

        request.save(output);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
        helper.setSpaceRepoDirQuota(1000);
        final File repoDir = new File("REPO");
        helper.install(repoDir,false);
        helper.printBashExports(repoDir);
        helper.showRepo(repoDir);
        helper.prune(repoDir);
        helper.printBashExports(repoDir);

    }

    @Test
    // check that we can execute requests sent from the web server in pb format.
    public void testRetentionPolicy2() throws IOException {
        BuildArtifactRequest request = new BuildArtifactRequest(getUserName() + "@localhost");
        request.addArtifact("PLUGIN", "FILE1", "1.0", false, "test-data/install-scripts/install-script4.sh");
        request.addArtifact("PLUGIN", "FILE2", "1.0", false, "test-data/install-scripts/install-script4.sh");
        final File output = new File("test-results/requests/request4.pb");

        request.save(output);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
        helper.setSpaceRepoDirQuota(1000);
        final File repoDir = new File("REPO");
        helper.install(repoDir,false);
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

        request.addArtifact("PLUGIN", "FILE1", "1.0", false, "test-data/install-scripts/install-script7.sh", avp1, avp2);
        request.addArtifact("PLUGIN", "FILE2", "1.0", false, "test-data/install-scripts/install-script7.sh", avp2);
        request.addArtifact("PLUGIN", "NO-ATTRIBUTE", "1.0", false, "test-data/install-scripts/install-script7.sh");
        final File output = new File("test-results/requests/request4.pb");

        request.save(output);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
        helper.setSpaceRepoDirQuota(1000);
        final File repoDir = new File("REPO");
        helper.install(repoDir,false);
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

        request.addArtifact("PLUGIN", "RANDOM", "VERSION", false, "test-data/install-scripts/install-script8.sh", avp1, avp2);
        request.addArtifact("PLUGIN", "RANDOM", "VERSION", false, "test-data/install-scripts/install-script8.sh", avp1, avp2);
        request.addArtifact("PLUGIN", "RANDOM", "VERSION", false, "test-data/install-scripts/install-script8.sh", avp1, avp2);


        final File output = new File("test-results/requests/request4.pb");

        request.save(output);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
        helper.setSpaceRepoDirQuota(1000000000);
        final File repoDir = new File("REPO");
        helper.install(repoDir,false);
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
        Artifacts.AttributeValuePair avp1 = Artifacts.AttributeValuePair.newBuilder().setName("attribute-A").build();

        request.install("A", "ARTIFACT", "test-data/install-scripts/install-script-A_ARTIFACT.sh", "VERSION", false, avp1);
        request.install("B", "ARTIFACT", "test-data/install-scripts/install-script-B_ARTIFACT.sh", "VERSION", false);
        request.install("C", "ARTIFACT", "test-data/install-scripts/install-script-C_ARTIFACT.sh", "VERSION", false);
        request.install("D", "ARTIFACT", "test-data/install-scripts/install-script-D_ARTIFACT.sh", "VERSION", false);


        final File output = new File("test-results/requests/request5.pb");

        request.save(output);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
        helper.setSpaceRepoDirQuota(1000000000);
        final File repoDir = new File("REPO");
        helper.install(repoDir);
        StringWriter stringWriter = new StringWriter();
        helper.printBashExports(repoDir, new PrintWriter(stringWriter));
        System.out.println(stringWriter.toString());
        assertTrue(stringWriter.getBuffer().indexOf("export RESOURCES_ARTIFACTS_A_ARTIFACT_VA=") >= 0);
        assertTrue(stringWriter.getBuffer().indexOf("export RESOURCES_ARTIFACTS_B_ARTIFACT=") >= 0);
        assertTrue(stringWriter.getBuffer().indexOf("export RESOURCES_ARTIFACTS_C_ARTIFACT=") >= 0);
        assertTrue(stringWriter.getBuffer().indexOf("export RESOURCES_ARTIFACTS_D_ARTIFACT=") >= 0);

    }

    @Test
    public void testMandatoryArtifacts() throws IOException {
        BuildArtifactRequest request = new BuildArtifactRequest(getUserName() + "@localhost");
        request.addArtifact("A", "ARTIFACT", "1.2", true, "test-data/install-scripts/install-script-A_ARTIFACT.sh");
        request.addArtifact("A1", "ARTIFACT", "1.1", false, "test-data/install-scripts/install-script-A_ARTIFACT.sh");
        request.addArtifact("A2", "ARTIFACT", "1.1", true, "test-data/install-scripts/install-script-A_ARTIFACT.sh");

        final File output = new File("test-results/requests/mandatory.pb");
        request.save(output);
        ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
        helper.setSpaceRepoDirQuota(1000000000);
        final File repoDir = new File("REPO-ONLY_MANDATORY");
        helper.install(repoDir,true);
        StringWriter stringWriter = new StringWriter();
        helper.printBashExports(repoDir, new PrintWriter(stringWriter));
        System.out.println(stringWriter.toString());
    }


    @Test
    public void testWrongExports() throws IOException {
        BuildArtifactRequest request = new BuildArtifactRequest(getUserName() + "@localhost");
        Artifacts.AttributeValuePair avp1 = Artifacts.AttributeValuePair.newBuilder().setName("attribute-A").build();

        request.install("A", "ARTIFACT", "test-data/install-scripts/install-script-A_ARTIFACT.sh", "1.2", false, avp1);
        request.install("A", "ARTIFACT", "test-data/install-scripts/install-script-A_ARTIFACT.sh", "1.1", false, avp1);
        request.install("A", "ARTIFACT", "test-data/install-scripts/install-script-A_ARTIFACT.sh", "1.1.1", false, avp1);


        final File output = new File("test-results/requests/request-wrong-1.pb");

        //install three versions of A:
        request.save(output);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
        helper.setSpaceRepoDirQuota(1000000000);
        final File repoDir = new File("REPO");
        helper.install(repoDir,false);

        BuildArtifactRequest requestUsed = new BuildArtifactRequest(getUserName() + "@localhost");
        final File outputUsed = new File("test-results/requests/request-wrong-2.pb");

        //a request with only one version of A (1.2):
        requestUsed.install("A", "ARTIFACT", "test-data/install-scripts/install-script-A_ARTIFACT.sh", "1.2", false, avp1);
        requestUsed.save(outputUsed);

        ArtifactRequestHelper helperUsed = new ArtifactRequestHelper(outputUsed);
        helperUsed.setSpaceRepoDirQuota(1000000000);

        helperUsed.install(repoDir,false);

        StringWriter stringWriterUsed = new StringWriter();
        helperUsed.printBashExports(repoDir, new PrintWriter(stringWriterUsed));
        System.out.flush();
        System.out.println(stringWriterUsed.toString());
        assertTrue(stringWriterUsed.getBuffer().indexOf("export RESOURCES_ARTIFACTS_A_ARTIFACT_VA=") >= 0);
        // version 1.2 must be found
        assertTrue("version 1.2 must be found", stringWriterUsed.getBuffer().indexOf("artifacts/A/ARTIFACT/1.2/VA") >= 0);
        assertTrue("version 1.1 must not be found",stringWriterUsed.getBuffer().indexOf("artifacts/A/ARTIFACT/1.1/VA") == -1);
        assertTrue("version 1.1.1 must not be found",stringWriterUsed.getBuffer().indexOf("artifacts/A/ARTIFACT/1.1.1/VA") == -1);


    }


    @Test
    /**
     * Check that the end user can remove the cached install script from the repository to trigger a new fetch of the script.
     */
    public void testRemoveCachedInstallScript() throws IOException {
        BuildArtifactRequest request1 = new BuildArtifactRequest(getUserName() + "@localhost");
        BuildArtifactRequest request2 = new BuildArtifactRequest(getUserName() + "@localhost");
        Artifacts.AttributeValuePair avp1 = Artifacts.AttributeValuePair.newBuilder().setName("attribute-A").build();

        request1.install("A", "ARTIFACT", "test-data/install-scripts/install-script-A_ARTIFACT.sh", "VERSION", false, avp1);
        request1.install("B", "ARTIFACT", "test-data/install-scripts/install-script-B_ARTIFACT.sh", "VERSION", false);

        request2.install("A", "ARTIFACT", "test-data/install-scripts/install-script-A_ARTIFACT.sh", "VERSION", false, avp1);
        request2.install("B", "ARTIFACT", "test-data/install-scripts/install-script-B_ARTIFACT.sh", "VERSION", false);
        request2.install("C", "ARTIFACT", "test-data/install-scripts/install-script-C_ARTIFACT.sh", "VERSION", false);
        request2.install("D", "ARTIFACT", "test-data/install-scripts/install-script-D_ARTIFACT.sh", "VERSION", false);


        final File output1 = new File("test-results/requests/request5.pb");
        final File output2 = new File("test-results/requests/request6.pb");

        request1.save(output1);
        request2.save(output2);


        // install request1:
        ArtifactRequestHelper helper = new ArtifactRequestHelper(output1);
        helper.setSpaceRepoDirQuota(1000000000);
        final File repoDir = new File("REPO");
        helper.install(repoDir,false);
        StringWriter stringWriter = new StringWriter();
        helper.printBashExports(repoDir, new PrintWriter(stringWriter));
        System.out.println(stringWriter.toString());
        assertTrue(stringWriter.getBuffer().indexOf("export RESOURCES_ARTIFACTS_A_ARTIFACT_VA=") >= 0);
        assertTrue(stringWriter.getBuffer().indexOf("export RESOURCES_ARTIFACTS_B_ARTIFACT=") >= 0);


        // now we delete script-A:
        final String cachedInstallScriptA = "REPO/scripts/A/VERSION/install.sh";

        final File file = new File(cachedInstallScriptA);
        assertTrue("cached install script must exist", file.exists());
        file.delete();

        // we install request2:
        helper = new ArtifactRequestHelper(output2);
        helper.setSpaceRepoDirQuota(1000000000);

        helper.install(repoDir,false);
        StringWriter stringWriter2 = new StringWriter();
        helper.printBashExports(repoDir, new PrintWriter(stringWriter2));
        System.out.println(stringWriter2.toString());
        final StringBuffer buffer2 = stringWriter2.getBuffer();
        assertTrue(buffer2.indexOf("export RESOURCES_ARTIFACTS_A_ARTIFACT_VA=") >= 0);
        assertTrue(buffer2.indexOf("export RESOURCES_ARTIFACTS_B_ARTIFACT=") >= 0);
        assertTrue(buffer2.indexOf("export RESOURCES_ARTIFACTS_C_ARTIFACT=") >= 0);
        assertTrue(buffer2.indexOf("export RESOURCES_ARTIFACTS_D_ARTIFACT=") >= 0);


        ArtifactRepo repo = new ArtifactRepo(repoDir);
        repo.load();
        //    assertTrue("cached install script must have been refetched", repo.hasCachedInstallationScript("A"));
        assertTrue("cached install script must have been refetched", file.exists());
        //repo.readAttributeValues()
    }


    @Test
    // check that we can execute requests sent from the web server in pb format.
    public void testEnvironmentCollectionScripts() throws IOException {
        BuildArtifactRequest request = new BuildArtifactRequest(getUserName() + "@localhost");
        request.registerEnvironmentCollection("test-data/env-scripts/env-script1.sh");
        request.registerEnvironmentCollection("test-data/env-scripts/env-script2.sh");
        Artifacts.AttributeValuePair avp1 = Artifacts.AttributeValuePair.newBuilder().setName("attribute-A").build();
        Artifacts.AttributeValuePair avp2 = Artifacts.AttributeValuePair.newBuilder().setName("attribute-B").build();

        request.addArtifact("PLUGIN", "FILE1", "1.0", false, "test-data/install-scripts/install-script11.sh", avp1, avp2);
        request.addArtifact("PLUGIN", "FILE2", "1.0", false, "test-data/install-scripts/install-script11.sh", avp2);
        request.addArtifact("PLUGIN", "NO-ATTRIBUTE", "1.0", false,"test-data/install-scripts/install-script11.sh");
        final File output = new File("test-results/requests/request4.pb");

        request.save(output);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(output);
        helper.setSpaceRepoDirQuota(1000);
        final File repoDir = new File("REPO");
        helper.install(repoDir,false);
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

    @Before
    public void cleanRepo() throws IOException {
        FileUtils.deleteDirectory(new File("REPO"));
        new File("test-results/requests").mkdirs();
    }

    public String getUserName() {
        return System.getProperty("user.name");
    }
}
