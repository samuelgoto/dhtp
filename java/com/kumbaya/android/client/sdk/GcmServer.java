package com.kumbaya.android.client.sdk;

import static com.kumbaya.android.client.sdk.CommonUtilities.SENDER_ID;

import java.io.IOException;
import java.net.SocketAddress;

import org.limewire.mojito.KUID;

import android.content.Context;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.kumbaya.dht.Server;

class GcmServer implements Server {
    private static final String TAG = "GcmServer";

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
		// TODO(goto): figure out a solution to this problem.
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
		        // TODO(goto): make this sleep on a Future instead.
				Thread.sleep(100);
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
