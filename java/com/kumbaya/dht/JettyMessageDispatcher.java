package com.kumbaya.dht;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import com.kumbaya.dht.Dht.Model;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.limewire.mojito.Context;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.io.Tag;
import org.limewire.mojito.messages.DHTMessage;
import org.limewire.security.SecureMessage;
import org.limewire.security.SecureMessageCallback;

@Singleton
class JettyMessageDispatcher {
  private final HttpClient client = new HttpClient();
  private Server server;
  private final Context context;
  private final Map<String, HttpServlet> servlets;

  @Inject
  JettyMessageDispatcher(Context context, Map<String, HttpServlet> servlets) {
    this.context = context;
    this.servlets = servlets;
    client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
  }
  
  static class IndexHandler extends HttpServlet {
    private static final Log log = LogFactory.getLog(IndexHandler.class);

    private static final long serialVersionUID = 1L;
    private final Context context;
    private final Dht dht;

    @Inject
    IndexHandler(Context context, Dht dht) {
      this.context = context;
      this.dht = dht;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {

      log.info("Storing a value");

      PrintWriter writer = response.getWriter();

      String key = request.getParameter("key");
      String value = request.getParameter("value");

      try {
        dht.put(Keys.of(key), Values.of(value));
        response.sendRedirect("/" + key);
      } catch (InterruptedException e) {
        writer.write(e.getMessage());
        return;
      } catch (ExecutionException e) {
        writer.write(e.getMessage());
        return;
      }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {


      PrintWriter writer = response.getWriter();
      writer.write("<html>");
      writer.write("<body>");
      String path = request.getRequestURI();
      if (!"/".equals(path) && !"/favicon.ico".equals(path)) {
        writer.write("<pre>");
        try {
          log.info("Getting a value");
          EntityKey key = EntityKey.createEntityKey(
              Keys.of(path.substring(1)), DHTValueType.TEXT);
          List<DHTValueEntity> result = dht.get(key, 200);
          writer.write(result.toString());
          log.info("Done");
        } catch (InterruptedException e) {
          writer.write(e.toString());
          log.error(e);
        } catch (ExecutionException e) {
          writer.write(e.toString());
          log.error(e);
        } catch (TimeoutException e) {
          writer.write(e.toString());
          log.error(e);
        }
        writer.write("</pre>");
      }
      writer.write("<pre>");
      writer.write("Local node:\n\n");
      writer.write(context.toString());
      writer.write("\n");
      writer.write("Routing table:\n\n");
      writer.write(context.getRouteTable().toString());
      writer.write("\n");
      writer.write("Database:\n\n");
      writer.write(context.getDatabase().toString());
      writer.write("</pre>");
      writer.write("<br>");
      writer.write("<form method='post'>");
      writer.write("  key: <input type='text' name='key'>");
      writer.write("  value: <input type='text' name='value'>");
      writer.write("  <input type='submit' value='create'>");
      writer.write("</form>");
      writer.write("</body>");
      writer.write("</html>");

      response.flushBuffer();
    }
  }

  static class ThreadZHandler extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {

      PrintWriter writer = response.getWriter();
      writer.write("<html>");
      writer.write("<head>");
      writer.write("<title>Threads</title>");
      writer.write("</head>");
      writer.write("<body>");
      writer.write("<h1>Threads</h1>");
      
      Set<Thread> threads = Thread.getAllStackTraces().keySet();
      for (Thread thread : threads) {
        writer.write("<div style='margin-top: 30px;'>");
        writer.write("<p><b>" + thread.getName() + ": " + thread.getState() + "</b></p>");        
        for (StackTraceElement stackTrace : thread.getStackTrace()) {
          writer.write("<p style='margin-left: 50px;'>" + stackTrace.toString() + "</p>");
        }
        writer.write("</div>");
      }

      writer.write("</body>");
      writer.write("</html>");

      response.flushBuffer();
    }
  }

  
  static class DhtHandler extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Context context;
    private Provider<HttpMessageDispatcher> dispatcher;

    @Inject
    DhtHandler(Context context, Provider<HttpMessageDispatcher> dispatcher) {
      this.context = context;
      this.dispatcher = dispatcher;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
      response.getWriter().write("Welcome to the DHT entrypoint!");
      response.flushBuffer();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
      int length = request.getContentLength();
      byte[] data = new byte[length];
      DataInputStream dataIs = new DataInputStream(
          request.getInputStream());
      dataIs.readFully(data);

      String hostname = request.getHeader("X-Node-Host");
      String port = request.getHeader("X-Node-Port");

      InetSocketAddress src = InetSocketAddress.createUnresolved(
          hostname, Integer.valueOf(port));

      try {
        DHTMessage destination = context.getMessageFactory()
            .createMessage(src, ByteBuffer.wrap(data));				
        dispatcher.get().handleMessage(destination);
      } catch (Exception e) {
        System.out.println(e);
      }

    }
  }

  public void bind(SocketAddress address) throws IOException {
    server = new Server(((InetSocketAddress) address)
        .getPort());

    ServletContextHandler servlet = new ServletContextHandler(ServletContextHandler.SESSIONS);
    servlet.setContextPath("/");

    for (Map.Entry<String, HttpServlet> entry : servlets.entrySet()) {
      servlet.addServlet(new ServletHolder(entry.getValue()), entry.getKey());
    }

    server.setHandler(servlet);

    try {
      server.start();
      client.start();
    } catch (InterruptedException e) {
      throw new IOException(e);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  public boolean send(final Tag tag) {
    HttpExchange request = new HttpExchange() {
      @Override
      protected void onConnectionFailed(Throwable x) {
        tag.handleError(new IOException(x));
      }

      @Override
      protected void onException(Throwable x) {
        tag.handleError(new IOException(x));
      }

      @Override
      protected void onExpire() {
        tag.handleError(new IOException("Request expired"));
      }
    };

    // Optionally set the HTTP method
    request.setMethod("POST");

    InetSocketAddress ip = (InetSocketAddress) tag.getSocketAddress();

    try {
      URL url = new URL("http", ip.getHostName(), ip.getPort(),
          "/.well-known/dht");
      request.setURL(url.toString());
    } catch (MalformedURLException e1) {
      throw new RuntimeException("Invalid external address.", e1);
    }

    // TODO(goto): figure out what's the best way to do this.
    request.addRequestHeader("X-Node-Host",
        ((InetSocketAddress) context.getContactAddress()).getHostName());
    request.addRequestHeader("X-Node-Port",
        String.valueOf(((InetSocketAddress) context.getContactAddress()).getPort()));

    try {
      DHTMessage message = tag.getMessage();
      ByteBuffer data = context.getMessageFactory()
          .writeMessage(ip, message);

      request.setRequestContent(new ByteArrayBuffer(data.array()));

      client.send(request);
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }

  public void verify(SecureMessage secureMessage,
      SecureMessageCallback smc) {
    throw new UnsupportedOperationException(
        "Dispatcher doesn't support verifying secure messages");
  }
  
  void close() {
	  try {
		server.stop();
		server.destroy();
	} catch (Exception e) {
		throw new RuntimeException("Error stopping http server", e);
	}
  }
}
