package com.kumbaya.router;

import com.google.common.base.Optional;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Kumbaya {
  private static final Log logger = LogFactory.getLog(Kumbaya.class);

  static {
    Packets.register();
  }
  
  public <T> Optional<T> send(InetSocketAddress host, Object packet) throws UnknownHostException, IOException, IllegalArgumentException, IllegalAccessException, InstantiationException {
    logger.info("Sending a request to the network");
    
    Socket socket = new Socket(host.getHostName(), host.getPort());
    DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
    Serializer.serialize(outToServer, packet);
    outToServer.flush();
    try {
      InputStream stream = socket.getInputStream();
      T result = Serializer.unserialize(stream);
      return Optional.of(result);
    } catch (EOFException e) {
      logger.info("No response sent back, returning an absent packet.");
      // If no response was sent back, return an absent content.
      return Optional.absent();
    } finally {
      socket.close();
    }
  }
}