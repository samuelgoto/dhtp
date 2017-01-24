package com.kumbaya.router;

import com.google.common.base.Optional;
import com.kumbaya.common.InetSocketAddresses;
import com.kumbaya.router.Kumbaya;
import com.kumbaya.router.Packets.Data;
import com.kumbaya.router.Packets.Interest;
import com.kumbaya.router.TcpServer.Interface;
import com.kumbaya.router.TcpServer.RequestHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Executors;

import junit.framework.TestCase;

public class TcpServerTest extends TestCase {
  public void testStartingStopping() throws Exception {
    TcpServer server = new TcpServer(Executors.newFixedThreadPool(10), new RequestHandler.Builder() {
      @Override
      public RequestHandler build(Socket connection) {
        throw new UnsupportedOperationException("no handlers registered");
      }
    });
    server.bind(new InetSocketAddress("localhost", 8080));
    server.close();
  }

  public void testPacket() throws IOException, IllegalArgumentException, IllegalAccessException, InstantiationException {
    TcpServer server = new TcpServer(Executors.newFixedThreadPool(10), 
        RequestHandler.of(Interest.class, new TcpServer.Handler<Interest>() {
          @Override
          public void handle(Interest request, Interface response) throws IOException {
            Data result = new Data();
            result.setName(request.getName());
            result.getMetadata().setFreshnessPeriod(2);
            result.getMetadata().setContentType("text/html");
            result.setContent("hello world".getBytes());
            response.push(result);
          }
        }));

    server.bind(new InetSocketAddress("localhost", 8081));

    Kumbaya client = new Kumbaya();

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
    RequestHandler.Builder handler = RequestHandler.of(Interest.class, new TcpServer.Handler<Interest>() {
      @Override
      public void handle(Interest request, Interface response) throws IOException {
        Data result = new Data();
        result.setName(request.getName());
        result.getMetadata().setFreshnessPeriod(2);
        result.getMetadata().setContentType("text/html");
        byte[] content = new byte[10 * 1000 * 1000];
        result.setContent(content);
        try {
          response.push(result);
        } catch (SocketException e) {
          e.printStackTrace();
        }
      }
    });
    TcpServer server = new TcpServer(Executors.newFixedThreadPool(10), handler);
    server.bind(new InetSocketAddress("localhost", 8080));

    Kumbaya client = new Kumbaya();

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

