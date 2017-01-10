package com.kumbaya.router;

import com.google.common.base.Optional;
import com.kumbaya.router.Client;
import com.kumbaya.router.Packets.Data;
import com.kumbaya.router.Packets.Interest;
import java.io.IOException;
import java.net.InetSocketAddress;
import junit.framework.TestCase;

public class TcpServerTest extends TestCase {

  public void testPacket() throws IOException, IllegalArgumentException, IllegalAccessException, InstantiationException {
    TcpServer server = new TcpServer();
    server.bind(new InetSocketAddress("localhost", 6789));
    server.register(Interest.class, new TcpServer.Handler<Interest>() {
      @Override
      public Optional<Object> handle(Interest request) {
        Data response = new Data();
        response.setName(request.getName());
        response.getMetadata().setFreshnessPeriod(2);
        response.setContent("hello world".getBytes());
        return Optional.of(response);
      }
    });
    Thread thread = new Thread(server);
    thread.start();

    Client client = new Client("localhost", 6789);

    Interest interest = new Interest();
    interest.getName().setName("foo");
    Optional<Data> result = client.send(interest);

    assertTrue(result.isPresent());
    assertEquals("foo", result.get().getName().getName());
    assertEquals(2, result.get().getMetadata().getFreshnessPeriod());
    assertEquals("hello world", new String(result.get().getContent()));
  }
}

