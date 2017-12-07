package com.kumbaya.dht;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.kumbaya.common.Flags;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

public class SpeedTest {

  private static class Timer {
    private static final Log log = LogFactory.getLog(Timer.class);
    private long begin = System.currentTimeMillis();

    long check(String message) {
      long ellapsed = System.currentTimeMillis() - begin;
      log.info(message + " [" + ellapsed + "ms]");
      begin = System.currentTimeMillis();
      return ellapsed;
    }
  }

  @Test
  public void testStartStop() throws Exception {
    Timer timer = new Timer();

    Dht bootstrap = createDht(8080);

    timer.check("Starting DHT");
    bootstrap.start();
    assertTrue(timer.check("Started DHT") < 100);
    bootstrap.stop();
    assertTrue(timer.check("Stopped DHT") < 10);
  }

  @Test
  public void testBootstrap() throws Exception {
    Timer timer = new Timer();

    Dht bootstrap = createDht(8080);
    bootstrap.start();

    Dht node = createDht(8081);
    timer.check("Starting node and bootstrapping it");
    node.start();
    node.bootstrap("localhost", 8080).get(100, TimeUnit.MILLISECONDS);
    assertTrue(node.isBootstraped());
    assertTrue(timer.check("Bootstrapped node") < 100);

    node.stop();
    bootstrap.stop();
  }

  @Test
  public void testPut() throws Exception {
    Timer timer = new Timer();

    Dht bootstrap = createDht(8080);
    bootstrap.start();

    Dht node = createDht(8081);

    node.start();
    node.bootstrap("localhost", 8080).get(100, TimeUnit.MILLISECONDS);

    timer.check("Putting a value");
    node.put("hello", "world");
    assertTrue(timer.check("Done putting a value") < 50);

    node.stop();
    bootstrap.stop();
  }

  @Test
  public void testGet() throws Exception {
    Timer timer = new Timer();

    Dht bootstrap = createDht(8080);
    bootstrap.start();

    Dht node = createDht(8081);

    node.start();
    node.bootstrap("localhost", 8080).get(100, TimeUnit.MILLISECONDS);

    node.put("hello", "world");

    timer.check("Getting a value");
    List<String> result = node.get("hello", 100);
    assertEquals(1, result.size());
    assertEquals("world", result.get(0));

    assertTrue(timer.check("Done getting a value") < 50);

    node.stop();
    bootstrap.stop();
  }

  @Test
  public void testGetFromAnotherNode() throws Exception {
    Timer timer = new Timer();

    Dht bootstrap = createDht(8080);
    bootstrap.start();

    Dht node = createDht(8081);
    node.start();
    node.bootstrap("localhost", 8080).get(100, TimeUnit.MILLISECONDS);

    Dht other = createDht(8082);
    other.start();
    other.bootstrap("localhost", 8080).get(100, TimeUnit.MILLISECONDS);

    node.put("hello", "world");

    timer.check("Getting a value");
    List<String> result = other.get("hello", 100);
    assertEquals(1, result.size());
    assertEquals("world", result.get(0));

    assertTrue(timer.check("Done getting a value from another node") < 50);

    node.stop();
    other.stop();
    bootstrap.stop();
  }

  @Test
  public void testExchanging10Messages() throws Exception {
    Timer timer = new Timer();

    Dht bootstrap = createDht(8080);
    bootstrap.start();

    Dht node = createDht(8081);
    node.start();
    node.bootstrap("localhost", 8080).get(100, TimeUnit.MILLISECONDS);

    Dht other = createDht(8082);
    other.start();
    other.bootstrap("localhost", 8080).get(100, TimeUnit.MILLISECONDS);

    timer.check("Writing 10 values");
    for (int i = 0; i < 10; i++) {
      node.put("hello" + i, "world" + i);
    }
    assertTrue(timer.check("Done writing 10 values") < 100 * 10);

    timer.check("Getting 10 values");
    for (int i = 0; i < 10; i++) {
      List<String> result = other.get("hello" + i, 100);
      assertEquals(1, result.size());
      assertEquals("world" + i, result.get(0));
    }

    assertTrue(timer.check("Done getting 10 values") < 50 * 10);

    node.stop();
    other.stop();
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
