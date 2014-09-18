package com.kumbaya.dht;

import javax.servlet.http.HttpServlet;

import org.limewire.mojito.Context;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.io.MessageDispatcher;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

import com.kumbaya.dht.JettyMessageDispatcher.DhtHandler;
import com.kumbaya.dht.JettyMessageDispatcher.IndexHandler;
import com.kumbaya.dht.JettyMessageDispatcher.ThreadZHandler;

public class DhtModule extends AbstractModule {
	  @Override
      protected void configure() {
	      bind(MessageDispatcher.class).to(HttpMessageDispatcher.class);
	      
	      MapBinder<String, HttpServlet> mapbinder
	         = MapBinder.newMapBinder(binder(), String.class, HttpServlet.class);
	      
	      mapbinder.addBinding("/.well-known/dht").to(DhtHandler.class);
          mapbinder.addBinding("/").to(IndexHandler.class);
          mapbinder.addBinding("/threadz").to(ThreadZHandler.class);
	  }
}
