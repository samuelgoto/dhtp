package com.kumbaya.www;

import java.io.IOException;
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

public class ContentTypeTest extends TestCase {
  @SuppressWarnings("serial")
  private static class ImageServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
      response.setContentType("image/jpeg");
      response.getWriter().print("hi");
    }	
  }
  
  private final Supplier<LocalNetwork> network = LocalNetwork.supplier(
      ImmutableMap.of("/image.jpg", ImageServlet.class));

  @Override
  public void setUp() throws Exception {
    network.clear().get().setUp();
  }
  
  @Override
  public void tearDown() throws Exception {
    network.get().tearDown();
  }
  
  public void testsThatTheContentTypeIsPropagatedThroughTheNetwork() throws Exception {
    Optional<Resource> content = WorldWideWeb.get(
        InetSocketAddresses.parse("localhost:8080"),
        "http://localhost:8083/image.jpg");
    assertTrue(content.isPresent());
    assertEquals("image/jpeg;charset=ISO-8859-1", content.get().contentType());
  }
}
