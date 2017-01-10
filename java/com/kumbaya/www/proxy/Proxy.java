package com.kumbaya.www.proxy;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.kumbaya.common.Server;
import com.kumbaya.common.monitor.MonitoringModule;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class Proxy implements Server {
  private final JettyServer http;
  
  @Inject
  Proxy(JettyServer http) {
    this.http = http;
  }  

  public static class Module extends AbstractModule {
    @Override
    protected void configure() {
      install(new ServletModule()); 
      install(new MonitoringModule());
    }
  }
  
  public static void main(String[] args) throws Exception {
    Injector injector = Guice.createInjector(new Module());

    Server server = injector.getInstance(Proxy.class);
    server.bind(new InetSocketAddress("localhost", 8080));
  }

  @Override
  public void bind(SocketAddress address) throws IOException {
    http.bind(address);
  }

  @Override
  public void close() {
    http.close();
  }
}
