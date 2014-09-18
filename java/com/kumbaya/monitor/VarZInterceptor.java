package com.kumbaya.monitor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
class VarZInterceptor implements MethodInterceptor {
	@Inject Provider<Sampler> sampler;
	
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		VarZ varZ = invocation.getMethod().getAnnotation(VarZ.class);
		String key = varZ.value();
		Object result = invocation.proceed();
		for (VarZ.Type type : varZ.type()) {
			if (type == VarZ.Type.COUNTER) {
				sampler.get().sample(key, (Integer) result);
			} else {
				sampler.get().qps(key);
			}
		}
		return result;
	}
}
