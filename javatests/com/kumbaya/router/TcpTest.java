package com.kumbaya.router;

import com.google.common.base.Optional;
import com.kumbaya.router.Packets.Data;
import com.kumbaya.router.Packets.Interest;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;

public class TcpTest extends TestCase {

  private static class TcpServer implements Runnable {
    private final Map<Class<?>, Handler<?>> handlers = new HashMap<Class<?>, Handler<?>>();
    private final ServerSocket welcomeSocket;

    TcpServer(int port) throws IOException {
      this.welcomeSocket = new ServerSocket(port);
    }
    
    <I> void register(Class<I> clazz, Handler<I> handler) {
      handlers.put(clazz, handler);
    }

    interface Handler<I> {
      Optional<Object> handle(I request);
    }
    
    @SuppressWarnings({"unchecked", "cast"})
    private <I> Optional<Object> handle(Handler<I> handler, Object request) {
      return handler.handle((I) request);
    }
    
    @Override
    public void run() {
      while(true) {
        try {
          Socket connectionSocket = welcomeSocket.accept();
          DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
          Object request = Serializer.unserialize(connectionSocket.getInputStream());
          Handler<?> handler = handlers.get(request.getClass());
          if (handler == null) {
            // Unknown packet type.
            throw new RuntimeException("Unexpected packet type " + request.getClass());
          }
          Optional<Object> response = handle(handler, request);
          if (response.isPresent()) {
            Serializer.serialize(outToClient, response.get());
          }
        } catch (IOException | IllegalArgumentException | IllegalAccessException | InstantiationException e) {
          throw new RuntimeException("Unexpected error", e);
        }
      }
    }
  }

  private static class TcpClient {
    <T> T send(Object packet) throws UnknownHostException, IOException, IllegalArgumentException, IllegalAccessException, InstantiationException {
      Socket clientSocket = new Socket("localhost", 6789);
      DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
      Serializer.serialize(outToServer, packet);
      T result = Serializer.unserialize(clientSocket.getInputStream());
      clientSocket.close();
      return result;
    }
  }

  public void testPacket() throws IOException, IllegalArgumentException, IllegalAccessException, InstantiationException {
    TcpServer server = new TcpServer(6789);
    server.register(Interest.class, new TcpServer.Handler<Interest>() {
      @Override
      public Optional<Object> handle(Interest request) {
        Data response = new Data();
        response.name = request.name;
        response.metadata.freshnessPeriod = 2;
        response.content = "hello world".getBytes();
        return Optional.of(response);
      }
    });
    Thread thread = new Thread(server);
    thread.start();

    TcpClient client = new TcpClient();

    Interest interest = new Interest();
    interest.name.name = "foo";
    interest.name.sha256 = "bar";
    Data result = client.send(interest);

    assertEquals("foo", result.name.name);
    assertEquals(2, result.metadata.freshnessPeriod);
    assertEquals("hello world", new String(result.content));
  }
}

