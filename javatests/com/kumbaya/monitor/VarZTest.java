package com.kumbaya.monitor;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.kumbaya.monitor.Sampler.Clock;

public class VarZTest {
	private IMocksControl control = EasyMock.createControl();
	private Clock clock = control.createMock(Clock.class);
	
	@Before
	public void setUp() {
		control.reset();
	}
	
	@After
	public void tearDown() {
		control.verify();
	}
	
	static class Foo {
		@VarZ("b") void doStuff() {
		}
	}

	@Test
	public void testQps() {
		Injector injector = create();
		Foo foo = injector.getInstance(Foo.class);
		Sampler sampler = injector.getInstance(Sampler.class);

		EasyMock.expect(clock.now()).andReturn(new Date(0));
		EasyMock.expect(clock.now()).andReturn(new Date(1));

		EasyMock.expect(clock.now()).andReturn(new Date(1001));
		EasyMock.expect(clock.now()).andReturn(new Date(1002));
		EasyMock.expect(clock.now()).andReturn(new Date(1003));
		EasyMock.expect(clock.now()).andReturn(new Date(1004));
		
		control.replay();
		
		foo.doStuff();
		assertEquals(1, sampler.get("b").size());
		assertEquals("0: 1", sampler.get("b").get(0).toString());

		foo.doStuff();
		assertEquals(1, sampler.get("b").size());
		assertEquals("0: 2", sampler.get("b").get(0).toString());

		foo.doStuff();
		assertEquals(2, sampler.get("b").size());
		assertEquals("1000: 1", sampler.get("b").get(1).toString());

		foo.doStuff();
		foo.doStuff();
		foo.doStuff();
		assertEquals(2, sampler.get("b").size());
		assertEquals("1000: 4", sampler.get("b").get(1).toString());
	}

	private Injector create() {
		return Guice.createInjector(new VarZModule(), new AbstractModule() {
			@Override
			protected void configure() {
				bind(Clock.class).toInstance(clock);
			}
		});
	}
}
