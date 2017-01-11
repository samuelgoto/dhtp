package com.kumbaya.router;

import com.google.common.base.Optional;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.kumbaya.common.Server;
import com.kumbaya.router.Packets.Data;
import com.kumbaya.router.Packets.Interest;
import com.kumbaya.router.TcpServer.Handler;
import com.kumbaya.router.TcpServer.Queue;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
}
