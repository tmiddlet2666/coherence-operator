/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.oracle.coherence.common.base.Logger;
import com.tangosol.net.NamedCache;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonStructure;

import io.helidon.common.http.Http;
import io.helidon.media.jsonp.server.JsonSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerConfiguration;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.WebServer;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.util.QueryHelper;

/**
 * @author jk  2019.03.21
 */
public class Main {

    // ---- constructors ----------------------------------------------------

    /**
     * Private constructor to stop instantiation.
     */
    private Main() {
    }

    /**
     * Program entry point.
     *
     * @param args the program command line arguments
     *
     * @throws Exception if there is a program error
     */
    public static void main(String[] args) throws Exception {
        CacheFactory.ensureCluster();

        LogManager.getLogManager().readConfiguration(
                Main.class.getResourceAsStream("/helidon-logging.properties"));

        ServerConfiguration configuration = ServerConfiguration.builder()
                                                               .port(8080)
                                                               .build();

        Routing routing = Routing.builder()
                                 .register(JsonSupport.create())
                                 .put("/query", Main::query)
                                 .put("/insert", Main::insert)
                                 .put("/run", Main::run)
                                 .build();

        WebServer.create(configuration, routing)
                 .start()
                 .thenAccept(s->{
                     System.out.println("HTTP server is UP! http://localhost:" + s.port());
                     DefaultCacheServer.startServerDaemon().waitForServiceStart();
                     s.whenShutdown().thenRun(()->System.out.println("HTTP server is DOWN. Good bye!"));
                 })
                 .exceptionally(t->{
                     System.err.println("Startup failed: " + t.getMessage());
                     t.printStackTrace(System.err);
                     return null;
                 });
    }

    /**
     * Query endpoint for Helidon.
     *
     * @param request  {@link ServerRequest} request
     * @param response {@link ServerResponse} respons
     */
    private static void query(ServerRequest request, ServerResponse response) {
        request.content().as(JsonObject.class)
               .thenAccept(json->executeQuery(json, response));
    }

    /**
     * Insert MAX customers.
     */
    private static void insert(ServerRequest request, ServerResponse response) {
        NamedCache<Integer, Person> people = CacheFactory.getCache("people");
        people.clear();
        Map<Integer, Person> buffer = new HashMap<>();

        for (int i = 1; i <= MAX; i++) {
            buffer.put(i, new Person(i, "Name " + i, "Address " + i));
            if (i % 1000 == 0) {
                people.putAll(buffer);
                buffer.clear();
            }
        }
        if (!buffer.isEmpty()) {
            people.putAll(buffer);
        }

        ServerResponse serverResponse = response.status(Http.Status.OK_200.code());
        serverResponse.send("Number of people in cache is " + people.size());
    }

    /**
     * The the query.
     *
     * @param request
     * @param response
     */
    private static void run(ServerRequest request, ServerResponse response) {
        NamedCache<Integer, Person> people = CacheFactory.getCache("people");
        StringBuilder sb = new StringBuilder();

        MetricRegistry metrics = new MetricRegistry();

        // Metrics reporter
        ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                                                  .convertRatesTo(TimeUnit.SECONDS)
                                                  .convertDurationsTo(TimeUnit.MILLISECONDS)
                                                  .build();
        reporter.start(15, TimeUnit.SECONDS);
        
        Timer invoke = metrics.timer("invoke");
        
        // measure invocation time
        for (int iter = 0; iter < ITERS; iter++) {
            for (int i = 1; i <= MAX; i++) {
                if (iter > 0) {
                    Timer.Context time = invoke.time();
                    people.invoke(i, new MyEntryProcessor("New Address"));
                    time.stop();
                } else {
                    // give 1 iteration as warmup
                    people.invoke(i, new MyEntryProcessor("New Address"));
                }
            }
        }
//
//        // measure put time
//        for (int iter = 0; iter < ITERS; iter++) {
//            for (int i = 1; i <= MAX; i++) {
//                Person p = new Person(i, "Name " + i, "Address " + i);
//                if (iter > 0) {
//                    Timer.Context time = put.time();
//                    people.putAll(Collections.singletonMap(p.getId(), p));
//                    time.stop();
//                }
//                else {
//                    // give 1 iteration as warmup
//                    people.putAll(Collections.singletonMap(p.getId(), p));
//                }
//            }
//        }
//
       System.out.println("**** FINAL REPORT ******");

        reporter.report();
        reporter.stop();

        ServerResponse serverResponse = response.status(Http.Status.OK_200.code());
        serverResponse.send(sb.toString());

    }

    /**
     * Execute a CohQL query and return the response.
     *
     * @param json     CohQL query JSON
     * @param response {@link ServerResponse} response
     */
    @SuppressWarnings("unchecked")
    private static void executeQuery(JsonObject json, ServerResponse response) {
        if (json == null) {
            response.status(Http.ResponseStatus.create(Http.Status.BAD_REQUEST_400.code(),
                    "missing json request body")).send();
            return;
        }

        String sQuery = json.getString("query");
        if (sQuery == null || sQuery.isEmpty()) {
            response.status(Http.ResponseStatus.create(Http.Status.BAD_REQUEST_400.code(),
                    "missing query field")).send();
            return;
        }

        try {
            Object        oResult    = QueryHelper.executeStatement(sQuery);
            JsonStructure jsonResult = null;

            if (oResult != null) {
                jsonResult = JSON.createObjectBuilder()
                                 .add("result", String.valueOf(oResult))
                                 .build();
            }

            ServerResponse serverResponse = response.status(Http.Status.OK_200.code());

            if (jsonResult == null) {
                serverResponse.send();
            }
            else {
                serverResponse.send(jsonResult);
            }
        }
        catch (Throwable e) {
            e.printStackTrace();
            response.status(Http.ResponseStatus.create(Http.Status.INTERNAL_SERVER_ERROR_500.code(),
                    e.getMessage())).send();
        }
    }

    // ----- data members ---------------------------------------------------

    /**
     * Factory for JSON.
     */
    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());

    private static final int MAX = 10000;
    private static final int ITERS = 11;
}
