package com.kumbaya.dht;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.kumbaya.common.Flags;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.limewire.mojito.exceptions.NotBootstrappedException;

public class DhtTest {

  @Test
  public void testBasic() throws Exception {
    Dht bootstrap = createDht(8080);
    bootstrap.start();

    Dht node1 = createDht(8081);
    node1.start();

    Dht node2 = createDht(8082);
    node2.start();

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

    node1.stop();
    node2.stop();
    bootstrap.stop();
  }

  @Test
  public void testAlone() throws Exception {
    Dht bootstrap = createDht(8080);
    bootstrap.start();

    try {
      bootstrap.get("/foo", 100);
      fail("Should not return anything unless you are bootstrapped");
    } catch (NotBootstrappedException e) {
      // expected
    }

    bootstrap.stop();
  }

  private Dht createDht(int port) {
    // Makes sure each instance of the Dht has its own set of classes,
    // rather than sharing instances of @Singletons for example.
    Injector injector = Guice.createInjector(new DhtModule(),
        Flags.asModule(new String[] {"--port=" + port, "--host=localhost"}));

    Dht dht = injector.getInstance(Dht.class);

    return dht;
  }
}
