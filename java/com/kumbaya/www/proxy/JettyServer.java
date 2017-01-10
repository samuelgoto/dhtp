package com.kumbaya.www.proxy;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.kumbaya.common.Server;
import com.kumbaya.common.monitor.VarZLogger;
import com.kumbaya.www.IntegrationTest;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

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
  JettyServer(
      Map<String, Servlet> servlets,
      VarZLogger varZ) {
    this.servlets = servlets;
  }

  @Override
  public void bind(InetSocketAddress address) throws IOException {
    server = new org.eclipse.jetty.server.Server(address.getPort());

    ServletContextHandler servlet = new ServletContextHandler(ServletContextHandler.SESSIONS);
    servlet.setContextPath("/");

    for (Map.Entry<String, Servlet> entry : servlets.entrySet()) {
      servlet.addServlet(new ServletHolder(entry.getValue()), entry.getKey());
    }
    
    server.setHandler(servlet);

    try {
      server.start();
    } catch (InterruptedException e) {
      throw new IOException(e);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void close() {
    try {
      server.stop();
      server.destroy();
    } catch (Exception e) {
      throw new RuntimeException("Error stopping http server", e);
    }
  }
}
