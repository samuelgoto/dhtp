package com.kumbaya.android.client;

import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.io.MessageDispatcher;

import android.content.Context;

import com.google.inject.AbstractModule;
import com.kumbaya.dht.AsyncMessageDispatcher;
import com.kumbaya.dht.Server;

class ClientModule extends AbstractModule {
	private final Context context;
	
	ClientModule(Context context) {
		this.context = context;
	}
	
	@Override
	protected void configure() {
		bind(MessageDispatcher.class).to(AsyncMessageDispatcher.class);
		bind(Context.class).toInstance(context);
		bind(Server.class).to(GcmServer.class);

		bind(org.limewire.mojito.Context.class).toInstance(
				(org.limewire.mojito.Context) MojitoFactory.createDHT(
						CommonUtilities.GCM_HOSTNAME));
	}
}