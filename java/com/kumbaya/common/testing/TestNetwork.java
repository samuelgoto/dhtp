package com.kumbaya.common.testing;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

import javax.servlet.Servlet;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.kumbaya.common.Flags;
import com.kumbaya.common.Server;
import com.kumbaya.router.Router;
import com.kumbaya.www.gateway.Gateway;
import com.kumbaya.www.proxy.Proxy;
import com.kumbaya.www.testing.WorldWideWebServer;

public class TestNetwork {
  private final Map<String, Class<? extends Servlet>> servlets;
  
  TestNetwork(Map<String, Class<? extends Servlet>> servlets) {
    this.servlets = servlets;
  }
    
  private final Supplier<Proxy> proxy = new Supplier<Proxy>() {
    @Override
    public Proxy build() {
      return Guice.createInjector(
          Flags.asModule(new String[] {"--entrypoint=localhost:9090"}),
          new Proxy.Module() 
          ).getInstance(Proxy.class);
    }
  };
  
  private final Supplier<Router> router = new Supplier<Router>() {
    @Override
    public Router build() {
      return Guice.createInjector(
          Flags.asModule(new String[] {"--forwarding=localhost:7070"}),
          new Router.Module())
      .getInstance(Router.class);    
    }
  };
  
  private final Supplier<Gateway> gateway = new Supplier<Gateway>() {
    @Override
    public Gateway build() {
      return Guice.createInjector(new Gateway.Module()).getInstance(Gateway.class);    
    }
  };
  
  private final Supplier<Server> www = new Supplier<Server>() {
    @Override
    public Server build() {
      return WorldWideWebServer.server(servlets);
    }
  };
  
  public void setUp() throws IOException {
    proxy.clear().get().bind(new InetSocketAddress("127.0.0.1", 8080));
    router.clear().get().bind(new InetSocketAddress("127.0.0.1", 9090));
    gateway.clear().get().bind(new InetSocketAddress("127.0.0.1", 7070));
    www.clear().get().bind(new InetSocketAddress("127.0.0.1", 6060));
  }
  
  public void tearDown() throws IOException {
    proxy.get().close();
    router.get().close();
    gateway.get().close();
    www.get().close();
  }
  
  public static Supplier<TestNetwork> supplier(final Map<String, Class<? extends Servlet>> servlets) {
    return new Supplier<TestNetwork>() {
      @Override
      public TestNetwork build() {
        return new TestNetwork(servlets);
      }
    };
  }
}
