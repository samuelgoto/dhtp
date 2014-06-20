package com.kumbaya.dht;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;
import java.net.SocketAddress;

import org.limewire.mojito.Context;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.io.Tag;
import org.limewire.mojito.messages.DHTMessage;
import org.limewire.security.SecureMessage;
import org.limewire.security.SecureMessageCallback;

@Singleton
class HttpMessageDispatcher extends MessageDispatcher {
	private boolean isBound = false;
	private boolean started = false;
	private final Dispatcher dispatcher;

	@Inject
	public HttpMessageDispatcher(Context context, Dispatcher dispatcher) {
		super(context);
		this.dispatcher = dispatcher;
	}

	@Override
	public void handleMessage(DHTMessage message) {
		super.handleMessage(message);
	}

	@Override
	public void bind(SocketAddress address) throws IOException {
		dispatcher.bind(address);
		isBound = true;
	}

	@Override
	public boolean isBound() {
		return isBound;
	}

	@Override
	public boolean isRunning() {
		return started;
	}

	@Override
	protected boolean submit(Tag tag) {
		register(tag);
		return dispatcher.submit(tag);
	}

	@Override
	public void start() {
		super.start();
		started = true;
	}

	@Override
	protected void process(Runnable runnable) {
		runnable.run();
	}

	@Override
	protected void verify(SecureMessage secureMessage,
			SecureMessageCallback smc) {
		dispatcher.verify(secureMessage, smc);
	}
}
