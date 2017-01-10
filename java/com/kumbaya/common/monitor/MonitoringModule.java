package com.kumbaya.common.monitor;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.MapBinder;
import com.kumbaya.common.annotations.VarZ;
import javax.servlet.Servlet;

public class MonitoringModule extends AbstractModule {
	@Override
	protected void configure() {
		VarZInterceptor interceptor = new VarZInterceptor();
		bindInterceptor(Matchers.any(), 
				Matchers.annotatedWith(VarZ.class ), 
				interceptor);
		requestInjection(interceptor);
		
		
		MapBinder<String, Servlet> mapbinder
          = MapBinder.newMapBinder(binder(), String.class, Servlet.class);

        mapbinder.addBinding("/varz/*").to(VarZGraphServlet.class);
        mapbinder.addBinding("/varz").to(VarZServlet.class);
        mapbinder.addBinding("/threadz").to(ThreadZServlet.class);
        mapbinder.addBinding("/healthz").to(HealthZServlet.class);
	}
}
