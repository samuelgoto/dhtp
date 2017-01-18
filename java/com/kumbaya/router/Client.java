package com.kumbaya.router;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.kumbaya.common.Flags.Flag;
import com.kumbaya.common.InetSocketAddresses;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Client {
  private static final Log logger = LogFactory.getLog(Client.class);
  @Inject
  public Client() {
    Packets.register();
    // this.host = InetSocketAddresses.parse(entrypoint);
  }

  public <T> Optional<T> send(InetSocketAddress host, Object packet) throws UnknownHostException, IOException, IllegalArgumentException, IllegalAccessException, InstantiationException {
    logger.info("Sending a request to the network");
    
    Socket clientSocket = new Socket(host.getHostName(), host.getPort());
    DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
    Serializer.serialize(outToServer, packet);
    outToServer.flush();
    try {
      InputStream stream = clientSocket.getInputStream();
      T result = Serializer.unserialize(stream);
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