package com.kumbaya;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.kumbaya.common.InetSocketAddresses;
import com.kumbaya.common.testing.LocalNetwork;
import com.kumbaya.common.testing.Supplier;
import com.kumbaya.www.WorldWideWeb;
import com.kumbaya.www.WorldWideWeb.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;

public class PublicNetworkTest extends TestCase {
  Supplier<LocalNetwork> network = LocalNetwork.supplier(ImmutableMap.of());
  
  @Override
  public void setUp() throws Exception {
    network.clear().get().setUp();
  }
  
  @Override
  public void tearDown() throws Exception {
    network.get().tearDown();
  }
  
  public void testRunnningAll() throws Exception {
    List<String> urls = ImmutableList.of(
        // Some default servers that I normally try manually.
        "http://sgo.to",
        "http://1500wordmtu.com",
        "http://www.1500wordmtu.com/2017/digital-objects-last-foreveror-five-years-whichever-comes-first",
        "http://johnpanzer.com",
        "http://cnn.com",
        // This is a good test because it has some weird markup.
        "http://cloud.google.com/compute/docs/activity-logs");
    
    for (String url : urls) {
      // Builds a client request against the proxy and traverses the network looking for content.
      WorldWideWeb.setTimeout(TimeUnit.MINUTES.toMillis(5));
      Optional<Resource> content = WorldWideWeb.get(
          InetSocketAddresses.parse("localhost:8080"), url);
      assertTrue(content.isPresent());
    }
  }
}
