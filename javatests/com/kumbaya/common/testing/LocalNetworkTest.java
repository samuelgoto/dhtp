package com.kumbaya.common.testing;

import com.google.common.base.Optional;
import com.kumbaya.www.WorldWideWeb;
import com.kumbaya.www.WorldWideWeb.Resource;
import java.io.IOException;
import junit.framework.TestCase;

public class LocalNetworkTest extends TestCase {
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
}

