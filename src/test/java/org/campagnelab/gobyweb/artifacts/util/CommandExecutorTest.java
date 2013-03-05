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
