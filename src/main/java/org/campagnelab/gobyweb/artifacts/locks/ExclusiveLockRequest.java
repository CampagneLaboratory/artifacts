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

import java.io.IOException;
import java.io.RandomAccessFile;

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

    /**
     * Return the file that was locked.
     *
     * @return
     */
    public RandomAccessFile getLockedFile();
}
