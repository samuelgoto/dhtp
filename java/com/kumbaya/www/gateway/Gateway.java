package com.kumbaya.www.gateway;

import com.google.common.base.Optional;
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
import com.kumbaya.router.TcpServer.Interface;
import com.kumbaya.www.WorldWideWeb;
import com.kumbaya.www.WorldWideWeb.Resource;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.HttpHostConnectException;

public class Gateway implements Server {
  private static final Log logger = LogFactory.getLog(Gateway.class);

  private final TcpServer server;
  
  @Inject
  Gateway(TcpServer server) {
    this.server = server;
  }
  
  public static class Module extends AbstractModule {
    @Override
    protected void configure() {
      ThreadFactory factory = new ThreadFactoryBuilder()
        .setNameFormat("Gateway-%d").build();
      bind(ExecutorService.class).toInstance(Executors.newFixedThreadPool(10, factory));

      install(new TcpServer.HandlerModule() {
        @Override
        protected void register() {
          addHandler(Interest.class, InterestHandler.class);
        }
      });
    }
  }
  
  private static class InterestHandler implements Handler<Interest> {
    @Override
    public void handle(Interest request, Interface response) throws IOException {
      // Fetches the content of the page.
      logger.info("Got a request to fetch " + request.getName().getName());
      try {
        Optional<Resource> content = WorldWideWeb.get(request.getName().getName());
        // If the content is available, return it.
        if (content.isPresent()) {
          logger.info("Got data back from the web, returning");
          Packets.Data data = new Packets.Data();
          data.getName().setName(request.getName().getName());
          data.getMetadata().setFreshnessPeriod(2);
          // TODO(goto): figure out how to pass the content type back to the network. Will require
          // us to re-do the WorldWideWeb class.
          data.getMetadata().setContentType(content.get().contentType());
          data.setContent(content.get().content());
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
      } finally {
        response.close();
      }
    }
  }
  
  @Override
  public void bind(InetSocketAddress address) throws IOException {
    logger.info("Binding into " + host + ":" + port);
    Packets.register();
    server.bind(address);
  }

  @Override
  public void close() throws IOException {
    server.close();
  }
  
  private @Inject(optional=true) @Flag("host") String host = "localhost";
  private @Inject(optional=true) @Flag("port") int port = 8081;
  
  public static void main(String[] args) throws Exception {
    logger.info("Running the Kumbaya Gateway");
    
    Gateway gateway = Guice.createInjector(
        new Module(),
        Flags.asModule(args))
        .getInstance(Gateway.class);
    gateway.bind(new InetSocketAddress(gateway.host, gateway.port));
  }
}
