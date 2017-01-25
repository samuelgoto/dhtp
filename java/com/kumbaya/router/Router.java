package com.kumbaya.router;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.kumbaya.common.Flags;
import com.kumbaya.common.Flags.Flag;
import com.kumbaya.common.Server;
import com.kumbaya.dht.Dht;
import com.kumbaya.dht.DhtModule;
import com.kumbaya.router.handlers.HandlersModule;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Router implements Server {
  private static final Log logger = LogFactory.getLog(Router.class);
  private final TcpServer server;
  private final Dht dht;

  @Inject
  Router(TcpServer server, Dht dht) {
    this.server = server;
    this.dht = dht;
  }

  public static class Module extends AbstractModule {
    @Override
    protected void configure() {
      ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("Router-%d").build();
      bind(ExecutorService.class).toInstance(Executors.newFixedThreadPool(10, factory));

      install(new HandlersModule());

      install(new DhtModule());
    }
  }

  @Override
  public void start() throws IOException {
    logger.info("Binding into port " + port);
    Packets.register();
    server.start();
    dht.start();
  }

  @Override
  public void stop() throws IOException {
    server.stop();
    dht.stop();
  }

  private @Inject @Flag("port") int port = 8082;

  public static void main(String[] args) throws Exception {
    logger.info("Running the Kumbaya Router");

    Router router =
        Guice.createInjector(new Module(), Flags.asModule(args)).getInstance(Router.class);
    router.start();
  }
}
