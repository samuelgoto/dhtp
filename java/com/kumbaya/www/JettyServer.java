package com.kumbaya.www;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kumbaya.common.Flags.Flag;
import com.kumbaya.common.Server;
import com.kumbaya.common.monitor.VarZLogger;
import java.io.IOException;
import java.util.Map;
import javax.servlet.Servlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

@Singleton
public class JettyServer implements Server {
  private static final Log logger = LogFactory.getLog(JettyServer.class);
  private org.eclipse.jetty.server.Server server;
  private final Map<String, Servlet> servlets;

  @Inject
  @Flag("port")
  private int port;

  @Inject
  JettyServer(Map<String, Servlet> servlets, VarZLogger varZ) {
    this.servlets = servlets;
  }

  @Override
  public void start() throws IOException {
    server = new org.eclipse.jetty.server.Server(port);

    ServletContextHandler servlet = new ServletContextHandler(ServletContextHandler.SESSIONS);
    servlet.setContextPath("/");

    for (Map.Entry<String, Servlet> entry : servlets.entrySet()) {
      servlet.addServlet(new ServletHolder(entry.getValue()), entry.getKey());
    }

    server.setHandler(servlet);

    try {
      server.start();
      while (!server.isRunning()) {
        Thread.sleep(100);
      }
    } catch (InterruptedException e) {
      throw new IOException(e);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void stop() {
    try {
      server.stop();
      server.destroy();
    } catch (Exception e) {
      throw new RuntimeException("Error stopping http server", e);
    }
  }
}
