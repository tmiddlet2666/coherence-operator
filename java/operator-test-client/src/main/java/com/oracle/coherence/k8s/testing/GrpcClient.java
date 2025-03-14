/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.k8s.testing;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

/**
 * A simple Coherence gRPC client.
 */
public class GrpcClient {

    /**
     * Private constructor for utility class.
     */
    private GrpcClient() {
    }

    /**
     * Run the gRPC client.
     *
     * @param args  the program arguments.
     */
    public static void main(String[] args) {
        try {
            Coherence coherence = Coherence.client(Coherence.Mode.GrpcFixed).startAndWait();
            Session   session   = coherence.getSession();

            System.out.println("Getting cache 'test' from gRPC session");
            NamedCache<String, String> cache = session.getCache("test");
            System.out.println("Putting key and value into cache 'test'");
            cache.put("key-1", "value-1");

            System.out.println("Test completed successfully");
            System.exit(0);
        }
        catch (Throwable e) {
            e.printStackTrace();
            System.out.println("Test failed");
            System.exit(1);
        }
    }
}
