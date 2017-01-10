package com.kumbaya.www.gateway;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.kumbaya.common.Server;
import com.kumbaya.router.TcpServer;
import com.kumbaya.router.Packets;
import com.kumbaya.router.Packets.Interest;
import com.kumbaya.router.TcpServer.Handler;
import java.io.IOException;
import java.net.InetSocketAddress;

public class Gateway implements Server {
  private final TcpServer server;
  @Inject private InterestHandler handler;
  private final Thread thread;
  
  @Inject
  Gateway(TcpServer server) {
    this.server = server;
    this.thread = new Thread(server);
  }
  
  private static class InterestHandler implements Handler<Interest> {
    @Inject
    InterestHandler() {
    }
    
    @Override
    public Optional<Object> handle(Interest request) {
      // TODO(goto): implement the network <-> web interface
      Packets.Data data = new Packets.Data();
      data.getName().setName(request.getName().getName());
      data.setContent("hello world".getBytes());
      data.getMetadata().setFreshnessPeriod(2);
      
      return Optional.of(data);
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
}
