/*
 * Copyright (c) [2012-2017] [Weill Cornell Medical College]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
