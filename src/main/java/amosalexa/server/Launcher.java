package amosalexa.server; /**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

import amosalexa.AmosAlexaSpeechlet;
import com.amazon.speech.speechlet.SpeechletV2;
import com.amazon.speech.speechlet.servlet.SpeechletServlet;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Shared launcher for executing all sample skills within a single servlet container.
 */
public final class Launcher {
    private static final Logger log = LoggerFactory.getLogger(Launcher.class);
    /**
     * port number for the jetty server.
     */
    private static final int PORT = 8888;

    /**
     * Security scheme to use.
     */
    private static final String HTTPS_SCHEME = "https";

    /**
     *
     */
    public static Server server;

    /**
     * default constructor.
     */
    private Launcher() {
    }


    /**
     * Main entry point. Starts a Jetty server.
     *
     * @param args ignored.
     * @throws Exception if anything goes wrong.
     */
    public static void main(final String[] args) throws Exception {
        PropertyConfigurator.configure(Thread.currentThread().getContextClassLoader().getResource("log4j.properties"));

        server = newServer();

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        server.setHandler(context);

        context.addServlet(new ServletHolder(createServlet(AmosAlexaSpeechlet.getInstance())), "/amosalexa");

        server.start();
        server.join();
    }

    private static SpeechletServlet createServlet(final SpeechletV2 speechlet) {
        SpeechletServlet servlet = new SpeechletServlet();

        servlet.setSpeechlet(speechlet);
        return servlet;
    }



    private static Server newServer() {
        return new Server(PORT);
    }
}