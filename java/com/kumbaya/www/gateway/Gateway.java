package com.kumbaya.www.gateway;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.kumbaya.common.Server;
import com.kumbaya.router.TcpServer;
import com.kumbaya.router.Packets;
import com.kumbaya.router.Packets.Interest;
import com.kumbaya.router.TcpServer.Handler;
import com.kumbaya.router.TcpServer.Queue;
import com.kumbaya.www.WorldWideWeb;
import java.io.IOException;
import java.net.InetSocketAddress;

public class Gateway implements Server {
  private final TcpServer server;
  @Inject private InterestHandler handler;
  
  @Inject
  Gateway(TcpServer server) {
    this.server = server;
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
    server.register(Interest.class, handler);
    server.bind(address);
  }

  @Override
  public void close() throws IOException {
    server.close();
  }
}
