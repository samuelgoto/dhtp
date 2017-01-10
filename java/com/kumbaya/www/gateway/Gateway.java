package com.kumbaya.www.gateway;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.kumbaya.common.Server;
import com.kumbaya.router.TcpServer;
import com.kumbaya.router.Packets;
import com.kumbaya.router.Packets.Interest;
import com.kumbaya.router.TcpServer.Handler;
import java.io.FileNotFoundException;
import java.io.IOException;
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
    public Optional<Object> handle(Interest request) {
      Packets.Data data = new Packets.Data();
      data.getName().setName(request.getName().getName());
      data.getMetadata().setFreshnessPeriod(2);
      try {
        data.setContent(get(request.getName().getName()).getBytes());
        return Optional.of(data);
      } catch (FileNotFoundException e) {
        // We got a 404 from the server.
        // TODO(goto): there is probably a nack that we should send.
        return Optional.absent();
      } catch (IOException e) {
        return Optional.absent();
      }
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
