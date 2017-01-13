package com.kumbaya.router;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Client {
  private static final Log logger = LogFactory.getLog(Client.class);

  private final InetSocketAddress host;

  @Inject
  public Client(InetSocketAddress host) {
    Packets.register();
    this.host = host;
  }

  public <T> Optional<T> send(Object packet) throws UnknownHostException, IOException, IllegalArgumentException, IllegalAccessException, InstantiationException {
    logger.info("Sending a request to the network");
    Socket clientSocket = new Socket(host.getHostName(), host.getPort());
    DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
    Serializer.serialize(outToServer, packet);
    outToServer.flush();
    try {
      T result = Serializer.unserialize(clientSocket.getInputStream());
      return Optional.of(result);
    } catch (EOFException e) {
      logger.info("No response sent back", e);
      // If no response was sent back, return an absent content.
      return Optional.absent();
    } finally {
      clientSocket.close();
    }
  }
}