/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.httpserver.priorities;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.threadpool.GrizzlyExecutorService;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

/**
 * Example of HTTP server, which assigns different priorities (thread-pools)
 * to registered {@link HttpHandler}s.
 * 
 * Suppose we have two types of web applications ({@link HttpHandler}s)
 * registered on {@link NetworkListener}. One of them is high-priority
 * application we want to get response from as fast as possible (say some
 * core service we use to check if server is running). Other application runs
 * a long-lasting task, which may occupy a worker thread for significant
 * amount of time, so, considering the thread-pool is limited, may impact other
 * applications availability.
 * 
 * How we can separate these two applications?
 * There are two options:
 * 1) we can use HttpHandler suspend/resume mechanism
 * 2) lower level by-Request prioritization mechanism, which allows us to dispatch
 *      a Request processing to a proper thread-pool before passing control to
 *      {@link HttpHandler#service(org.glassfish.grizzly.http.server.Request, org.glassfish.grizzly.http.server.Response)} method.
 * 
 * In this example we take the the 2nd approach...
 * 
 * @author Alexey Stashok
 */
public class Server {
    private static final Logger LOGGER = Grizzly.logger(Server.class);
    
    public static void main(String[] args) {
        // Prepare the low priority thread pool configuration
        final ThreadPoolConfig lowPriorityThreadPoolConfig =
                ThreadPoolConfig.defaultConfig().copy()
                .setPoolName("low-priority-thread-pool")
                .setCorePoolSize(2)
                .setMaxPoolSize(16)
                .setMemoryManager(MemoryManager.DEFAULT_MEMORY_MANAGER)
                .setPriority(Thread.MIN_PRIORITY);
        
        // Prepare the high priority thread pool configuration
        final ThreadPoolConfig highPriorityThreadPoolConfig =
                ThreadPoolConfig.defaultConfig().copy()
                .setPoolName("high-priority-thread-pool")
                .setCorePoolSize(2)
                .setMaxPoolSize(16)
                .setMemoryManager(MemoryManager.DEFAULT_MEMORY_MANAGER)
                .setPriority(Thread.MAX_PRIORITY);

        // Initialize the low priority thread pool
        final ExecutorService lowPriorityExecutorService =
                GrizzlyExecutorService.createInstance(lowPriorityThreadPoolConfig);
        
        // Initialize the high priority thread pool
        final ExecutorService highPriorityExecutorService =
                GrizzlyExecutorService.createInstance(highPriorityThreadPoolConfig);

        final HttpServer server = new HttpServer();
        final ServerConfiguration config = server.getServerConfiguration();

        // Register the LowPriorityHandler and passing low priority thread pool
        // to be used.
        config.addHttpHandler(
                new LowPriorityHandler(lowPriorityExecutorService), "/doNormal");

        // Register the HighPriorityHandler and passing high priority thread pool
        // to be used.
        config.addHttpHandler(
                new HighPriorityHandler(highPriorityExecutorService), "/doService");
        
        // create a network listener that listens on port 8080.
        final NetworkListener networkListener = new NetworkListener(
                "prioritization-test",
                NetworkListener.DEFAULT_NETWORK_HOST,
                NetworkListener.DEFAULT_NETWORK_PORT);
        
        // important!!! Once we're not planning to use Grizzly embedded/default
        // thread pool - we have to null it.
        networkListener.getTransport().setWorkerThreadPoolConfig(null);
        
        server.addListener(networkListener);
        try {
            // Start the server
            server.start();
            System.out.println("The server is running. Press enter to stop...");
            System.in.read();
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, ioe.toString(), ioe);
        } finally {
            server.shutdownNow();
            // !!! Don't forget to shutdown the custom threads
            lowPriorityExecutorService.shutdownNow();
            highPriorityExecutorService.shutdownNow();
        }
    }
}
