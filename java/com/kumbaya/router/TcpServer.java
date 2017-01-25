package com.kumbaya.router;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.util.Providers;
import com.kumbaya.common.Flags.Flag;
import com.kumbaya.common.Server;
import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Provider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TcpServer implements Runnable, Server {
  private static final Log logger = LogFactory.getLog(TcpServer.class);
  private final ExecutorService executor;
  private ServerSocket socket;
  private AtomicBoolean running = new AtomicBoolean(false);
  private final RequestHandler.Builder handler;

  @Inject
  @Flag("port")
  private int port = 8080;


  @Inject
  TcpServer(ExecutorService executor, RequestHandler.Builder handler) {
    this.executor = executor;
    this.handler = handler;
  }

  public interface Handler<I> {
    void handle(I request, Interface queue) throws IOException;
  }

  public static class Interface {
    private final Socket connection;

    Interface(Socket connection) {
      this.connection = connection;
    }

    public <T> Interface push(T object) throws IOException {
      // TODO(goto): do multiple calls to getOutputStream() return the same OutputStream?
      Serializer.serialize(connection.getOutputStream(), object);
      return this;
    }

    public <T> T pull() throws IOException {
      return Serializer.unserialize(connection.getInputStream());
    }

    public void close() throws IOException {
      connection.close();
    }
  }

  public static abstract class HandlerModule extends AbstractModule {

    @Override
    public void configure() {
      install(new FactoryModuleBuilder().build(RequestHandler.Builder.class));
      register();
    }

    protected abstract void register();

    protected <I> void addHandler(Class<I> clazz, Class<? extends Handler<I>> handler) {
      MapBinder<Class, Handler> binder =
          MapBinder.newMapBinder(binder(), Class.class, Handler.class);
      binder.addBinding(clazz).to(handler);
    }
  }

  @Override
  public void run() {
    while (running.get()) {
      try {
        logger.info("Accepting new connections");
        executor.execute(handler.build(socket.accept()));
      } catch (SocketException e) {
        Preconditions.checkArgument(!running.get(), "Socket closed but server is still running");
      } catch (IOException e) {
        e.printStackTrace();
        logger.error("Unexpected IOException: ", e);
      }
    }
  }

  static class RequestHandler implements Runnable {
    private final Interface connection;
    private final Map<Class, Provider<Handler>> handlers;

    @Inject
    RequestHandler(@Assisted Socket connection, Map<Class, Provider<Handler>> handlers) {
      this.connection = new Interface(connection);
      this.handlers = handlers;
    }

    interface Builder {
      RequestHandler build(Socket connection);
    }

    static <I> Builder of(Class<I> clazz, Handler<I> handler) {
      return new Builder() {
        @Override
        public RequestHandler build(Socket connection) {
          return new RequestHandler(connection, ImmutableMap.of(clazz, Providers.of(handler)));
        }
      };
    }

    @SuppressWarnings({"unchecked", "cast"})
    private <I> void handle(Handler<I> handler, Object request, Interface response)
        throws IOException {
      handler.handle((I) request, response);
    }

    @Override
    public void run() {
      try {
        logger.info("Starting a connection");

        Object request = connection.pull();

        Handler<?> handler = handlers.get(request.getClass()).get();
        if (handler == null) {
          // Unknown packet type.
          throw new RuntimeException("Unexpected packet type " + request.getClass());
        }
        handle(handler, request, connection);
      } catch (ConnectException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
        logger.error("Unexpected IOException: ", e);
      } catch (Exception e) {
        e.printStackTrace();
        logger.error("Got an unexpected exception: ", e);
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
  public void start() throws IOException {
    running.set(true);
    socket = new ServerSocket(port);
    executor.execute(this);
  }

  @Override
  public void stop() throws IOException {
    running.set(false);
    socket.close();
    executor.shutdown();
  }
}
