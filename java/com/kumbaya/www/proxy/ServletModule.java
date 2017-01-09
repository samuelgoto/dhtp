package com.kumbaya.www.proxy;

import com.google.inject.AbstractModule;
import com.kumbaya.common.Server;

class ServletModule extends AbstractModule {
	@Override
	protected void configure() {
        bind(Server.class).to(JettyServer.class);
	}
}
