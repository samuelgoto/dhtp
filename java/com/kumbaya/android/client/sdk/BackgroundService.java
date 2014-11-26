package com.kumbaya.android.client.sdk;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.io.MessageDispatcher.MessageDispatcherEvent;
import org.limewire.mojito.io.MessageDispatcher.MessageDispatcherListener;
import org.limewire.mojito.messages.DHTMessage;

import com.google.common.base.Function;
import com.google.common.base.Optional;
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
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

public class BackgroundService extends Service {
	public static String UPDATE_ACTION = "com.kumbaya.android.client.sdk.UPDATE_ACTION";
	public static String BOOTSTRAPED_ACTION = "com.kumbaya.android.client.sdk.BOOTSTRAPED_ACTION";
    private static final String TAG = "BackgroundService";
	// NOTE(goto): you can set this to localhost while running appengine
	// locally.
	private final String hostname = CommonUtilities.GCM_HOSTNAME;
	private final int port = CommonUtilities.GCM_PORT;
	private final int proxy = CommonUtilities.GCM_PORT;
	private final Injector injector = Guice.createInjector(
			new SdkModule(this));

	@Inject private Dht dht = null;
	@Inject private AsyncMessageDispatcher dispatcher = null;
	@Inject private org.limewire.mojito.Context context = null;
	private Context wrapper = this;
	private final Binder mBinder = new LocalBinder();

	int corePoolSize = 60;
	int maximumPoolSize = 80;
	int keepAliveTime = 10;

	// TODO(goto): figure out how to give this threadpool a higher priority.
	// Adding more threads has substantially increased the performance of the DHT.
	BlockingQueue<java.lang.Runnable> workQueue = new LinkedBlockingQueue<java.lang.Runnable>(maximumPoolSize);
	Executor executor = new ThreadPoolExecutor(
			corePoolSize,
			maximumPoolSize,
			keepAliveTime, 
			TimeUnit.SECONDS, 
			workQueue);
	
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
		public BackgroundService getService() {
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

        dispatcher.addMessageDispatcherListener(new MessageDispatcherListener() {
			@Override
			public void handleMessageDispatcherEvent(MessageDispatcherEvent event) {
				String destination = event.getSocketAddress() != null ? 
						event.getSocketAddress().toString() : ""; 
				dispatchEvent(event.getMessage().getOpCode().toString() + 
						" to: " + destination);
			}
        });
        
        Log.i(TAG, "Bootstraping.");
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
		}.executeOnExecutor(executor);
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
				dispatchEvent("Starting the DHT");
		        Log.i(TAG, "Starting the DHT.");
		        
		        Optional<E164> id = E164.localNumber(wrapper);
		        
		        if (id.isPresent()) {
		        	// Makes the id of the node stable, via associating the node
		        	// that is running on this phone with its phone number.
		        	// The phone number is hashed, so it is opaque to everybody.
		        	// This helps not creating duplicates on restarts and
		        	// should lead to a better replication strategy for neighbors.
		        	dht.setId(id.get().toString());
		        }
		        
				dispatchEvent("Binding server");
				try {
					if (dht.isBound()) {
				        Log.i(TAG, "Already bound..");
					} else {
				        Log.i(TAG, "Binding to port.");
				        dht.start(hostname, port, proxy);
					}
					dispatchEvent("Done. Bootstraping.");

					if (dht.isBootstraped()) {
				        Log.i(TAG, "Already bootstraped.");
					} else {
				        Log.i(TAG, "Bootstraping.");
						dht.bootstrap("kumbaya-node0.herokuapp.com", 80).get();
				        Log.i(TAG, "Done bootstraping.");
					}

			        Log.i(TAG, "Done. Bootstraped.");

			        Intent intent = new Intent(BOOTSTRAPED_ACTION);
			        sendBroadcast(intent);
					
			        if (id.isPresent()) {
				        // We don't block necessarily on the announcement.
						put(id.get().toString(), id.get().toString());
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

			dispatchEvent(
					message.getOpCode().toString()  + " from: " + 
					message.getContact().getContactAddress().toString());
			
			dispatcher.handleMessage(message);
		} catch (Exception e) {
			// ignores silently.
			throw new RuntimeException(e);
		}
	}
	
	private void dispatchEvent(String message) {
        Intent intent = new Intent(UPDATE_ACTION);
        Log.i(TAG, message);
        intent.putExtra("message", message);
        sendBroadcast(intent);
	}
}