package com.kumbaya.common.monitor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kumbaya.common.monitor.Sampler.Clock;
import com.kumbaya.common.annotations.VarZ;

@Singleton
class VarZInterceptor implements MethodInterceptor {
	@Inject Provider<Sampler> sampler;
	@Inject Provider<Clock> clock;
	
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		VarZ varZ = invocation.getMethod().getAnnotation(VarZ.class);
		String key = varZ.value();
		Object result = invocation.proceed();
		sampler.get().log(key);
		return result;
	}
}
