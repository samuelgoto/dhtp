package com.kumbaya.www.gateway;

import com.google.inject.Inject;
import com.kumbaya.common.Server;
import com.kumbaya.router.TcpServer;
import com.kumbaya.router.Packets;
import com.kumbaya.router.Packets.Interest;
import com.kumbaya.router.TcpServer.Handler;
import com.kumbaya.router.TcpServer.Queue;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

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
    
    private String get(String url) throws MalformedURLException, IOException {
      HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
      Scanner scanner = new Scanner(connection.getInputStream());
      scanner.useDelimiter("\\Z");
      String result = scanner.next();
      scanner.close();
      return result;
    }
    
    @Override
    public void handle(Interest request, Queue response) throws IOException {
      Packets.Data data = new Packets.Data();
      data.getName().setName(request.getName().getName());
      data.getMetadata().setFreshnessPeriod(2);
      data.setContent(get(request.getName().getName()).getBytes());
      response.push(data);
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
