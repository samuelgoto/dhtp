package com.kumbaya.dht;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.kumbaya.common.Server;
import com.kumbaya.common.monitor.MonitoringModule;
import com.kumbaya.www.JettyServer;
import javax.servlet.Servlet;
import org.limewire.mojito.io.MessageDispatcher;

public class DhtModule extends AbstractModule {
  private boolean useHttpDispatcher = false;

  public DhtModule(boolean useHttpDispatcher) {
    this.useHttpDispatcher = useHttpDispatcher;
  }

  public DhtModule() {
    this(false);
  }

  @Override
  protected void configure() {
    // install(new VarZModule());
    install(new MonitoringModule());
    bind(Server.class).to(JettyServer.class);


    install(new AbstractModule() {
      @Override
      protected void configure() {
        MapBinder<String, Servlet> mapbinder =
            MapBinder.newMapBinder(binder(), String.class, Servlet.class);

        mapbinder.addBinding("/.well-known/dht").to(DhtServlet.class);
      }
    });

    if (useHttpDispatcher) {
      bind(MessageDispatcher.class).to(AsyncMessageDispatcher.class);
    }
  }
}
