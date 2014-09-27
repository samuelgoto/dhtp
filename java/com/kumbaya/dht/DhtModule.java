package com.kumbaya.dht;

import javax.servlet.http.HttpServlet;

import org.limewire.mojito.Context;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.io.MessageDispatcher;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

import com.kumbaya.monitor.VarZModule;
import com.kumbaya.www.ServletModule;

public class DhtModule extends AbstractModule {
	  @Override
      protected void configure() {
          install(new VarZModule());
          install(new ServletModule());

          bind(MessageDispatcher.class).to(AsyncMessageDispatcher.class);
	  }
}
