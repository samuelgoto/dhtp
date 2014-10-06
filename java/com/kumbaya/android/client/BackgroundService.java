package com.kumbaya.android.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.messages.DHTMessage;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.kumbaya.dht.AsyncMessageDispatcher;
import com.kumbaya.dht.Dht;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

public class BackgroundService extends Service {
    private static final String TAG = "BackgroundService";
	// NOTE(goto): you can set this to localhost while running appengine
	// locally.
	private final String hostname = CommonUtilities.GCM_HOSTNAME;
	private final int port = CommonUtilities.GCM_PORT;
	private final int proxy = CommonUtilities.GCM_PORT;
	private final Injector injector = Guice.createInjector(
			new ClientModule(this));

	@Inject private Dht dht = null;
	@Inject private AsyncMessageDispatcher dispatcher = null;
	@Inject private org.limewire.mojito.Context context = null;
	private final Binder mBinder = new LocalBinder();

	public BackgroundService() {
		super();
        Log.i(TAG, "Hash code: " + this.hashCode());
        if (this.dht != null) {
            // NOTE(goto): this doesn't smell right, but android is somehow
            // calling this constructor with an existing instance attached
            // to it, where private properties are already set. weird.
            Log.e(TAG, "Service already created. Bootstraped? " + dht.isBootstraped());
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
		super.onCreate();
        Log.i(TAG, "Creating the background service.");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
        Log.i(TAG, "Starting the service.");

        bootstrap();
        
		// If we get killed, after returning from here, restart
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
        Log.i(TAG, "Destroying the service.");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	private interface Runnable<K> {
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

	public boolean isBootstraped() {
		return dht.isBootstraped();
	}
		
	public ListenableFuture<Boolean> waitForBootstrap() {
		return run(new Runnable<Boolean>() {
			@Override
			public Boolean run() throws Exception {
				while (!dht.isBootstraped()) {
					Thread.sleep(1000);
				}
				return true;
			}
		});
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
		return context.toString()
				+ "" 
				+ context.getRouteTable().toString()
				+ ""
				+ context.getDatabase().toString();
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
			DHTMessage message = context.getMessageFactory()
					.createMessage(src, ByteBuffer.wrap(data));				

			Log.i("BackgroundService", "Got message: " + message.getOpCode() +
					", from: " + message.getContact().toString());

			dispatcher.handleMessage(message);
		} catch (Exception e) {
			// ignores silently.
			throw new RuntimeException(e);
		}
	}
}