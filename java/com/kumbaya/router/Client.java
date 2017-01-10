package com.kumbaya.router;

import com.google.common.base.Optional;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
  private final String hostname;
  private final int port;
  
  public Client(String hostname, int port) {
    this.hostname = hostname;
    this.port = port;
  }

  public <T> Optional<T> send(Object packet) throws UnknownHostException, IOException, IllegalArgumentException, IllegalAccessException, InstantiationException {
    Socket clientSocket = new Socket(hostname, port);
    DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
    Serializer.serialize(outToServer, packet);
    T result = Serializer.unserialize(clientSocket.getInputStream());
    clientSocket.close();
    return Optional.of(result);
  }
}