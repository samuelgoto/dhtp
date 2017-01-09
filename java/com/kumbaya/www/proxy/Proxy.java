package com.kumbaya.www.proxy;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.multibindings.MapBinder;
import com.kumbaya.common.Server;
import com.kumbaya.monitor.MonitoringModule;
import java.io.IOException;
import java.net.InetSocketAddress;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

class Proxy {
  static class WebServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
      response.getWriter().println("hello world");
    }
  }

  public static void main(String[] args) throws Exception {
    Injector injector = Guice.createInjector(
        new ServletModule(), 
        new MonitoringModule(),
        new AbstractModule() {
          @Override
          protected void configure() {
            MapBinder<String, HttpServlet> mapbinder
            = MapBinder.newMapBinder(binder(), String.class, HttpServlet.class);

            mapbinder.addBinding("/*").to(WebServlet.class);
          }
        });

    Server server = injector.getInstance(Server.class);
    server.bind(new InetSocketAddress("localhost", 8080));
  }
}
