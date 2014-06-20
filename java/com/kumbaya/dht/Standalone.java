package com.kumbaya.dht;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.multibindings.MapBinder;

import com.kumbaya.dht.JettyMessageDispatcher.DhtHandler;
import com.kumbaya.dht.JettyMessageDispatcher.IndexHandler;

import javax.servlet.http.HttpServlet;

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


public class Standalone {
	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure(new ConsoleAppender(new PatternLayout(
		    "[%-5p] %d %c - %m%n")));

		Options options = new Options();
		options.addOption("port", true, "The external port");
		options.addOption("hostname", true, "The external hostname");
		options.addOption("bootstrap", true, "The node to bootstrap");

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
		
		Injector injector = Guice.createInjector(new AbstractModule() {
		  @Override
          protected void configure() {
		      bind(Context.class).toInstance(
		          (Context) MojitoFactory.createDHT(hostname));
		      bind(MessageDispatcher.class).to(HttpMessageDispatcher.class);
		      bind(Dispatcher.class).to(JettyMessageDispatcher.class);
		      
		      MapBinder<String, HttpServlet> mapbinder
		         = MapBinder.newMapBinder(binder(), String.class, HttpServlet.class);
		      
		      mapbinder.addBinding("/.well-known/dht").to(DhtHandler.class);
              mapbinder.addBinding("/").to(IndexHandler.class);
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
