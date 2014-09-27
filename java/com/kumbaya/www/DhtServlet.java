package com.kumbaya.www;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.messages.DHTMessage;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.kumbaya.dht.AsyncMessageDispatcher;
import com.kumbaya.annotations.VarZ;
import com.kumbaya.monitor.VarZLogger;

class DhtServlet extends HttpServlet {
	private static final Log logger = LogFactory.getLog(DhtServlet.class);
	private static final long serialVersionUID = 1L;
	private final Context context;
	private Provider<AsyncMessageDispatcher> dispatcher;
	private final VarZLogger varZ;

	@Inject
	DhtServlet(
			Context context,
			Provider<AsyncMessageDispatcher> dispatcher,
			VarZLogger varZ) {
		this.context = context;
		this.dispatcher = dispatcher;
		this.varZ = varZ;
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

			varZ.log("/dht/messages/incoming/" +
			    destination.getOpCode().name().toLowerCase());

			dispatcher.get().handleMessage(destination);
		} catch (Exception e) {
			logger.error(e);
		}
	}
}

