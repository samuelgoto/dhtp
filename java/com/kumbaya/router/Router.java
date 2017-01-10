package com.kumbaya.router;

import com.google.common.base.Optional;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.kumbaya.common.Server;
import com.kumbaya.router.Packets.Data;
import com.kumbaya.router.Packets.Interest;
import com.kumbaya.router.TcpServer.Handler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;

public class Router implements Server {
  private final TcpServer server;
  private final Thread thread;
  @Inject
  private InterestHandler handler;
  
  @Inject
  Router(TcpServer server) {
    this.server = server;
    this.thread = new Thread(server);
  }
  
  public static class Module extends AbstractModule {
    private final InetSocketAddress forwardingRouter;
    
    public Module(InetSocketAddress forwardingRouter) {
      this.forwardingRouter = forwardingRouter;
    }
    
    @Override
    protected void configure() {
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
    public void handle(Interest request, OutputStream response) {
      try {
        // Forwards the interest to the next hop.
        Optional<Data> result = client.send(request);
        
        if (result.isPresent()) {
          Serializer.serialize(response, result.get());
        }
      } catch (IllegalArgumentException | IllegalAccessException | InstantiationException
          | IOException e) {
      }
    }
  }
  
  @Override
  public void bind(InetSocketAddress address) throws IOException {
    server.bind(address);
    server.register(Interest.class, handler);
    thread.start();
  }

  @Override
  public void close() {
  }

  public static void main(String[] args) throws Exception {
    BasicConfigurator.configure(new ConsoleAppender(new PatternLayout(
        "[%-5p] %d %c - %m%n")));

    Options options = new Options();
    options.addOption("port", true, "The external port");
    options.addOption("hostname", true, "The external hostname");
    options.addOption("bootstrap", true, "The node to bootstrap");
    options.addOption("db", true, "Whether to write values to disk or not");

    CommandLineParser parser = new PosixParser();
    CommandLine line = parser.parse(options, args);

    line.getOptionValue("port");

    final int port;
    if (System.getenv().containsKey("PORT")) {
      port = Integer.valueOf(System.getenv("PORT"));
    } else if (line.hasOption("port")) {
      port = Integer.valueOf(line.getOptionValue("port"));
    } else {
      port = 8080;
    }

    final int proxy;
    final String hostname;
    if (line.hasOption("hostname")) {
      String[] ip = line.getOptionValue("hostname").split(":");
      hostname = ip[0];
      proxy = Integer.valueOf(ip[1]);
    } else {
      proxy = port;
      hostname = "localhost";
    }
  }

}
