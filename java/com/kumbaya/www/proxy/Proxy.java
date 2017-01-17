package com.kumbaya.www.proxy;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.multibindings.MapBinder;
import com.kumbaya.common.Flags;
import com.kumbaya.common.InetSocketAddresses;
import com.kumbaya.common.Server;
import com.kumbaya.common.Flags.Flag;
import com.kumbaya.common.monitor.MonitoringModule;
import com.kumbaya.router.Client;
import com.kumbaya.router.Packets;
import com.kumbaya.router.Packets.Data;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Set;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.servlets.ProxyServlet;

public class Proxy implements Server {
  private static final Log logger = LogFactory.getLog(Proxy.class);

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

  @Override
  public void bind(InetSocketAddress address) throws IOException {
    http.bind(address);
  }

  @Override
  public void close() {
    http.close();
  }
  
  static class MyProxyServlet extends ProxyServlet.Transparent {
    private static final Log logger = LogFactory.getLog(MyProxyServlet.class);

    private final Client client;

    @Inject
    MyProxyServlet(Client client) {
      this.client = client;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
    }
    
    static String assemble(String url) throws MalformedURLException {
      URL result = new URL(url);
      String[] parts = result.getHost().split("\\.");
      if (parts.length > 2 &&
          parts[parts.length - 2].equals("kumbaya") &&
          parts[parts.length - 1].equals("io")
          ) {
        
        String[] domain = parts[parts.length - 3].split("-");
        
        if (domain.length > 1) {
          // There is a port available.
          // Override last element with just the domain
          parts[parts.length - 3] = domain[0];
        }
        
        String host = String.join(".", Arrays.copyOfRange(parts, 0, parts.length - 2));

        if (domain.length > 1) {
          host += ":" + domain[1];
        }
        
        // Removes the old port and old host.
        return  url.replace(":" + result.getPort(), "").replace(result.getHost(), host);
      }
      return url;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
      // The proxy will get two types of requests:
      // - through HTTP proxying, which sets the entire request with an absolute url.
      // - through DNS proxying, which requires the proxy to dissamble the url and reconstruct it.
      
      Packets.Interest interest = new Packets.Interest();
      HttpServletRequest request = (HttpServletRequest) req;
      HttpServletResponse response = (HttpServletResponse) res;
      
      String url = assemble(request.getRequestURL().toString());

      interest.getName().setName(url);
      try {
    	logger.info("Got a request: " + url);
        Optional<Data> result = client.send(interest);
        logger.info("Got a response");

        if (result.isPresent()) {
          response.getOutputStream().write(result.get().getContent());
        } else {
          response.sendError(404);
        }
      } catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
        logger.error(e);
        response.sendError(500);
      }
    }
  }  
  

  public static void main(String[] args) throws Exception {
    logger.info("Running the Kumbaya Proxy");
    
    Set<Flag<?>> options = ImmutableSet.of(
        Flag.of("host", "The external hostname", true, "localhost"),
        Flag.of("port", "The external hostname", true, 8083),
        Flag.of("entrypoint", "The external ip/port of the network entrypoint", true, "localhost:8082")
        );

    Flags flags = Flags.parse(options, args);
    final String host = flags.get("host");
    final int port = flags.get("port");
    final String entrypoint = flags.get("entrypoint");
    
    Proxy proxy = Guice.createInjector(
        new Module(InetSocketAddresses.parse(entrypoint)))
        .getInstance(Proxy.class);
    proxy.bind(new InetSocketAddress(host, port));
  }
}
