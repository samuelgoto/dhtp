package com.kumbaya.www.proxy;

import com.google.common.base.Optional;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.multibindings.MapBinder;
import com.kumbaya.common.Server;
import com.kumbaya.common.monitor.MonitoringModule;
import com.kumbaya.router.Client;
import com.kumbaya.router.Packets;
import com.kumbaya.router.Packets.Data;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.servlets.ProxyServlet;

public class Proxy implements Server {
  private final JettyServer http;

  @Inject
  Proxy(JettyServer http) {
    this.http = http;
  }  

  public static class Module extends AbstractModule {
    private final InetSocketAddress entrypoint;
    public Module(InetSocketAddress entrypoint) {
      this.entrypoint = entrypoint;
    }

    @Override
    protected void configure() {
      install(new ServletModule()); 
      install(new MonitoringModule());

      install(new AbstractModule() {
        @Override
        protected void configure() {
          MapBinder<String, Servlet> mapbinder
          = MapBinder.newMapBinder(binder(), String.class, Servlet.class);
          mapbinder.addBinding("/*").to(MyProxyServlet.class);
        }
      });

      bind(InetSocketAddress.class).toInstance(entrypoint);
    }
  }

  public static void main(String[] args) throws Exception {
    Injector injector = Guice.createInjector(
        new Module(new InetSocketAddress("localhost", 8081)));

    Server server = injector.getInstance(Proxy.class);
    server.bind(new InetSocketAddress("localhost", 8080));
  }

  @Override
  public void bind(InetSocketAddress address) throws IOException {
    http.bind(address);
  }

  @Override
  public void close() {
    http.close();
  }

  private static class MyProxyServlet extends ProxyServlet.Transparent {
    private final InetSocketAddress entrypoint;

    @Inject
    MyProxyServlet(InetSocketAddress entrypoint) {
      this.entrypoint = entrypoint;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
      Client client = new Client(entrypoint);
      Packets.Interest interest = new Packets.Interest();
      HttpServletRequest request = (HttpServletRequest) req;
      HttpServletResponse response = (HttpServletResponse) res;
      interest.getName().setName(request.getRequestURL().toString());
      try {
        Optional<Data> result = client.send(interest);
        if (result.isPresent()) {
          response.getOutputStream().write(result.get().getContent());
        } else {
          response.sendError(404);
        }
      } catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
        e.printStackTrace();        
        response.sendError(500);
      }
    }
  }
}
