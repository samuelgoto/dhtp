package com.kumbaya;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.kumbaya.common.InetSocketAddresses;
import com.kumbaya.router.Router;
import com.kumbaya.www.WorldWideWeb;
import com.kumbaya.www.gateway.Gateway;
import com.kumbaya.www.proxy.Proxy;
import java.util.List;
import junit.framework.TestCase;

public class PublicNetworkTest extends TestCase {
  public void testRunnningAll() throws Exception {
    // Spins up a gateway.
    Gateway.main(new String[] {
        "--host=localhost",
        "--port=19081"
    });

    // Spins up a router.
    Router.main(new String[] {
        "--host=localhost",
        "--port=19082",
        // Points to the gateway.
        "--forwarding=localhost:19081",
    });

    // Spins up a proxy.
    Proxy.main(new String[] {
        "--host=localhost",
        "--port=19083",
        // Points to the router.
        "--entrypoint=localhost:19082",
    });
    
    List<String> urls = ImmutableList.of(
        "http://sgo.to",
        "http://1500wordmtu.com",
        "http://www.1500wordmtu.com/2017/digital-objects-last-foreveror-five-years-whichever-comes-first",
        "http://johnpanzer.com",
        "http://cnn.com",
        "http://cloud.google.com/compute/docs/activity-logs");
    
    for (String url : urls) {
      // Builds a client request against the proxy and traverses the network looking for content.
      Optional<String> content = WorldWideWeb.get(
          InetSocketAddresses.parse("localhost:19083"), url);
      assertTrue(content.isPresent());
    }
  }
}
