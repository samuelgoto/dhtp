package com.kumbaya.android.client;

import static com.kumbaya.android.client.CommonUtilities.DISPLAY_MESSAGE_ACTION;
import static com.kumbaya.android.client.CommonUtilities.EXTRA_MESSAGE;
import static com.kumbaya.android.client.CommonUtilities.SENDER_ID;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.io.Tag;
import org.limewire.mojito.messages.DHTMessage;
import org.limewire.mojito.result.BootstrapResult;
import org.limewire.security.SecureMessage;
import org.limewire.security.SecureMessageCallback;

import com.google.android.gcm.GCMRegistrar;
import com.google.common.base.Optional;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.util.Providers;
import com.kumbaya.dht.AsyncMessageDispatcher;
import com.kumbaya.dht.Dht;
import com.kumbaya.dht.Server;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
	AsyncTask<Void, Void, Void> mRegisterTask;
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

	@Inject private Dht dht;
	@Inject private AsyncMessageDispatcher dispatcher;
	@Inject private org.limewire.mojito.Context messageFactory;
	private final Binder mBinder = new LocalBinder();

	public class LocalBinder extends Binder {
		BackgroundService getService() {
			// Return this instance of LocalService so clients can call public methods
			return BackgroundService.this;
		}
	}

	private void error(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	// Handler that receives messages from the thread
	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			// Normally we would do some work here, like download a file.
			// For our sample, we just sleep for 5 seconds.

			try {
				dht.start(hostname, port, proxy);
				dht.bootstrap("kumbaya-node0.herokuapp.com", 80).get();
			} catch (IOException e) {
				error("io exception");
			} catch (InterruptedException e) {
				error("interrupted exception");
			} catch (ExecutionException e) {
				error("execution exception");
			}
		}
	}

	@Override
	public void onCreate() {
		// Start up the thread running the service.  Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block.  We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		HandlerThread thread = new HandlerThread("ServiceStartArguments",
				android.os.Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		injector.injectMembers(this);

		// Get the HandlerThread's Looper and use it for our Handler
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
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
			//GCMRegistrar.unregister(context);
			if (regId.equals("")) {
				// Automatically registers application on startup.
				GCMRegistrar.register(context, SENDER_ID);
			} else {
				ServerUtilities.register(context, regId);
			}
			
			while (!GCMRegistrar.isRegisteredOnServer(context)) {
				try {
					// Blocks until we are not registered on the server.
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
		Toast.makeText(this, "Kumbaya starting", Toast.LENGTH_SHORT).show();

		if (intent == null || dht.isBootstraped()) {
			Toast.makeText(this, "Kumbaya is already running ... skipping.",
					Toast.LENGTH_SHORT).show();
		} else {
			// For each start request, send a message to start a job and deliver the
			// start ID so we know which request we're stopping when we finish the job
			Message msg = mServiceHandler.obtainMessage();
			msg.arg1 = startId;
			mServiceHandler.sendMessage(msg);
		}

		// If we get killed, after returning from here, restart
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	/** method for clients */
	public void handleMessage(InetSocketAddress src, byte[] data) {
		if (data == null || src == null || src.getPort() == 0) {
			return;
		}

		try {
			DHTMessage message = messageFactory.getMessageFactory()
					.createMessage(src, ByteBuffer.wrap(data));				

			Log.w("BackgroundService", "Got message: " + message.getOpCode() +
					", from: " + message.getContact().toString());

			Log.w("BackgroundService", messageFactory.toString());
			Log.w("BackgroundService", messageFactory.getRouteTable().toString());

			dispatcher.handleMessage(message);

			Log.w("BackgroundService", messageFactory.toString());
			Log.w("BackgroundService", messageFactory.getRouteTable().toString());

		} catch (Exception e) {
			// ignores silently.
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onDestroy() {
		Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
	}
}