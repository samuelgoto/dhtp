package com.kumbaya;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Optional;
import com.kumbaya.common.InetSocketAddresses;
import com.kumbaya.common.Server;
import com.kumbaya.router.Router;
import com.kumbaya.www.WorldWideWeb;
import com.kumbaya.www.gateway.Gateway;
import com.kumbaya.www.proxy.Proxy;
import com.kumbaya.www.testing.WorldWideWebServer;

import junit.framework.TestCase;

public class TimeoutTest extends TestCase {
  @SuppressWarnings("serial")
  private static class DeliberatelyTimeoutsServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
      try {
        // Sleeps for 10 seconds.
        Thread.sleep(5 * 1000);
      } catch (InterruptedException e) {
        throw new IOException("Failed to sleep", e);
      }
    }	
  }

  public void testRunnningAll() throws Exception {
    // Spins up a web server.
    Server www = WorldWideWebServer.server(
        "/deliberately-timeouts", DeliberatelyTimeoutsServlet.class);
    www.bind(InetSocketAddresses.parse("localhost:29080"));

    // Spins up a gateway.
    Gateway.main(new String[] {
        "--host=localhost",
        "--port=29081"
    });

    // Spins up a router.
    Router.main(new String[] {
        "--host=localhost",
        "--port=29082",
        // Points to the gateway.
        "--forwarding=localhost:29081",
    });

    // Spins up a proxy.
    Proxy.main(new String[] {
        "--host=localhost",
        "--port=29083",
        // Points to the router.
        "--entrypoint=localhost:29082",
    });

    // Builds a client request against the proxy and traverses the network looking for content.
    Optional<String> content = WorldWideWeb.get(
        InetSocketAddresses.parse("localhost:29083"),
        "http://localhost:29080/deliberately-timeouts");
    assertFalse(content.isPresent());

    System.out.println("Network returned a non-present content for the timeout.");
    // Sleeps for 10 seconds.
    Thread.sleep(10 * 1000);
  }
}
