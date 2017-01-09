package com.kumbaya.monitor;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.MapBinder;
import com.kumbaya.annotations.VarZ;
import javax.servlet.http.HttpServlet;

public class VarZModule extends AbstractModule {
	@Override
	protected void configure() {
		VarZInterceptor interceptor = new VarZInterceptor();
		bindInterceptor(Matchers.any(), 
				Matchers.annotatedWith(VarZ.class ), 
				interceptor);
		requestInjection(interceptor);
		
		
		MapBinder<String, HttpServlet> mapbinder
          = MapBinder.newMapBinder(binder(), String.class, HttpServlet.class);

        mapbinder.addBinding("/varz/*").to(VarZGraphServlet.class);
        mapbinder.addBinding("/varz").to(VarZServlet.class);
        mapbinder.addBinding("/threadz").to(ThreadZServlet.class);
        mapbinder.addBinding("/healthz").to(HealthZServlet.class);
	}
}
