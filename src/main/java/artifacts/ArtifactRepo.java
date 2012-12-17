package artifacts;

import artifacts.locks.ExclusiveLockRequest;
import artifacts.locks.ExclusiveLockRequestWithFile;

import java.io.File;
import java.io.IOException;

/**
 * @author Fabien Campagne
 *         Date: 12/16/12
 *         Time: 12:12 PM
 */
public class ArtifactRepo {
    File repoDir;

    public ArtifactRepo(File repoDir) {
        this.repoDir = repoDir;
    }

    /**
     * @param pluginId
     * @param artifactId
     */
    public void install(String pluginId, String artifactId) {

    }

    public void load(File repoDir) {
        System.out.println("Loading from " + repoDir.getAbsolutePath());

        // start LockManager
    }

    ExclusiveLockRequest request;

    public void acquireExclusiveLock() throws IOException {
        request  = new ExclusiveLockRequestWithFile(repoDir);
        boolean done = false;
        do {
            request.waitAndLock();

            request.query();

            if (request.granted()) {
             done=true;
            } else {
                // wait a bit.
                try {
                    Thread.currentThread().sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } while (!done);
    }

    public void releaseLock() throws IOException {
        request.release();
    }
}
