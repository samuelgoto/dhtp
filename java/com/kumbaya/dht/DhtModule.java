package com.kumbaya.dht;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.MapBinder;
import com.kumbaya.common.Flags.Flag;
import com.kumbaya.common.monitor.MonitoringModule;
import javax.servlet.Servlet;
import org.limewire.mojito.Context;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.io.MessageDispatcher;

public class DhtModule extends AbstractModule {
  private boolean useHttpDispatcher = false;
  private boolean installDefaultDht = true;

  public DhtModule(boolean useHttpDispatcher, boolean installDefaultDht) {
    this.useHttpDispatcher = useHttpDispatcher;
    this.installDefaultDht = installDefaultDht;
  }

  public DhtModule() {
    this(false, true);
  }

  @Override
  protected void configure() {
    if (installDefaultDht) {
      install(new AbstractModule() {
        @Override
        protected void configure() {}

        @Provides
        Context create(@Flag("host") String hostname) {
          return (Context) MojitoFactory.createDHT(hostname);
        }

      });
    }
    install(new MonitoringModule());
    // bind(Server.class).to(JettyServer.class);

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
