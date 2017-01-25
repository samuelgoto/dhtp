package com.kumbaya.common.testing;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.kumbaya.common.Flags;
import com.kumbaya.common.Server;
import com.kumbaya.router.Router;
import com.kumbaya.www.WorldWideWeb;
import com.kumbaya.www.gateway.Gateway;
import com.kumbaya.www.proxy.Proxy;
import java.io.IOException;
import java.util.Map;
import javax.servlet.Servlet;

public class LocalNetwork {
  private final Map<String, Class<? extends Servlet>> servlets;

  LocalNetwork(Map<String, Class<? extends Servlet>> servlets) {
    this.servlets = servlets;
  }

  private final Supplier<Proxy> proxy = new Supplier<Proxy>() {
    @Override
    public Proxy build() {
      return Guice.createInjector(
          Flags.asModule(new String[] {"--port=8080", "--entrypoint=localhost:8081"}),
          new Proxy.Module()).getInstance(Proxy.class);
    }
  };

  private final Supplier<Router> router = new Supplier<Router>() {
    @Override
    public Router build() {
      return Guice.createInjector(
          Flags.asModule(
              new String[] {"--port=8081", "--forwarding=localhost:8082", "--host=localhost"}),
          new Router.Module()).getInstance(Router.class);
    }
  };

  private final Supplier<Gateway> gateway = new Supplier<Gateway>() {
    @Override
    public Gateway build() {
      return Guice
          .createInjector(Flags.asModule(new String[] {"--port=8082"}), new Gateway.Module())
          .getInstance(Gateway.class);
    }
  };

  private final Supplier<Server> www = new Supplier<Server>() {
    @Override
    public Server build() {
      return WorldWideWebServer.server(8083, servlets);
    }
  };

  public void setUp() throws IOException {
    WorldWideWeb.setTimeout(WorldWideWeb.DEFAULT_CONNECTION_TIMEOUT_MS);
    proxy.clear().get().start();
    router.clear().get().start();
    gateway.clear().get().start();
    www.clear().get().start();
  }

  public void tearDown() throws IOException {
    proxy.get().stop();
    router.get().stop();
    gateway.get().stop();
    www.get().stop();
  }

  public static Supplier<LocalNetwork> supplier(
      final Map<String, Class<? extends Servlet>> servlets) {
    return new Supplier<LocalNetwork>() {
      @Override
      public LocalNetwork build() {
        return new LocalNetwork(servlets);
      }
    };
  }

  public static void main(String args[]) throws Exception {
    LocalNetwork network = LocalNetwork.supplier(ImmutableMap.of()).get();
    network.setUp();
    System.out.println("Quit?");
    System.in.read();
    network.tearDown();
    System.out.println("Done.");
  }
}
