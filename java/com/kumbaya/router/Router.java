package com.kumbaya.router;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.kumbaya.common.Server;
import java.io.IOException;
import java.net.SocketAddress;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;

public class Router implements Server {
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
	}

  @Override
  public void bind(SocketAddress address) throws IOException {
  }

  @Override
  public void close() {
  }
}
