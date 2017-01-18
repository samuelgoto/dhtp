package com.kumbaya.www;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.kumbaya.common.InetSocketAddresses;
import com.kumbaya.common.Server;
import com.kumbaya.common.testing.LocalNetwork;
import com.kumbaya.common.testing.Supplier;
import com.kumbaya.common.testing.WorldWideWebServer;
import com.kumbaya.router.Router;
import com.kumbaya.www.WorldWideWeb;
import com.kumbaya.www.WorldWideWeb.Resource;
import com.kumbaya.www.gateway.Gateway;
import com.kumbaya.www.proxy.Proxy;
import junit.framework.TestCase;

public class LoadTest extends TestCase {
	@SuppressWarnings("serial")
	private static class OneMegabyteFileServlet extends HttpServlet {
		@Override
		protected void doGet(HttpServletRequest request, HttpServletResponse response)
				throws IOException, ServletException {
			// Writes one megabyte.
			for (int i = 0; i < 1000; i++) {
				byte[] kilobyte = new byte[1000];
				response.getOutputStream().write(kilobyte);
			}
			response.flushBuffer();
			response.getOutputStream().close();
		}	
	}

	private final Supplier<LocalNetwork> network = LocalNetwork.supplier(
	    ImmutableMap.of("/onemegabytefile", OneMegabyteFileServlet.class));
	
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
		for (int i = 0; i < 10; i++) {
			Optional<Resource> content = WorldWideWeb.get(
					InetSocketAddresses.parse("localhost:8080"),
					"http://localhost:8083/onemegabytefile");
			assertTrue(content.isPresent());
			assertEquals(1000 * 1000, content.get().content().length);
		}
	}
}
