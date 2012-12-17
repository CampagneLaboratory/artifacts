package artifacts.locks;

import java.io.IOException;

/**
 * @author Fabien Campagne
 *         Date: 12/16/12
 *         Time: 2:56 PM
 */
public interface ExclusiveLockRequest {

    void query();

    boolean granted();

    void waitAndLock() throws IOException;


    /**
     * Release the lock, call after the lock was granted to release.
     */
    public void release() throws IOException;
}
