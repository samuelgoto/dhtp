package com.kumbaya.router;

import com.google.common.base.Optional;
import com.kumbaya.common.InetSocketAddresses;
import com.kumbaya.router.Client;
import com.kumbaya.router.Packets.Data;
import com.kumbaya.router.Packets.Interest;
import com.kumbaya.router.TcpServer.Queue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.Executors;

import junit.framework.TestCase;

public class TcpServerTest extends TestCase {
  public void testStartingStopping() throws Exception {
    TcpServer server = new TcpServer(Executors.newFixedThreadPool(10));
    server.bind(new InetSocketAddress("localhost", 8080));
    server.close();
  }
  
  public void testPacket() throws IOException, IllegalArgumentException, IllegalAccessException, InstantiationException {
    TcpServer server = new TcpServer(Executors.newFixedThreadPool(10));
    
    server.register(Interest.class, new TcpServer.Handler<Interest>() {
      @Override
      public void handle(Interest request, Queue response) throws IOException {
        Data result = new Data();
        result.setName(request.getName());
        result.getMetadata().setFreshnessPeriod(2);
        result.setContent("hello world".getBytes());
        response.push(result);
      }
    });
    
    server.bind(new InetSocketAddress("localhost", 8081));

    Client client = new Client();

    Interest interest = new Interest();
    interest.getName().setName("foo");
    Optional<Data> result = client.send(InetSocketAddresses.parse("localhost:8081"), interest);

    assertTrue(result.isPresent());
    assertEquals("foo", result.get().getName().getName());
    assertEquals(2, result.get().getMetadata().getFreshnessPeriod());
    assertEquals("hello world", new String(result.get().getContent()));
    
    server.close();
  }
  
  public void testLargePacket() throws IOException, IllegalArgumentException, IllegalAccessException, InstantiationException {
    TcpServer server = new TcpServer(Executors.newFixedThreadPool(10));
    server.register(Interest.class, new TcpServer.Handler<Interest>() {
      @Override
      public void handle(Interest request, Queue response) throws IOException {
        Data result = new Data();
        result.setName(request.getName());
        result.getMetadata().setFreshnessPeriod(2);
        byte[] content = new byte[10 * 1000 * 1000];
        result.setContent(content);
        try {
          response.push(result);
        } catch (SocketException e) {
          e.printStackTrace();
        }
      }
    });
    server.bind(new InetSocketAddress("localhost", 8080));

    Client client = new Client();

    Interest interest = new Interest();
    interest.getName().setName("foo");
    Optional<Data> result = client.send(InetSocketAddresses.parse("localhost:8080"), interest);

    assertTrue(result.isPresent());
    assertEquals("foo", result.get().getName().getName());
    assertEquals(2, result.get().getMetadata().getFreshnessPeriod());
    assertEquals(10  * 1000 * 1000, result.get().getContent().length);
    
    server.close();
  }
}

