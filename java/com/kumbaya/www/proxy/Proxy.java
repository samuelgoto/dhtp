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
    private final InetSocketAddress entrypoint;
    public Module(InetSocketAddress entrypoint) {
      this.entrypoint = entrypoint;
    }
    
    @Override
    protected void configure() {
      install(new ServletModule()); 
      install(new MonitoringModule());
      
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
  public void bind(SocketAddress address) throws IOException {
    http.bind(address);
  }

  @Override
  public void close() {
    http.close();
  }
}
