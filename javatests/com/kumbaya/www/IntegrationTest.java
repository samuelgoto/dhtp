package com.kumbaya.www;

import com.google.common.base.Optional;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.multibindings.MapBinder;
import com.kumbaya.common.Server;
import com.kumbaya.router.Router;
import com.kumbaya.www.WorldWideWeb;
import com.kumbaya.www.gateway.Gateway;
import com.kumbaya.www.proxy.JettyServer;
import com.kumbaya.www.proxy.Proxy;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import junit.framework.TestCase;

public class IntegrationTest extends TestCase {
  private final Supplier<Proxy> proxy = new Supplier<Proxy>() {
    @Override
    Proxy build() {
      return Guice.createInjector(
          new Proxy.Module(new InetSocketAddress("localhost", 9090)) 
          ).getInstance(Proxy.class);
    }
  };
  
  private final Supplier<Router> router = new Supplier<Router>() {
    @Override
    Router build() {
      return Guice.createInjector(
          new Router.Module(new InetSocketAddress("localhost", 7070)))
      .getInstance(Router.class);    
    }
  };
  
  private final Supplier<Gateway> gateway = new Supplier<Gateway>() {
    @Override
    Gateway build() {
      return Guice.createInjector(new AbstractModule() {
        @Override
        protected void configure() {
          bind(ExecutorService.class).toInstance(Executors.newFixedThreadPool(1));
        }
      }).getInstance(Gateway.class);    
    }
  };
  
  private final Supplier<Server> www = new Supplier<Server>() {
    @Override
    Server build() {
      return server();
    }
  };

  @Override
  public void setUp() throws IOException {
    proxy.clear().get().bind(new InetSocketAddress("127.0.0.1", 8080));
    router.clear().get().bind(new InetSocketAddress("127.0.0.1", 9090));
    gateway.clear().get().bind(new InetSocketAddress("127.0.0.1", 7070));
    www.clear().get().bind(new InetSocketAddress("127.0.0.1", 6060));
  }
  
  @Override
  public void tearDown() throws IOException {
    proxy.get().close();
    router.get().close();
    gateway.get().close();
    www.get().close();
  }  
  
  public void test200_direclty() throws IOException {
    Optional<String> result = WorldWideWeb.get("http://localhost:6060/helloworld");
    assertTrue(result.isPresent());
    assertEquals("hello world", result.get());
  }
 
  public void test404s_directly() throws IOException {
    // Straight to the server
    Optional<String> result = WorldWideWeb.get("http://localhost:6060/doesntexist");
    assertFalse(result.isPresent());
  }

  public void test200_throughNetwork() throws IOException {
    Optional<String> result = WorldWideWeb.get(
        new InetSocketAddress("localhost", 8080), 
        "http://localhost:6060/helloworld");
    assertTrue(result.isPresent());
    assertEquals("hello world", result.get());
  }
  
  public void test404s_throughNetwork() throws Exception {
    // Through the network
    Optional<String> proxied = WorldWideWeb.get(
        new InetSocketAddress("localhost", 8080), 
        "http://localhost:6060/doesntexist");
    assertFalse(proxied.isPresent());
  }

  public void test404s_throughNetworkDnsDoesntExist() throws Exception {
    // Through the network
    Optional<String> proxied = WorldWideWeb.get(
        new InetSocketAddress("localhost", 8080),
        "http://localhost:9999/doesntexist");
    assertFalse(proxied.isPresent());
  }
  
  public void test500s_throughNetwork() throws Exception {
    // Through the network
    Optional<String> proxied = WorldWideWeb.get(
        new InetSocketAddress("localhost", 8080), 
        "http://localhost:6060/error");
    assertFalse(proxied.isPresent());
  }
  
  // TODO(goto): tests for loops in the network! Just ran into one right now.

  public void test200_throughNetwork_notProxied() throws IOException {
    // This test requires you to add the following line to your /etc/host file.
    try {
      Optional<String> result = WorldWideWeb.get("http://localhost-6060.example.com:8080/helloworld");
      assertTrue(result.isPresent());
      assertEquals("hello world", result.get());
    } catch (UnknownHostException e) {
      e.printStackTrace();
      System.out.println("===================================================================");
      System.out.println("DNS set up failure: ignoring this test.");
      System.out.println("This test requires you to add the following line to /etc/host");
      System.out.println("127.0.0.1     localhost.6060.example.com");
      System.out.println("===================================================================");
    }
  }
  
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

  Server server() {
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
  
  static abstract class Supplier<T> {
    private Optional<T> instance = Optional.absent();
    
    T get() {
      if (instance.isPresent()) {
        return instance.get();
      } else {
        instance = Optional.of(build());
        return instance.get();
      }      
    }
    
    Supplier<T> clear() {
      this.instance = Optional.absent();
      return this;
    }
    
    abstract T build();
  }
}

