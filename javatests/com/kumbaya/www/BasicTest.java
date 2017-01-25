package com.kumbaya.www;

import com.google.common.base.Optional;
import com.kumbaya.common.InetSocketAddresses;
import com.kumbaya.common.testing.LocalNetwork;
import com.kumbaya.common.testing.Supplier;
import com.kumbaya.common.testing.WorldWideWebServer;
import com.kumbaya.www.WorldWideWeb.Resource;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import junit.framework.TestCase;

public class BasicTest extends TestCase {
  Supplier<LocalNetwork> network = LocalNetwork.supplier(WorldWideWebServer.defaultServlets());

  @Override
  public void setUp() throws Exception {
    network.clear().get().setUp();
  }

  @Override
  public void tearDown() throws Exception {
    network.get().tearDown();
  }

  public void test404s_directly() throws IOException {
    // Straight to the server
    Optional<Resource> result = WorldWideWeb.get("http://localhost:8083/doesntexist");
    assertFalse(result.isPresent());
  }

  public void test200_direclty() throws IOException {
    Optional<Resource> result = WorldWideWeb.get("http://localhost:8083/helloworld");
    assertTrue(result.isPresent());
    assertEquals("hello world", new String(result.get().content()));
  }

  public void test200_throughNetwork() throws IOException {
    Optional<Resource> result = WorldWideWeb.get(InetSocketAddresses.parse("localhost:8080"),
        "http://localhost:8083/helloworld");
    assertTrue(result.isPresent());
    assertEquals("hello world", new String(result.get().content()));
  }

  public void test404s_throughNetwork() throws Exception {
    // Through the network
    Optional<Resource> proxied = WorldWideWeb.get(new InetSocketAddress("localhost", 8080),
        "http://localhost:8083/doesntexist");
    assertFalse(proxied.isPresent());
  }

  public void test404s_throughNetworkDnsDoesntExist() throws Exception {
    // Through the network
    Optional<Resource> proxied = WorldWideWeb.get(new InetSocketAddress("localhost", 8080),
        "http://localhost:9999/doesntexist");
    assertFalse(proxied.isPresent());
  }

  public void atest500s_throughNetwork() throws Exception {
    // Through the network
    Optional<Resource> proxied =
        WorldWideWeb.get(new InetSocketAddress("localhost", 8080), "http://localhost:8083/error");
    assertFalse(proxied.isPresent());
  }

  // TODO(goto): tests for loops in the network! Just ran into one right now.

  public void atest200_throughNetwork_notProxied() throws IOException {
    // This test requires you to add the following line to your /etc/host file.
    try {
      Optional<Resource> result =
          WorldWideWeb.get("http://localhost-6060.kumbaya.io:8080/helloworld");
      assertTrue(result.isPresent());
      assertEquals("hello world", result.get().content());
    } catch (UnknownHostException e) {
      e.printStackTrace();
      System.out.println("===================================================================");
      System.out.println("DNS set up failure: ignoring this test.");
      System.out.println("This test requires you to add the following line to /etc/host");
      System.out.println("127.0.0.1     localhost.6060.kumbaya.io");
      System.out.println("===================================================================");
    }
  }

  public void atest200_throughNetwork_publicNetwork() throws IOException {
    // This test requires you to add the following line to your /etc/host file.
    try {
      Optional<Resource> result =
          WorldWideWeb.get("http://sgo.to.kumbaya.io:8080/google6986897775888699.html");
      assertTrue(result.isPresent());
      assertEquals("google-site-verification: google6986897775888699.html", result.get().content());
    } catch (UnknownHostException e) {
      e.printStackTrace();
      System.out.println("===================================================================");
      System.out.println("DNS set up failure: ignoring this test.");
      System.out.println("This test requires you to add the following line to /etc/host");
      System.out.println("127.0.0.1     sgo.to.kumbaya.io");
      System.out.println("===================================================================");
    }
  }

  public void testBreaksInfiniteLoop() throws IOException {
    // We'll send a request to the proxy to fetch content from the proxy itself, leading into
    // an infinite loop.
    // WorldWideWeb.setTimeout(TimeUnit.MINUTES.toMillis(5));
    Optional<Resource> result = WorldWideWeb.get(new InetSocketAddress("localhost", 8080),
        "http://localhost:8080/index.php");
    assertFalse(result.isPresent());
  }
}

