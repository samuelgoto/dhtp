package com.kumbaya.router;

import com.kumbaya.router.Packets.Data;
import com.kumbaya.router.Packets.Interest;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import junit.framework.TestCase;

public class TcpTest extends TestCase {

  private static class TcpServer implements Runnable {
    private final ServerSocket welcomeSocket;
    
    TcpServer(int port) throws IOException {
      this.welcomeSocket = new ServerSocket(port);
    }
    
    @Override
    public void run() {
      while(true) {
        try {
          Socket connectionSocket = welcomeSocket.accept();
           Interest request = Serializer.unserialize(connectionSocket.getInputStream());
           DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
           Data response = new Data();
           response.name = request.name;
           response.metadata.freshnessPeriod = 2;
           response.content = "hello world".getBytes();

           Serializer.serialize(outToClient, response);
        } catch (IOException | IllegalArgumentException | IllegalAccessException | InstantiationException e) {
        }
      }
    }
  }
  
  private static class TcpClient {
    <T> T send(Object packet) throws UnknownHostException, IOException, IllegalArgumentException, IllegalAccessException, InstantiationException {
      Socket clientSocket = new Socket("localhost", 6789);
      DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
      Serializer.serialize(outToServer, packet);
      T result = Serializer.unserialize(clientSocket.getInputStream());
      clientSocket.close();
      return result;
    }
  }
  
  public void testPacket() throws IOException, IllegalArgumentException, IllegalAccessException, InstantiationException {
    Thread server = new Thread(new TcpServer(6789));
    server.start();

    TcpClient client = new TcpClient();
    
    Interest interest = new Interest();
    interest.name.name = "foo";
    interest.name.sha256 = "bar";
    Data result = client.send(interest);
    
    assertEquals("foo", result.name.name);
    assertEquals(2, result.metadata.freshnessPeriod);
    assertEquals("hello world", new String(result.content));
  }
}
