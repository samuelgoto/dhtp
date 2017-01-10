package com.kumbaya.router;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
  private final InetSocketAddress host;
  
  @Inject
  public Client(InetSocketAddress host) {
    this.host = host;
  }

  public <T> Optional<T> send(Object packet) throws UnknownHostException, IOException, IllegalArgumentException, IllegalAccessException, InstantiationException {
    Socket clientSocket = new Socket(host.getHostName(), host.getPort());
    DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
    Serializer.serialize(outToServer, packet);
    T result = Serializer.unserialize(clientSocket.getInputStream());
    clientSocket.close();
    return Optional.of(result);
  }
}