package com.kumbaya.dht;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Before;
import org.junit.Test;
import org.limewire.mojito.Context;
import org.limewire.mojito.MojitoFactory;

public class LoadTest {
  private static final Log log = LogFactory.getLog(LoadTest.class);

  // There will be O(NETWORK_SIZE) threads hanging around, so
  // this test will fail depending on how many threads your
  // computer can take.
  private static final int NETWORK_SIZE = 100;
  private static final int NUMBER_OF_VALUES_PER_NODE = 1;

  @Before
  public void setUp() {
    BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("[%-5p] %d %c - %m%n")));
    Logger.getRootLogger().setLevel(Level.WARN);
  }

  @Test
  public void withUdpDispatcher() throws Exception {
    manyNodes(false);
  }

  @Test
  public void withHttpDispathcer() throws Exception {
    manyNodes(true);
  }

  public void manyNodes(boolean withHttpDispatcher) throws Exception {
    ImmutableList.Builder<Dht> builder = ImmutableList.builder();

    // skip bootstrapping the first node.
    Dht bootstrap = createDht(8000, withHttpDispatcher);
    builder.add(bootstrap);

    // creates nodes and bootstraps them against the first one.
    for (int i = 1; i < NETWORK_SIZE; i++) {
      Dht node = createDht(8000 + i, withHttpDispatcher);
      builder.add(node);
    }

    ImmutableList<Dht> nodes = builder.build();

    try {
      for (int i = 1; i < NETWORK_SIZE; i++) {
        log.info("Waiting for node" + i + " to bootstrap ...");
        nodes.get(i).bootstrap("localhost", 8000).get(1, TimeUnit.MINUTES);
        assertTrue(nodes.get(i).isBootstraped());
      }

      // This test will execute NETWORK_SIZE puts and NETWORK_SIZE ^ 2 gets.

      // If we skip the bootstrap node, we can run this test a lot faster.
      for (int senderId = 1; senderId < NETWORK_SIZE; senderId++) {
        // for every node, publish a key/value pair ...
        for (int msgId = 0; msgId < NUMBER_OF_VALUES_PER_NODE; msgId++) {
          log.info("node" + senderId + " publishing.");
          nodes.get(senderId).put("keys/" + senderId + "/" + msgId,
              "values/" + senderId + "/" + msgId);
          for (int receiverId = 1; receiverId < NETWORK_SIZE; receiverId++) {
            // and for every other node, retrieve that key/value pair ...
            try {
              List<String> result =
                  nodes.get(receiverId).get("keys/" + senderId + "/" + msgId, 1000);
              assertEquals(1, result.size());
              // ... and assert that its value is correct.
              assertEquals(result.get(0), "values/" + senderId + "/" + msgId);
            } catch (TimeoutException e) {
              // Timing out is cool, we just want to have as few as
              // possible of these.
              log.warn("Timeout", e);
            }
          }
        }
      }
    } finally {
      for (int i = 0; i < NETWORK_SIZE; i++) {
        nodes.get(i).close();
      }
    }
  }

  private Dht createDht(int port, final boolean withHttpDispatcher)
      throws NumberFormatException, IOException {
    // Makes sure each instance of the Dht has its own set of classes,
    // rather than sharing instances of @Singletons for example.
    Injector injector =
        Guice.createInjector(new DhtModule(withHttpDispatcher), new AbstractModule() {
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
