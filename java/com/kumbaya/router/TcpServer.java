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

  @Override
  public void run() {
    while (running.get()) {
      try {
        logger.info("Accepting new connections");
        final Socket connection = socket.accept();
        executor.execute(new RequestHandler(connection, handlers));
      } catch (SocketException e) {
        Preconditions.checkArgument(!running.get(), "Socket closed but server is still running");
      } catch (IOException e) {
        e.printStackTrace();
        logger.error("Unexpected IOException: ", e);
      }
    }
  }

  private static class RequestHandler implements Runnable {
    private final Socket connection;
    private final Map<Class<?>, Handler<?>> handlers;

    RequestHandler(Socket connection, Map<Class<?>, Handler<?>> handlers) {
      this.connection = connection;
      this.handlers = handlers;
    }

    @SuppressWarnings({"unchecked", "cast"})
    private <I> void handle(Handler<I> handler, Object request, OutputStream response) throws IOException {
      handler.handle((I) request, new Queue(response));
    }

    @Override
    public void run() {

      try {
        OutputStream stream = connection.getOutputStream();
        logger.info("Starting a connection");
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
      } catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
        e.printStackTrace();
        logger.error("Unexpected Serialization error: ", e);
      } catch (ConnectException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
        logger.error("Unexpected IOException: ", e);
      } finally {
        logger.info("Ended a connection");   
        try {
          connection.close();
        } catch (IOException e) {
          e.printStackTrace();
          logger.error("Unexpected error closing the connection", e);
        }
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
