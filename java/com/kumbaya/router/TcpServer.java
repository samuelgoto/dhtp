package com.kumbaya.router;

import com.google.common.base.Optional;
import com.kumbaya.common.Server;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

class TcpServer implements Runnable, Server {
  private final Map<Class<?>, Handler<?>> handlers = new HashMap<Class<?>, Handler<?>>();
  private ServerSocket welcomeSocket;

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

  @Override
  public void bind(InetSocketAddress address) throws IOException {
    this.welcomeSocket = new ServerSocket(address.getPort());
  }

  @Override
  public void close() {
    // TODO(goto): Auto-generated method stub
    
  }
}
