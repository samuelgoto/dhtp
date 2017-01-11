package com.kumbaya.router;

import com.google.common.base.Optional;
import com.kumbaya.router.Client;
import com.kumbaya.router.Packets.Data;
import com.kumbaya.router.Packets.Interest;
import com.kumbaya.router.TcpServer.Queue;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import junit.framework.TestCase;

public class TcpServerTest extends TestCase {

  public void testPacket() throws IOException, IllegalArgumentException, IllegalAccessException, InstantiationException {
    TcpServer server = new TcpServer(Executors.newFixedThreadPool(1));
    server.bind(new InetSocketAddress("localhost", 6789));
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
    Thread thread = new Thread(server);
    thread.start();

    Client client = new Client(new InetSocketAddress("localhost", 6789));

    Interest interest = new Interest();
    interest.getName().setName("foo");
    Optional<Data> result = client.send(interest);

    assertTrue(result.isPresent());
    assertEquals("foo", result.get().getName().getName());
    assertEquals(2, result.get().getMetadata().getFreshnessPeriod());
    assertEquals("hello world", new String(result.get().getContent()));
  }
}

