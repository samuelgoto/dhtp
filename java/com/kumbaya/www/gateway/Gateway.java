package com.kumbaya.www.gateway;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.kumbaya.common.Server;
import com.kumbaya.router.TcpServer;
import com.kumbaya.router.Packets;
import com.kumbaya.router.Serializer;
import com.kumbaya.router.Packets.Interest;
import com.kumbaya.router.TcpServer.Handler;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

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
    
    private String get(String url) throws MalformedURLException, IOException {
      HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
      Scanner scanner = new Scanner(connection.getInputStream());
      scanner.useDelimiter("\\Z");
      String result = scanner.next();
      scanner.close();
      return result;
    }
    
    @Override
    public void handle(Interest request, OutputStream response) throws IOException {
      Packets.Data data = new Packets.Data();
      data.getName().setName(request.getName().getName());
      data.getMetadata().setFreshnessPeriod(2);
      data.setContent(get(request.getName().getName()).getBytes());
      Serializer.serialize(response, data);
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
