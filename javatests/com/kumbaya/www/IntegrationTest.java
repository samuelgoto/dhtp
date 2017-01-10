package com.kumbaya.www;

import com.google.inject.Guice;
import com.kumbaya.router.Router;
import com.kumbaya.www.gateway.Gateway;
import com.kumbaya.www.proxy.Proxy;
import java.io.IOException;
import java.net.InetSocketAddress;
import junit.framework.TestCase;

public class IntegrationTest extends TestCase {

  
  public void testNetwork() throws IOException {
    Proxy proxy = Guice.createInjector(new Proxy.Module()).getInstance(Proxy.class);
    proxy.bind(new InetSocketAddress("localhost", 8080));
    
    Router router = Guice.createInjector().getInstance(Router.class);
    router.bind(new InetSocketAddress("localhost", 9090));

    Gateway gateway = Guice.createInjector().getInstance(Gateway.class);
    gateway.bind(new InetSocketAddress("localhost", 7070));

    proxy.close();
    router.close();
    gateway.close();
  }
}
