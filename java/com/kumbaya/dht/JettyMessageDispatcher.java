package com.kumbaya.dht;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import com.kumbaya.monitor.VarZ;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Map;

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
import org.limewire.mojito.io.Tag;
import org.limewire.mojito.messages.DHTMessage;
import org.limewire.security.SecureMessage;
import org.limewire.security.SecureMessageCallback;

@Singleton
class JettyMessageDispatcher {
	private static final Log logger = LogFactory.getLog(JettyMessageDispatcher.class);
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
		@VarZ("/dht/messages/incoming")
		protected void doPost(HttpServletRequest request, HttpServletResponse response)
				throws IOException, ServletException {
			int length = request.getContentLength();
			byte[] data = new byte[length];
			DataInputStream dataIs = new DataInputStream(
					request.getInputStream());
			dataIs.readFully(data);
			dataIs.close();

			String hostname = request.getHeader("X-Node-Host");
			String port = request.getHeader("X-Node-Port");

			InetSocketAddress src = InetSocketAddress.createUnresolved(
					hostname, Integer.valueOf(port));

			try {
				DHTMessage destination = context.getMessageFactory()
						.createMessage(src, ByteBuffer.wrap(data));				
				dispatcher.get().handleMessage(destination);
			} catch (Exception e) {
				logger.error(e);
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

	@VarZ("/dht/messages/outgoing")
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
