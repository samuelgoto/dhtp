package com.kumbaya;

import com.google.common.base.Optional;
import com.kumbaya.common.InetSocketAddresses;
import com.kumbaya.common.Server;
import com.kumbaya.router.Router;
import com.kumbaya.www.WorldWideWeb;
import com.kumbaya.www.gateway.Gateway;
import com.kumbaya.www.proxy.Proxy;
import com.kumbaya.www.testing.WorldWideWebServer;
import junit.framework.TestCase;

public class CommandLineTest extends TestCase {
  public void testRunnningAll() throws Exception {
    // Spins up a web server.
    Server www = WorldWideWebServer.server(WorldWideWebServer.defaultServlets());
    www.bind(InetSocketAddresses.parse("localhost:9080"));
    
    // Spins up a gateway.
    Gateway.main(new String[] {
        "--host=localhost",
        "--port=9081"
        });
    
    // Spins up a router.
    Router.main(new String[] {
        "--host=localhost",
        "--port=9082",
        // Points to the gateway.
        "--forwarding=localhost:9081",
        });
    
    // Spins up a proxy.
    Proxy.main(new String[] {
        "--host=localhost",
        "--port=9083",
        // Points to the router.
        "--entrypoint=localhost:9082",
        });
    
    // Builds a client request against the proxy and traverses the network looking for content.
    Optional<String> content = WorldWideWeb.get(
        InetSocketAddresses.parse("localhost:9083"),
        "http://localhost:9080/helloworld");
    assertTrue(content.isPresent());
    assertEquals("hello world", content.get());
  }
}
