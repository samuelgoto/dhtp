package com.kumbaya.www;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import com.kumbaya.dht.AsyncMessageDispatcher;
import com.kumbaya.monitor.VarZLogger;

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
public class JettyServer implements com.kumbaya.dht.Server {
	private static final Log logger = LogFactory.getLog(JettyServer.class);
	private Server server;
	private final Map<String, HttpServlet> servlets;

	@Inject
	JettyServer(
			Context context,
			Map<String, HttpServlet> servlets,
			VarZLogger varZ) {
		this.servlets = servlets;
	}

	@Override
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
		} catch (InterruptedException e) {
			throw new IOException(e);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public void close() {
		try {
			server.stop();
			server.destroy();
		} catch (Exception e) {
			throw new RuntimeException("Error stopping http server", e);
		}
	}
}
