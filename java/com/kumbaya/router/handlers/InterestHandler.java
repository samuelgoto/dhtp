package com.kumbaya.router.handlers;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.kumbaya.common.InetSocketAddresses;
import com.kumbaya.common.Flags.Flag;
import com.kumbaya.router.Kumbaya;
import com.kumbaya.router.Packets.Data;
import com.kumbaya.router.Packets.Interest;
import com.kumbaya.router.TcpServer.Handler;
import com.kumbaya.router.TcpServer.Interface;
import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class InterestHandler implements Handler<Interest> {
  private static final Log logger = LogFactory.getLog(InterestHandler.class);

  private @Inject @Flag("forwarding") String forwarding = "localhost:8082";
  private final Kumbaya client;

  @Inject
  InterestHandler(Kumbaya client) {
    this.client = client;
  }
  
  @Override
  public void handle(Interest request, Interface response) throws IOException {
    logger.info("Handling a request: " + request.getName().getName());
    try {
      // Forwards the interest to the next hop.
      Optional<Data> result = client.send(InetSocketAddresses.parse(forwarding), request);

      if (result.isPresent()) {
        logger.info("Got a Data packet, responding.");
        response.push(result.get());
      }
    } catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
        logger.error("Unexpected error: ", e);
        e.printStackTrace();
    }
  }
}