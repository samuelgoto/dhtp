package com.kumbaya.www.gateway;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
      ThreadFactory factory = new ThreadFactoryBuilder()
        .setNameFormat("Gateway-%d").build();
      bind(ExecutorService.class).toInstance(Executors.newFixedThreadPool(1, factory));
    }
  }
  
  private static class InterestHandler implements Handler<Interest> {
    @Override
    public void handle(Interest request, Queue response) throws IOException {
      // Fetches the content of the page.
      logger.info("Got a request to fetch " + request.getName().getName());
      try {
        Optional<String> content = WorldWideWeb.get(request.getName().getName());
        // If the content is available, return it.
        if (content.isPresent()) {
        	logger.info("Got data back from the web (" + content.get().length() + " bytes), returning");
          Packets.Data data = new Packets.Data();
          data.getName().setName(request.getName().getName());
          data.getMetadata().setFreshnessPeriod(2);
          data.setContent(content.get().getBytes());
          response.push(data);
          logger.info("Finished writing the data");
        }
      } catch (SocketException e) {
    	  // Socket error, re-throwing.
        logger.error("Unexpected SocketException", e);
        throw new RuntimeException("Programming error: application protocol busted (client closed the stream before the server was done writing)");
        // throw e;
      } catch (IOException e) {
    	  // TODO(goto): we really have to be able to make a distinction between
    	  // 500s and IOExceptions.
        logger.error("Got an unexpected error: ", e);
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
