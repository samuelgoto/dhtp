package com.kumbaya.www.proxy;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

class ServletModule extends AbstractModule {

  static class WebServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
      response.getWriter().println("hello world");
    }
  }

  @Override
  protected void configure() {
    MapBinder<String, HttpServlet> mapbinder
    = MapBinder.newMapBinder(binder(), String.class, HttpServlet.class);

    mapbinder.addBinding("/*").to(WebServlet.class);
  }
}
