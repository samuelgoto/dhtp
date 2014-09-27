package com.kumbaya.www;

import javax.servlet.http.HttpServlet;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.kumbaya.dht.Server;

public class ServletModule extends AbstractModule {
	@Override
	protected void configure() {
		MapBinder<String, HttpServlet> mapbinder
			= MapBinder.newMapBinder(binder(), String.class, HttpServlet.class);

        bind(Server.class).to(JettyServer.class);

        mapbinder.addBinding("/varz/*").to(VarZGraphServlet.class);
		mapbinder.addBinding("/varz").to(VarZServlet.class);
        mapbinder.addBinding("/threadz").to(ThreadZServlet.class);
        mapbinder.addBinding("/healthz").to(HealthZServlet.class);
        mapbinder.addBinding("/").to(IndexServlet.class);
        mapbinder.addBinding("/.well-known/dht").to(DhtServlet.class);
	}
}
