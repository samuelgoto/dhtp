package com.kumbaya.router;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
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
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;

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
    private final InetSocketAddress forwardingRouter;

    public Module(InetSocketAddress forwardingRouter) {
      this.forwardingRouter = forwardingRouter;
    }

    @Override
    protected void configure() {
      ThreadFactory factory = new ThreadFactoryBuilder()
        .setNameFormat("Router-%d").build();
      bind(ExecutorService.class).toInstance(Executors.newFixedThreadPool(1, factory));
      bind(InetSocketAddress.class).toInstance(forwardingRouter);
    }
  }

  static class InterestHandler implements Handler<Interest> {
    private final Client client;

    @Inject
    InterestHandler(Client client) {
      this.client = client;
    }

    @Override
    public void handle(Interest request, Queue response) throws IOException {
      logger.info("Handling a request: " + request.getName().getName());
      try {
        // Forwards the interest to the next hop.
        Optional<Data> result = client.send(request);

        if (result.isPresent()) {
          logger.info("Got a Data packet response [" + result.get().getContent().length + " bytes], responding.");
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
    Packets.register();
    server.register(Interest.class, handler);
    server.bind(address);
  }

  @Override
  public void close() throws IOException {
    server.close();
  }

  public static void main(String[] args) throws Exception {
	BasicConfigurator.resetConfiguration();
    BasicConfigurator.configure(new ConsoleAppender(new PatternLayout(
        "[%-5p] %d %c - %m%n")));
    
    logger.info("Running the Kumbaya Router");

    Set<Flag<?>> options = ImmutableSet.of(
        Flag.of("host", "The external hostname", true, "localhost"),
        Flag.of("port", "The external hostname", true, 8082),
        Flag.of("forwarding", "The external ip/port of the forwarding table", true, "localhost:8081")
        );

    Flags flags = Flags.parse(options, args);
    final String host = flags.get("host");
    final int port = flags.get("port");
    final String forwarding = flags.get("forwarding");
    
    Router router = Guice.createInjector(
        new Module(InetSocketAddresses.parse(forwarding)))
        .getInstance(Router.class);
    router.bind(new InetSocketAddress(host, port));
  }
}
