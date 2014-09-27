package com.kumbaya.dht;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.limewire.mojito.Context;
import org.limewire.mojito.io.Tag;
import org.limewire.mojito.messages.DHTMessage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kumbaya.annotations.VarZ;
import com.kumbaya.monitor.VarZLogger;

@Singleton
public class HttpMessageDispatcher {
	private final HttpClient client;
	private final Context context;
	private final VarZLogger varZ;

	@Inject
	HttpMessageDispatcher(Context context, VarZLogger varZ, HttpClient client) throws Exception {
		this.context = context;
		this.client = client;
		this.varZ = varZ;
		this.client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
		this.client.start();
	}

	@VarZ("/dht/messages/outgoing")
	public boolean send(final Tag tag) {
		varZ.log("/dht/messages/outgoing/" + tag.getMessage().getOpCode()
				.name().toLowerCase());
		
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

		// tag.getNodeID()
		
		try {
			URL url = new URL("http", ip.getHostName(), ip.getPort(),
					"/.well-known/dht");
			request.setURL(url.toString());
		} catch (MalformedURLException e1) {
			throw new RuntimeException("Invalid external address.", e1);
		}

		// TODO(goto): figure out what's the best way to do this.
		request.addRequestHeader("X-Node-Debug", tag.getMessage().getOpCode().toString());
		request.addRequestHeader("X-Node-Host",
				((InetSocketAddress) context.getContactAddress()).getHostName());
		request.addRequestHeader("X-Node-Port",
				String.valueOf(((InetSocketAddress) context.getContactAddress()).getPort()));
		request.addRequestHeader("X-Node-Destination",
				tag.getNodeID() != null ? tag.getNodeID().toString() : "");

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
}
