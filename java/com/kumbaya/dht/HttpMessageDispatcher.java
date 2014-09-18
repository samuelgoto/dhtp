package com.kumbaya.dht;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

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
	private final JettyMessageDispatcher dispatcher;
	private final Thread thread;
	private final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(10);

	@Inject
	public HttpMessageDispatcher(Context context, JettyMessageDispatcher dispatcher) {
		super(context);
		this.dispatcher = dispatcher;
		thread = context.getDHTExecutorService().getThreadFactory().newThread(
				new Runnable() {
					@Override
					public void run() {
						do {
							try {
								Runnable task = queue.take();
								task.run();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						} while (true);
					}
				});
		thread.setName(context.getName() + "-MessageDispatcherThread");
		thread.setDaemon(Boolean.getBoolean("com.limegroup.mojito.io.MessageDispatcherIsDaemon"));
	}

	@Override
	public void handleMessage(final DHTMessage message) {
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
	protected boolean submit(final Tag tag) {
		queue.add(new Runnable() {
			@Override
			public void run() {
				boolean result = dispatcher.send(tag);
				if (result) {
					register(tag);
				}
			}
		});
		return true;
	}

	@Override
	public void start() {
		super.start();
		thread.start();
		started = true;
	}

	@Override
	protected void process(Runnable runnable) {
		queue.add(runnable);
	}

	@Override
	protected void verify(SecureMessage secureMessage,
			SecureMessageCallback smc) {
		dispatcher.verify(secureMessage, smc);
	}
}
