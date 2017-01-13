package com.kumbaya.router;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.kumbaya.common.Server;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TcpServer implements Runnable, Server {
  private static final Log logger = LogFactory.getLog(TcpServer.class);
  private final ExecutorService executor;
  private final Map<Class<?>, Handler<?>> handlers = new HashMap<Class<?>, Handler<?>>();
  private ServerSocket socket;
  private AtomicBoolean running = new AtomicBoolean(false);
  
  @Inject
  TcpServer(ExecutorService executor) {
    this.executor = executor;
  }

  public <I> void register(Class<I> clazz, Handler<I> handler) {
    handlers.put(clazz, handler);
  }

  public interface Handler<I> {
    void handle(I request, Queue queue) throws IOException;
  }
  
  public static class Queue {
    private final OutputStream out;
    Queue(OutputStream out) {
      this.out = out;
    }
    public <T> Queue push(T object) throws IOException {
      Serializer.serialize(out, object);
      return this;
    }
  }
  
  @SuppressWarnings({"unchecked", "cast"})
  private <I> void handle(Handler<I> handler, Object request, OutputStream response) throws IOException {
    handler.handle((I) request, new Queue(response));
  }
  
  @Override
  public void run() {
    while (running.get()) {
      try {
        Socket connection = socket.accept();
        logger.info("Starting a connection");
        OutputStream stream = connection.getOutputStream();
        DataOutputStream out = new DataOutputStream(stream);
        Object request = Serializer.unserialize(connection.getInputStream());
        Handler<?> handler = handlers.get(request.getClass());
        if (handler == null) {
          // Unknown packet type.
          throw new RuntimeException("Unexpected packet type " + request.getClass());
        }
        handle(handler, request, out);
        stream.close();
        connection.close();
        logger.info("Ended a connection");       
      } catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
        throw new RuntimeException("Failed to serialize payload", e);
      } catch (ConnectException e) {
        e.printStackTrace();
      } catch (SocketException e) {
        // e.printStackTrace();
        Preconditions.checkArgument(!running.get(), "Socket closed but server is still running");
      } catch (IOException e) {
        e.printStackTrace();
        logger.error("Unexpected IOException: ", e);
      }
    }
  }

  @Override
  public void bind(InetSocketAddress address) throws IOException {
    running.set(true);
    socket = new ServerSocket(address.getPort());
    executor.execute(this);
  }

  @Override
  public void close() throws IOException {
    running.set(false);
    socket.close();
    executor.shutdown();
  }
}
