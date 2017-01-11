package com.kumbaya.www.testing;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.multibindings.MapBinder;
import com.kumbaya.common.Server;
import com.kumbaya.www.proxy.JettyServer;
import java.io.IOException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WorldWideWebServer {
  private static class HelloWorldServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
      response.getWriter().println("hello world");
    }
  }

  private static class ServerErrorServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
      response.sendError(500);
    }
  }

  public static Server server() {
    return Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        MapBinder<String, Servlet> mapbinder
        = MapBinder.newMapBinder(binder(), String.class, Servlet.class);

        mapbinder.addBinding("/helloworld").to(HelloWorldServlet.class);
        mapbinder.addBinding("/error").to(ServerErrorServlet.class);
      }
    }).getInstance(JettyServer.class);
  }
}
