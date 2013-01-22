package org.campagnelab.gobyweb.artifacts.util;

import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * @author Fabien Campagne
 *         Date: 1/22/13
 *         Time: 9:48 AM
 */
public class CommandExecutor {
    private static final org.apache.log4j.Logger LOG = Logger.getLogger(CommandExecutor.class);

    private String remoteServer;
    private String username;

    /**
     * Construct a command executor with username and remote server.
     * @param username   user for login on remote server.
     * @param remoteServer Name of remote server.
     */
    public CommandExecutor(String username, String remoteServer) {
        this.remoteServer = remoteServer;
        this.username = username;
    }

    /**
     * Remote copy files from remote server.
     * @param remotePath   the path on the remote server
     * @param localFilename the local destination path.
     * @return scp return status.
     * @throws IOException
     * @throws InterruptedException
     */
    public int scp(String remotePath, String localFilename) throws IOException, InterruptedException {

        return exec(String.format("scp -o StrictHostKeyChecking=no %s@%s:%s %s", username, remoteServer, remotePath, localFilename));
    }

    /**
     * Execute a command on the remote server.
     * @param command Command line to execute.
     * @return ssh return status.
     * @throws IOException
     * @throws InterruptedException
     */
    public int ssh(String command) throws IOException, InterruptedException {
        return exec(String.format("ssh -o StrictHostKeyChecking=no %s@%s %s", username, remoteServer, command));
    }

    private int exec(String command) throws IOException, InterruptedException {
        Runtime rt = Runtime.getRuntime();
        String[] commands = command.split(" ");
        Process pr = rt.exec(commands);
        if (LOG.isTraceEnabled()) LOG.trace("executing command: " + command);
        new Thread(new SyncPipe(pr.getErrorStream(), System.err, LOG)).start();
        new Thread(new SyncPipe(pr.getInputStream(), System.out, LOG)).start();

        int exitVal = pr.waitFor();
        if (LOG.isTraceEnabled()) LOG.trace("Remote command  exited with error code " + exitVal);
        return exitVal;
    }

}
