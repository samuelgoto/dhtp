package com.kumbaya.www.gateway;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.kumbaya.common.Flags;
import com.kumbaya.common.Flags.Flag;
import com.kumbaya.common.InetSocketAddresses;
import com.kumbaya.common.Server;
import com.kumbaya.dht.Dht;
import com.kumbaya.dht.DhtModule;
import com.kumbaya.router.Packets;
import com.kumbaya.router.Packets.Interest;
import com.kumbaya.router.TcpServer;
import com.kumbaya.router.TcpServer.Handler;
import com.kumbaya.router.TcpServer.Interface;
import com.kumbaya.www.WorldWideWeb;
import com.kumbaya.www.WorldWideWeb.Resource;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Gateway implements Server {
  private static final Log logger = LogFactory.getLog(Gateway.class);

  private @Inject @Flag("port") int port;
  private @Inject @Flag("bootstrap") String bootstrap;
  private @Inject(optional = true) @Flag("domains") String domains =
      "sgo.to@192.30.252.153,1500wordmtu.com,johnpanzer.com";

  @Inject
  private TcpServer server;
  @Inject
  private Dht dht;

  public static class Module extends AbstractModule {
    @Override
    protected void configure() {
      ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("Gateway-%d").build();
      bind(ExecutorService.class).toInstance(Executors.newFixedThreadPool(10, factory));

      install(new TcpServer.HandlerModule() {
        @Override
        protected void register() {
          addHandler(Interest.class, InterestHandler.class);
        }
      });

      install(new DhtModule());
    }
  }


  private static class InterestHandler implements Handler<Interest> {
    private @Inject(optional = true) @Flag("domains") String domains;

    @Override
    public void handle(Interest request, Interface response) throws IOException {
      // Fetches the content of the page.
      logger.info("Got a request to fetch " + request.getName().getName());
      try {

        String url = request.getName().getName();
        String domain = new URL(url).getAuthority();

        Optional<InetAddress> host = Optional.absent();
        for (String config : domains.split(",")) {
          String[] keyValue = config.split("@");
          String key = keyValue[0];
          if (domain.equals(key) && keyValue.length == 2) {
            host = Optional.of(InetAddress.getByName(keyValue[1]));
            break;
          }
        }

        Optional<Resource> content =
            host.isPresent() ? WorldWideWeb.get(host.get(), url) : WorldWideWeb.get(url);

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
        throw new RuntimeException(
            "Programming error: application protocol busted (client closed the stream before the server was done writing)");
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
  public void start() throws IOException {
    logger.info("Binding into " + port);
    Packets.register();
    server.start();
    dht.start();

    InetSocketAddress router = InetSocketAddresses.parse(bootstrap);
    try {
      // Bootstraps.
      dht.bootstrap(router.getHostName(), router.getPort()).get();

      // Announces that we can serve sgo.to.
      for (String config : domains.split(",")) {
        String[] keyValue = config.split("@");
        String domain = keyValue[0];
        dht.put(domain, "*");
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new IOException("Failed to bootstrap", e);
    }

  }

  @Override
  public void stop() throws IOException {
    server.stop();
    dht.stop();
  }


  public static void main(String[] args) throws Exception {
    logger.info("Running the Kumbaya Gateway");

    Gateway gateway =
        Guice.createInjector(new Module(), Flags.asModule(args)).getInstance(Gateway.class);
    gateway.start();
  }
}
