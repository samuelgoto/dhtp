package com.kumbaya.router;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.kumbaya.common.Flags;
import com.kumbaya.common.Flags.Flag;
import com.kumbaya.common.InetSocketAddresses;
import com.kumbaya.common.Server;
import com.kumbaya.router.Packets.Data;
import com.kumbaya.router.Packets.Interest;
import com.kumbaya.router.TcpServer.Handler;
import com.kumbaya.router.TcpServer.Queue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Router implements Server {
  private static final Log logger = LogFactory.getLog(Router.class);

  private final TcpServer server;
  @Inject
  private InterestHandler handler;

  @Inject
  Router(TcpServer server) {
    this.server = server;
  }

  public static class Module extends AbstractModule {
    @Override
    protected void configure() {
      ThreadFactory factory = new ThreadFactoryBuilder()
        .setNameFormat("Router-%d").build();
      bind(ExecutorService.class).toInstance(Executors.newFixedThreadPool(10, factory));
    }
  }

  static class InterestHandler implements Handler<Interest> {
    private @Inject @Flag("forwarding") String forwarding = "localhost:8082";
    private final Kumbaya client;

    @Inject
    InterestHandler(Kumbaya client) {
      this.client = client;
    }

    @Override
    public void handle(Interest request, Queue response) throws IOException {
      logger.info("Handling a request: " + request.getName().getName());
      try {
        // Forwards the interest to the next hop.
        Optional<Data> result = client.send(InetSocketAddresses.parse(forwarding), request);

        if (result.isPresent()) {
          logger.info("Got a Data packet, responding.");
          response.push(result.get());
        }
      } catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
    	  logger.error("Unexpected error: ", e);
    	  e.printStackTrace();
      }
    }
  }

  @Override
  public void bind(InetSocketAddress address) throws IOException {
    logger.info("Binding into " + host + ":" + port);
    Packets.register();
    server.register(Interest.class, handler);
    server.bind(address);
  }

  @Override
  public void close() throws IOException {
    server.close();
  }

  private @Inject(optional=true) @Flag("host") String host = "localhost";
  private @Inject(optional=true) @Flag("port") int port = 8082;
  
  public static void main(String[] args) throws Exception {
    logger.info("Running the Kumbaya Router");

    Router router = Guice.createInjector(new Module(), Flags.asModule(args))
        .getInstance(Router.class);
    router.bind(new InetSocketAddress(router.host, router.port));
  }
}
