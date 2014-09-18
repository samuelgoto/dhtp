package com.kumbaya.dht;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.junit.Before;
import org.junit.Test;
import org.limewire.mojito.Context;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.io.MessageDispatcherImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class LoadTest {
    private static final Log log = LogFactory.getLog(LoadTest.class);

	@Before
	public void setUp() {
		BasicConfigurator.configure(new ConsoleAppender(new PatternLayout(
				"[%-5p] %d %c - %m%n")));
	}

	@Test
	public void twoNodes() throws Exception {
		Dht node0 = createDht(8080);
		Dht node1 = createDht(8081);
		
		node1.bootstrap("localhost", 8080).get(1, TimeUnit.SECONDS);
		assertTrue(node1.isBootstraped());
	
		while (!node0.isBootstraped()) {
			log.info("Waiting for node0 to bootstrap ...");
			Thread.sleep(1000);
		}
		
		assertTrue(node0.isBootstraped());
		
		node0.stop();
		node1.stop();
	}
	
	private Dht createDht(int port) throws NumberFormatException, IOException {
		// Makes sure each instance of the Dht has its own set of classes,
		// rather than sharing instances of @Singletons for example.
		Injector injector = Guice.createInjector(
				new DhtModule(),
				new AbstractModule() {
					@Override
					protected void configure() {
						bind(Context.class).toInstance(
								(Context) MojitoFactory.createDHT("localhost"));
					}
				});

		Dht dht = injector.getInstance(Dht.class);

		dht.start("localhost", port, port);

		return dht;
	}
}
