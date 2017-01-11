package com.kumbaya.www.gateway;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.kumbaya.common.Flags;
import com.kumbaya.common.Server;
import com.kumbaya.common.Flags.Flag;
import com.kumbaya.router.TcpServer;
import com.kumbaya.router.Packets;
import com.kumbaya.router.Packets.Interest;
import com.kumbaya.router.TcpServer.Handler;
import com.kumbaya.router.TcpServer.Queue;
import com.kumbaya.www.WorldWideWeb;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;

public class Gateway implements Server {
  private static final Log logger = LogFactory.getLog(Gateway.class);

  private final TcpServer server;
  @Inject private InterestHandler handler;
  
  @Inject
  Gateway(TcpServer server) {
    this.server = server;
  }
  
  public static class Module extends AbstractModule {
    @Override
    protected void configure() {
      bind(ExecutorService.class).toInstance(Executors.newFixedThreadPool(1));
    }
  }
  
  private static class InterestHandler implements Handler<Interest> {
    @Inject
    InterestHandler() {
    }
    
    @Override
    public void handle(Interest request, Queue response) throws IOException {
      // Fetches the content of the page.
      try {
        Optional<String> content = WorldWideWeb.get(request.getName().getName());
        // If the content is available, return it.
        if (content.isPresent()) {
          Packets.Data data = new Packets.Data();
          data.getName().setName(request.getName().getName());
          data.getMetadata().setFreshnessPeriod(2);
          data.setContent(content.get().getBytes());
          response.push(data);
        }      
      } catch (IOException e) {
        // Ignores 500s, assumes content isn't available. 
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
    BasicConfigurator.configure(new ConsoleAppender(new PatternLayout(
        "[%-5p] %d %c - %m%n")));
    
    logger.info("Running the Kumbaya Gateway");

    Set<Flag<?>> options = ImmutableSet.of(
        Flag.of("host", "The external hostname", true, "localhost"),
        Flag.of("port", "The external hostname", true, 8081)
        );

    Flags flags = Flags.parse(options, args);
    final String host = flags.get("host");
    final int port = flags.get("port");
    
    Gateway router = Guice.createInjector(new Module()).getInstance(Gateway.class);
    router.bind(new InetSocketAddress(host, port));
  }
}
