package com.kumbaya.dht;

import org.limewire.mojito.io.MessageDispatcher;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.kumbaya.common.Server;
import com.kumbaya.common.monitor.MonitoringModule;
import com.kumbaya.www.JettyServer;
import com.kumbaya.www.proxy.ServletModule;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

public class DhtModule extends AbstractModule {
  @Override
  protected void configure() {
    // install(new VarZModule());
    install(new MonitoringModule());
    bind(Server.class).to(JettyServer.class);
    

    install(new AbstractModule() {
      @Override
      protected void configure() {
        MapBinder<String, Servlet> mapbinder
        = MapBinder.newMapBinder(binder(), String.class, Servlet.class);

        mapbinder.addBinding("/.well-known/dht").to(DhtServlet.class);
      }      
    });

    bind(MessageDispatcher.class).to(AsyncMessageDispatcher.class);
  }
}
