package com.kumbaya.dht;

import org.limewire.mojito.io.MessageDispatcher;

import com.google.inject.AbstractModule;

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
