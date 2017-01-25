package com.kumbaya.dht;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.limewire.mojito.Context;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.exceptions.NotBootstrappedException;

public class DhtTest {

  @Test
  public void testBasic() throws Exception {
    Dht bootstrap = createDht(8080);

    Dht node1 = createDht(8081);
    Dht node2 = createDht(8082);

    node1.bootstrap("localhost", 8080).get(1, TimeUnit.MINUTES);
    assertTrue(node1.isBootstraped());

    node2.bootstrap("localhost", 8080).get(1, TimeUnit.MINUTES);
    assertTrue(node2.isBootstraped());

    // Inserts on node 1.
    node1.put("foo", "bar");

    // Fetches from node 2.
    List<String> result = node2.get("foo", 200);

    assertEquals(1, result.size());
    assertEquals("bar", result.get(0));

    node1.close();
    node2.close();
    bootstrap.close();
  }

  @Test
  public void testAlone() throws Exception {
    Dht bootstrap = createDht(8080);

    try {
      bootstrap.get("/foo", 100);
      fail("Should not return anything unless you are bootstrapped");
    } catch (NotBootstrappedException e) {
      // expected
    }

    bootstrap.close();
  }

  private Dht createDht(int port) throws NumberFormatException, IOException {
    // Makes sure each instance of the Dht has its own set of classes,
    // rather than sharing instances of @Singletons for example.
    Injector injector = Guice.createInjector(new DhtModule(), new AbstractModule() {
      @Override
      protected void configure() {
        bind(Context.class).toInstance((Context) MojitoFactory.createDHT("localhost"));
      }
    });

    Dht dht = injector.getInstance(Dht.class);

    dht.bind(new InetSocketAddress("localhost", port));

    return dht;
  }
}
