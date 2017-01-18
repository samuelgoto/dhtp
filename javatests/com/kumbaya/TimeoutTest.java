package com.kumbaya;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.kumbaya.common.InetSocketAddresses;
import com.kumbaya.common.testing.LocalNetwork;
import com.kumbaya.common.testing.Supplier;
import com.kumbaya.www.WorldWideWeb;
import com.kumbaya.www.WorldWideWeb.Resource;
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
  
  private final Supplier<LocalNetwork> network = LocalNetwork.supplier(
      ImmutableMap.of("/deliberately-timeouts", DeliberatelyTimeoutsServlet.class));

  @Override
  public void setUp() throws Exception {
    network.clear().get().setUp();
  }
  
  @Override
  public void tearDown() throws Exception {
    network.get().tearDown();
  }
  
  public void testRunnningAll() throws Exception {
    // Builds a client request against the proxy and traverses the network looking for content.
    WorldWideWeb.setTimeout(TimeUnit.SECONDS.toMillis(2));
    Optional<Resource> content = WorldWideWeb.get(
        InetSocketAddresses.parse("localhost:8080"),
        "http://localhost:8083/deliberately-timeouts");
    assertFalse(content.isPresent());
  }
}
