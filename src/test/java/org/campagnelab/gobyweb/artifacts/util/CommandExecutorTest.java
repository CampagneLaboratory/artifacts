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

package org.campagnelab.gobyweb.artifacts.util;

import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import edu.cornell.med.icb.net.CommandExecutor;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;

/**
 * @author Fabien Campagne
 *         Date: 1/24/13
 *         Time: 6:28 PM
 */
public class CommandExecutorTest {
    @Test
    public void sshTest() throws IOException, InterruptedException {
        CommandExecutor executor = new CommandExecutor(getUserName(), "localhost");
        int status = executor.ssh("date");
        assertEquals(0, status);

    }

    @Test
    public void sshWithEnvironmentTest() throws IOException, InterruptedException {
        CommandExecutor executor = new CommandExecutor(getUserName(), "localhost");
        int status = executor.ssh("echo ${JOB_DIR}", "JOB_DIR=AASSA");
        assertEquals(0, status);

    }

    public String getUserName() {
        return System.getProperty("user.name");
    }
}
