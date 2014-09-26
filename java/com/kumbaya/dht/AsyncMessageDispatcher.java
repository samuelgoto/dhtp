package com.kumbaya.dht;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.io.MessageDispatcherImpl;
import org.limewire.mojito.io.Tag;
import org.limewire.mojito.messages.DHTMessage;
import org.limewire.security.SecureMessage;
import org.limewire.security.SecureMessageCallback;

@Singleton
public class AsyncMessageDispatcher extends MessageDispatcher {
    private static final Log logger = LogFactory.getLog(AsyncMessageDispatcher.class);

	private boolean isBound = false;
	private boolean started = false;
	private final Server dispatcher;
	private final HttpMessageDispatcher sender;
	private final Thread thread;
	private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();

	@Inject
	public AsyncMessageDispatcher(
			Context context,
			Server dispatcher,
			HttpMessageDispatcher sender) {
		super(context);
		this.dispatcher = dispatcher;
		this.sender = sender;
		thread = context.getDHTExecutorService().getThreadFactory().newThread(
				new Runnable() {
					@Override
					public void run() {
						do {
							try {
								Runnable task = queue.take();
								task.run();
							} catch (InterruptedException e) {
								logger.error(e);
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
				boolean result = sender.send(tag);
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
		throw new UnsupportedOperationException(
				"Dispatcher doesn't support verifying secure messages");
	}

    @Override
    public void close() {
        super.close();
        dispatcher.close();
    }
}
