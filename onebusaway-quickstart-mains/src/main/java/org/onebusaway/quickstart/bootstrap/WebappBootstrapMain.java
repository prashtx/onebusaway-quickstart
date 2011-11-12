/**
 * Copyright (C) 2011 Brian Ferris (bdferris@google.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.quickstart.bootstrap;

import java.net.URL;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Parser;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import org.onebusaway.quickstart.BootstrapCommon;
import org.onebusaway.quickstart.WebappCommon;

/**
 * Boostrap a Jetty webapp container for running an instance of OneBusAway.
 * 
 * @author bdferris
 * 
 */
public class WebappBootstrapMain {

  private static final String ARG_PORT = "port";

  public static void run(URL warUrl, boolean consoleMode, String[] args)
      throws Exception {

    if (args.length == 0 || isHelp(args)) {
      BootstrapCommon.printUsage(WebappBootstrapMain.class, "usage-webapp.txt");
      System.exit(-1);
    }

    Options options = createOptions();
    Parser parser = new GnuParser();

    CommandLine cli = parser.parse(options, args);
    args = cli.getArgs();

    if (args.length != 1) {
      BootstrapCommon.printUsage(WebappBootstrapMain.class, "usage-webapp.txt");
      System.exit(-1);
    }

    String bundlePath = args[0];
    System.setProperty("bundlePath", bundlePath);

    int port = 8080;
    if (cli.hasOption(ARG_PORT))
      port = Integer.parseInt(cli.getOptionValue(ARG_PORT));

    Server server = new Server();
    SocketConnector connector = new SocketConnector();

    // Set some timeout options to make debugging easier.
    connector.setMaxIdleTime(1000 * 60 * 60);
    connector.setSoLingerTime(-1);
    connector.setPort(port);
    server.setConnectors(new Connector[] {connector});

    WebAppContext context = new WebAppContext();
    context.setServer(server);
    context.setContextPath("/");
    context.setWelcomeFiles(new String[] {"index.action"});
    context.setWar(warUrl.toExternalForm());
    // We store the command line object as a webapp context attribute so it can
    // be used by the context loader to inject additional beans as needed
    context.setAttribute(WebappCommon.COMMAND_LINE_CONTEXT_ATTRIBUTE, cli);
    server.setHandler(context);

    if (cli.hasOption(WebappCommon.ARG_BUILD)) {
      System.setProperty("hibernate.hbm2ddl.auto", "update");
      System.setProperty("bundleCacheDir", bundlePath + "/cache");
      System.setProperty("gtfsPath",
          cli.getOptionValue(WebappCommon.ARG_GTFS_PATH));
      context.addOverrideDescriptor("/WEB-INF/builder-override-web.xml");
    }

    try {
      server.start();

      System.err.println("=============================================================");
      System.err.println("=");
      System.err.println("= Your OneBusAway instance has started.  Browse to:");
      System.err.println("=");
      System.err.println("= http://localhost:8080/");
      System.err.println("=");
      System.err.println("= to see your instance in action.");
      if (consoleMode) {
        System.err.println("=");
        System.err.println("= When you are finished, press return to exit gracefully...");
      }
      System.err.println("=============================================================");

      if (consoleMode) {
        System.in.read();
        server.stop();
        server.join();
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(100);
    }
  }

  private static boolean isHelp(String[] options) {
    for (String option : options) {
      option = option.replaceAll("-", "");
      option = option.toLowerCase();
      if (option.equals("help") || option.equals("h") || option.equals("?"))
        return true;
    }
    return false;
  }

  private static Options createOptions() {
    Options options = new Options();
    options.addOption(ARG_PORT, true, "port (default=8080)");
    options.addOption(WebappCommon.ARG_BUILD, false, "");
    options.addOption(WebappCommon.ARG_GTFS_PATH, true, "");
    options.addOption(WebappCommon.ARG_GTFS_REALTIME_TRIP_UPDATES_URL, true, "");
    options.addOption(WebappCommon.ARG_GTFS_REALTIME_VEHICLE_POSITIONS_URL,
        true, "");
    options.addOption(WebappCommon.ARG_GTFS_REALTIME_ALERTS_URL, true, "");
    options.addOption(WebappCommon.ARG_GTFS_REALTIME_REFRESH_INTERVAL, true, "");
    return options;
  }
}
