package com.kumbaya;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.kumbaya.common.testing.Supplier;
import com.kumbaya.common.testing.LocalNetwork;
import com.kumbaya.www.WorldWideWeb;
import com.kumbaya.www.WorldWideWeb.Resource;
import junit.framework.TestCase;

public class MultithreadingTest extends TestCase {
  private static final Log logger = LogFactory.getLog(MultithreadingTest.class);
  
  Supplier<LocalNetwork> network = LocalNetwork.supplier(ImmutableMap.of(
      "/please-sleep", SleepServlet.class));
  
  @Override
  public void setUp() throws IOException {
    network.clear().get().setUp();
  }
  
  @Override
  public void tearDown() throws Exception {
    network.get().tearDown();
  }
  
  @SuppressWarnings("serial")
  private static class SleepServlet extends HttpServlet {
    private static final Log logger = LogFactory.getLog(SleepServlet.class);
    private AtomicInteger counter = new AtomicInteger(0);
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
      try {
        int sleep = 50;
        logger.info("Sleeping for " + sleep + " milliseconds");
        Thread.sleep(sleep);
        response.getWriter().print("hello world");
        response.getWriter().close();
        logger.info("Done sleeping " + sleep + " milliseconds, returning.");
      } catch (InterruptedException e) {
        response.sendError(500);
      }
    }
  }

  public void testRunnningAll() throws Exception {
    List<Thread> requests = new ArrayList<Thread>();

    AtomicInteger errors = new AtomicInteger();
    
    for (int i = 0; i < 100; i++) {
      requests.add(new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            logger.info("Sending request");
            
            Optional<Resource> content = WorldWideWeb.get(
                new InetSocketAddress("localhost", 8080),
                "http://localhost:6060/please-sleep");
            
            assertTrue(content.isPresent());
            assertEquals("hello world", new String(content.get().content()));
            logger.info("Got response");
          } catch (SocketTimeoutException e) {
            errors.incrementAndGet();
          } catch (IOException e) {
            errors.incrementAndGet();
          } catch (Exception e) {
            errors.incrementAndGet();
          }
        }
      }));
    }
    
    assertEquals(0, errors.get());
    
    long startTime = System.currentTimeMillis();

    // Kicks off all requests in parallel.
    for (Thread request : requests) {
      logger.info("Starting thread");
      request.start();
    }

    // Wait for all of them to return.
    for (Thread request : requests) {
      logger.info("Joining thread");
      request.join();
    }

    long totalTime = System.currentTimeMillis() - startTime;
    logger.info("Total time = " + totalTime);
  }
}
