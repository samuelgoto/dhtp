package com.kumbaya.router;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
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
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;

public class Router implements Server {
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
      bind(ExecutorService.class).toInstance(Executors.newFixedThreadPool(1));
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
    public void handle(Interest request, Queue response) {
      try {
        // Forwards the interest to the next hop.
        Optional<Data> result = client.send(request);

        if (result.isPresent()) {
          response.push(result.get());
        }
      } catch (IllegalArgumentException | IllegalAccessException | InstantiationException
          | IOException e) {
      }
    }
  }

  @Override
  public void bind(InetSocketAddress address) throws IOException {
    server.register(Interest.class, handler);
    server.bind(address);
  }

  @Override
  public void close() throws IOException {
    server.close();
  }

  public static void main(String[] args) throws Exception {
    BasicConfigurator.configure(new ConsoleAppender(new PatternLayout(
        "[%-5p] %d %c - %m%n")));

    Set<Flag<?>> options = ImmutableSet.of(
        Flag.of("host", "The external hostname", true, "localhost"),
        Flag.of("port", "The external hostname", true, 8080),
        Flag.of("forwarding", "The external ip/port of the forwarding table", true, "localhost:9090")
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
