package com.kumbaya.dht;

import java.io.File;

import com.google.common.base.Optional;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.limewire.mojito.Context;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.io.MessageDispatcher;
import org.mapdb.DB;
import org.mapdb.DBMaker;


public class Standalone {
	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure(new ConsoleAppender(new PatternLayout(
				"[%-5p] %d %c - %m%n")));

		Options options = new Options();
		options.addOption("port", true, "The external port");
		options.addOption("hostname", true, "The external hostname");
		options.addOption("bootstrap", true, "The node to bootstrap");
		options.addOption("db", true, "Whether to write values to disk or not");

		CommandLineParser parser = new PosixParser();
		CommandLine line = parser.parse(options, args);

		line.getOptionValue("port");

		final int port;
		if (System.getenv().containsKey("PORT")) {
			port = Integer.valueOf(System.getenv("PORT"));
		} else if (line.hasOption("port")) {
			port = Integer.valueOf(line.getOptionValue("port"));
		} else {
			port = 8080;
		}

		final int proxy;
		final String hostname;
		if (line.hasOption("hostname")) {
			String[] ip = line.getOptionValue("hostname").split(":");
			hostname = ip[0];
			proxy = Integer.valueOf(ip[1]);
		} else {
			proxy = port;
			hostname = "localhost";
		}
		
		final Optional<String> localDb = line.hasOption("db") ? 
				Optional.of(line.getOptionValue("db", "/tmp/kumbaya.db")) :
				Optional.<String>absent();
		
		Injector injector = Guice.createInjector(
				new DhtModule(),
				new AbstractModule() {
					@Override
					protected void configure() {
						bind(Context.class).toInstance(
								(Context) MojitoFactory.createDHT(hostname));
						if (localDb.isPresent()) {
							DB db = DBMaker.newFileDB(new File(localDb.get()))
									.closeOnJvmShutdown()
									.make();

							bind(DB.class).toInstance(db);
						}
					}
				});

		Dht dht = injector.getInstance(Dht.class);

		dht.start(hostname, port, proxy);

		if (line.hasOption("bootstrap")) {
			String[] ip = line.getOptionValue("bootstrap").split(":");
			dht.bootstrap(ip[0], Integer.parseInt(ip[1]));
		}
	}
}
