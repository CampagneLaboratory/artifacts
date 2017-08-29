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

package org.campagnelab.gobyweb.artifacts.locks;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

/**
 * @author Fabien Campagne
 *         Date: 12/16/12
 *         Time: 2:59 PM
 */
public class ExclusiveLockRequestWithFile implements ExclusiveLockRequest {

    RandomAccessFile lockFile;
    private FileLock lock;
    private static Logger LOG = Logger.getLogger(ExclusiveLockRequestWithFile.class);
    private final String filename;

    public ExclusiveLockRequestWithFile(String filename, File repoDir) {
        this.filename = filename;
        if (!repoDir.exists()) {
            LOG.warn("Repository directory did not exist, creating..");
            repoDir.mkdir();
        }
        String lockFilename = FilenameUtils.concat(repoDir.getAbsolutePath(), filename);


        try {
            lockFile = new RandomAccessFile(lockFilename, "rw");
        } catch (FileNotFoundException e) {
            LOG.error("Cannot create LOCK on " + this.filename, e);
        }

    }

    boolean granted = false;

    public void query() {

        synchronized (this) {
            try {
                granted = (lock = lockFile.getChannel().tryLock()) != null;
            } catch (IOException e) {
                LOG.error("Could not acquire lock on " + filename, e);
                granted = false;
            }
        }

    }

    /**
     * @return True when the lock was granted. False otherwise.
     */
    public boolean granted() {
        return granted;
    }

    public void waitAndLock() throws IOException {

        synchronized (this) {

            lock = lockFile.getChannel().lock();
            granted = lock != null;
        }
    }

    /**
     * Release the lock, call after the lock was granted to release.
     */
    public void release() throws IOException {
        synchronized (this) {
            boolean success = false;
            if (lock != null && lock.isValid()) {

                    try {
                        // workaround for IOException? see https://groups.google.com/forum/?fromgroups=#!topic/android-developers/gSb_xdlIG5A
                        lockFile.seek(0);
                        lock.release();

                    } catch (IOException e) {

                        LOG.warn("Caught IO exception when trying to release file lock. Ignoring.", e);
                    }

            }
        }
    }

    /**
     * Return the file that was locked.
     *
     * @return
     */
    public RandomAccessFile getLockedFile() {
        return lockFile;
    }
}
