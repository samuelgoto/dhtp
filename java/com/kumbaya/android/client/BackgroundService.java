package com.kumbaya.android.client;

import static com.kumbaya.android.client.CommonUtilities.SENDER_ID;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.messages.DHTMessage;

import com.google.android.gcm.GCMRegistrar;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.kumbaya.dht.AsyncMessageDispatcher;
import com.kumbaya.dht.Dht;
import com.kumbaya.dht.Server;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class BackgroundService extends Service {
    private static final String TAG = "BackgroundService";
	// NOTE(goto): you can set this to localhost while running appengine
	// locally.
	final String hostname = "kumbaya-android.appspot.com";
	int port = CommonUtilities.GCM_PORT;
	int proxy = CommonUtilities.GCM_PORT;
	private final Context context = this;
	private final Injector injector = Guice.createInjector(
			new AbstractModule() {
				@Override
				protected void configure() {
					bind(MessageDispatcher.class).to(AsyncMessageDispatcher.class);
					bind(Context.class).toInstance(context);
					bind(Server.class).to(GcmServer.class);

					bind(org.limewire.mojito.Context.class).toInstance(
							(org.limewire.mojito.Context) MojitoFactory.createDHT(hostname));
				}
			});

	@Inject private Dht dht = null;
	@Inject private AsyncMessageDispatcher dispatcher = null;
	@Inject private org.limewire.mojito.Context messageFactory = null;
	private final Binder mBinder = new LocalBinder();

	public BackgroundService() {
        // NOTE(goto): this doesn't smell right, but android is somehow
        // calling this constructor with an existing instance attached
        // to it, where private properties are already set. weird.
        Log.i(TAG, "Hash code: " + this.hashCode());
        if (this.dht != null) {
            Log.i(TAG, "Service already created. Bootstraped? " + dht.isBootstraped());
        	return;
        }
        Log.i(TAG, "Injecting members.");
        injector.injectMembers(this);
	}
	
	public class LocalBinder extends Binder {
		BackgroundService getService() {
			// Return this instance of LocalService so clients can call public methods
			return BackgroundService.this;
		}
	}

	@Override
	public void onCreate() {
        Log.i(TAG, "Creating the background service.");
	}

	static class GcmServer implements Server {
		@Inject private Context context;
		@Inject private Provider<org.limewire.mojito.Context> node;

		@Override
		public void bind(SocketAddress address) throws IOException {
			// Make sure the device has the proper dependencies.
			GCMRegistrar.checkDevice(context);
			// Make sure the manifest was properly set - comment out this line
			// while developing the app, then uncomment it when it's ready.
			GCMRegistrar.checkManifest(context);

			KUID nodeId = node.get().getLocalNodeID();
			ServerUtilities.id = Optional.of(nodeId);

			final String regId = GCMRegistrar.getRegistrationId(context);
			// GCMRegistrar.unregister(context);
			if (regId.equals("")) {
		        Log.i(TAG, "Device is not registered locally. Registering.");
				// Automatically registers application on startup.
				GCMRegistrar.register(context, SENDER_ID);
			} else {
		        Log.i(TAG, "Device is already registered. Registering on the server.");
				ServerUtilities.register(context, regId);
			}
			
			while (!GCMRegistrar.isRegisteredOnServer(context)) {
				try {
					// Blocks until we are not registered on the server.
			        Log.i(TAG, "Device is not registered on the server yet. Sleeping.");
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					throw new IOException("Failed to bind to GCM", e);
				}
			}
		}

		@Override
		public void close() {
			GCMRegistrar.onDestroy(context);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Starting the service: " + (dht.isBootstraped() ? "bootstraped" : "not bootstraped"));
		
		// If we get killed, after returning from here, restart
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	interface Runnable<K> {
		K run() throws Exception;
	}

	private <K> ListenableFuture<K> run(final Runnable<K> runnable) {
		final SettableFuture<K> future = SettableFuture.create();
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					K result = runnable.run();
					future.set(result);
				} catch (Exception e) {
					future.setException(e);
				}
				return null;
			}
		}.execute();
		return future;
	}
	
	public ListenableFuture<Boolean> bootstrap() {
		return run(new Runnable<Boolean>() {
			@Override
			public Boolean run() throws ExecutionException {
		        Log.i(TAG, "Starting DHT.");


				try {

					if (dht.isBound()) {
				        Log.i(TAG, "Already bound..");
					} else {
				        Log.i(TAG, "Binding to port.");
				        dht.start(hostname, port, proxy);
					}

					if (dht.isBootstraped()) {
				        Log.i(TAG, "Already bootstraped.");
					} else {
				        Log.i(TAG, "Bootstraping.");
						dht.bootstrap("kumbaya-node0.herokuapp.com", 80).get();
				        Log.i(TAG, "Done bootstraping.");
					}

					return true;
				} catch (IOException e) {
			        Log.w(TAG, "IOException.", e);
					throw new ExecutionException("", e);
				} catch (InterruptedException e) {
			        Log.w(TAG, "InterruptedException.", e);
					throw new ExecutionException("", e);
				} catch (ExecutionException e) {
			        Log.w(TAG, "ExecutionException.", e);
					throw new ExecutionException("", e);
				}
			}
		});
	}

	public ListenableFuture<Void> put(final String key, final String value) {
		return run(new Runnable<Void>() {
			@Override
			public Void run() throws Exception {
				dht.put(key, value);
				return null;
			}
		});
	}
	
	public ListenableFuture<List<String>> get(final String key, final int timeoutMs) {
		return run(new Runnable<List<String>>() {
			@Override
			public List<String> run() throws Exception {
				return Lists.transform(dht.get(key, timeoutMs), new Function<DHTValueEntity, String>() {
					public String apply(DHTValueEntity value) {
						return value.getValue().toString();
					}
				});
			}
		});
	}

	public String toString() {
		// Returns a debug string.
		return messageFactory.toString()
				+ "" 
				+ messageFactory.getRouteTable().toString()
				+ ""
				+ messageFactory.getDatabase().toString();
	}
	
	/**
	 * This method is available exclusively for the communication between the
	 * GCM intent receiver and the internal node.
	 */
	void handleMessage(InetSocketAddress src, byte[] data) {
		if (data == null || src == null || src.getPort() == 0) {
			return;
		}

		try {
			DHTMessage message = messageFactory.getMessageFactory()
					.createMessage(src, ByteBuffer.wrap(data));				

			Log.i("BackgroundService", "Got message: " + message.getOpCode() +
					", from: " + message.getContact().toString());

			dispatcher.handleMessage(message);
		} catch (Exception e) {
			// ignores silently.
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onDestroy() {
        Log.i(TAG, "Destroying the service.");
	}
}