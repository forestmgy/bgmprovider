package org.openeuler.gm.com.sun.net.httpserver;/*
 * Copyright (c) 2005, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * @test
 * @bug 6270015
 * @run main/othervm Test8a
 * @summary  Light weight HTTP server
 */

import com.sun.net.httpserver.*;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.Provider;
import java.security.Security;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Test POST large file via fixed len encoding
 */

public class Test8a extends Test {

    @org.junit.Test
    public void test () throws Exception {
        //Logger log = Logger.getLogger ("com.sun.net.httpserver");
        //ConsoleHandler h = new ConsoleHandler();
        //h.setLevel (Level.INFO);
        //log.addHandler (h);
        //log.setLevel (Level.INFO);
        Security.insertProviderAt((Provider)Class.forName("org.openeuler.BGMJCEProvider").newInstance(), 1);
        Security.insertProviderAt((Provider)Class.forName("org.openeuler.BGMJSSEProvider").newInstance(), 2);
        HttpsServer server = null;
        ExecutorService executor = null;
        try {
            Handler handler = new Handler();
            InetSocketAddress addr = new InetSocketAddress (0);
            server = HttpsServer.create (addr, 0);
            HttpContext ctx = server.createContext ("/test", handler);
            executor = Executors.newCachedThreadPool();
            SSLContext ssl = new SimpleSSLContext(Test8a.class.getClassLoader().getResource("").getPath()).get();
            server.setHttpsConfigurator(new HttpsConfigurator (ssl));
            server.setExecutor (executor);
            server.start ();

            URL url = new URL ("https://localhost:"+server.getAddress().getPort()+"/test/foo.html");
            System.out.print ("Test8a: " );
            HttpsURLConnection urlc = (HttpsURLConnection)url.openConnection ();
            urlc.setDoOutput (true);
            urlc.setRequestMethod ("POST");
            urlc.setHostnameVerifier (new DummyVerifier());
            urlc.setSSLSocketFactory (ssl.getSocketFactory());
            OutputStream os = new BufferedOutputStream (urlc.getOutputStream(), 8000);
            for (int i=0; i<SIZE; i++) {
                os.write (i % 250);
            }
            os.close();
            int resp = urlc.getResponseCode();
            if (resp != 200) {
                throw new RuntimeException ("test failed response code");
            }
            InputStream is = urlc.getInputStream ();
            for (int i=0; i<SIZE; i++) {
                int f = is.read();
                if (f != (i % 250)) {
                    System.out.println ("Setting error(" +f +")("+i+")" );
                    error = true;
                    break;
                }
            }
            is.close();
        } finally {
            delay();
            if (server != null) server.stop(2);
            if (executor != null) executor.shutdown();
        }
        if (error) {
            throw new RuntimeException ("test failed error");
        }
        System.out.println ("OK");

    }

    public static boolean error = false;
    //final static int SIZE = 999999;
    final static int SIZE = 9999;

    static class Handler implements HttpHandler {
        int invocation = 1;
        public void handle (HttpExchange t)
            throws IOException
        {
        System.out.println ("Handler.handle");
            InputStream is = t.getRequestBody();
            Headers map = t.getRequestHeaders();
            Headers rmap = t.getResponseHeaders();
            int c, count=0;
            while ((c=is.read ()) != -1) {
                if (c != (count % 250)) {
                System.out.println ("Setting error 1");
                    error = true;
                    break;
                }
                count ++;
            }
            if (count != SIZE) {
                System.out.println ("Setting error 2");
                error = true;
            }
            is.close();
            t.sendResponseHeaders (200, SIZE);
                System.out.println ("Sending 200 OK");
            OutputStream os = new BufferedOutputStream(t.getResponseBody(), 8000);
            for (int i=0; i<SIZE; i++) {
                os.write (i % 250);
            }
            os.close();
                System.out.println ("Finished");
        }
    }
}
