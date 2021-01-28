package com.oracle.coherence.k8s.testing;

import com.tangosol.net.DefaultCacheServer;

/**
 * Main class.
 *
 * @author Jonathan Knight
 */
public class Main {
    /**
     * Private constructor.
     */
    private Main() {
    }

    public static void main(String[] args) {
        DefaultCacheServer.main(args);
    }
}