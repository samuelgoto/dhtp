package com.kumbaya.www.testing;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.multibindings.MapBinder;
import com.kumbaya.common.Server;
import com.kumbaya.www.proxy.JettyServer;

import java.io.IOException;
import java.util.Map;

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
      response.getWriter().print("hello world");
    }
  }

  private static class ServerErrorServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
      response.sendError(500);
    }
  }
  
  public static Server server(Map<String, Class<? extends Servlet>> servlets) {
	    return Guice.createInjector(new AbstractModule() {
	        @Override
	        protected void configure() {
	          MapBinder<String, Servlet> mapbinder
	          = MapBinder.newMapBinder(binder(), String.class, Servlet.class);

	          for (Map.Entry<String, Class<? extends Servlet>> servlet : servlets.entrySet()) {
	        	  mapbinder.addBinding(servlet.getKey()).to(servlet.getValue());
	          }
	          
	        }
	      }).getInstance(JettyServer.class);
  }
  
  public static Server server(String path, Class<? extends Servlet> servlet) {
	  return server(ImmutableMap.<String, Class<? extends Servlet>>builder()
			  .put(path, servlet)
			  .build()); 
  }

  public static Server server() {
	  // Installs some default servlets.
	  return server(ImmutableMap.<String, Class<? extends Servlet>>builder()
			  .put("/helloworld", HelloWorldServlet.class)
			  .put("/error", ServerErrorServlet.class)
			  .build());
  }
  
  
}
