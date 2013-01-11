package org.campagnelab.gobyweb.artifacts.util;

import org.apache.log4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Class copied and adapted from http://stackoverflow.com/questions/4157303/how-to-execute-cmd-commands-via-java
 *
 * @author Fabien Campagne
 *         Date: 1/10/13
 *         Time: 4:06 PM
 */

public class SyncPipe implements Runnable {
    private final Logger logger;

    public SyncPipe(InputStream istrm, OutputStream ostrm, Logger logger) {
        istrm_ = istrm;
        ostrm_ = ostrm;
        this.logger = logger;
    }

    public void run() {
        try {
            final byte[] buffer = new byte[1024];
            for (int length = 0; (length = istrm_.read(buffer)) != -1; ) {
                ostrm_.write(buffer, 0, length);
            }
            ostrm_.flush();
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private final OutputStream ostrm_;
    private final InputStream istrm_;
}