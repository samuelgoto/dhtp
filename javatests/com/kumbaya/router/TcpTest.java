package com.kumbaya.router;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
      String clientSentence;
      String capitalizedSentence;

      while(true) {
        try {
          Socket connectionSocket = welcomeSocket.accept();
          BufferedReader inFromClient =
              new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
           DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
           clientSentence = inFromClient.readLine();
           capitalizedSentence = clientSentence.toUpperCase() + '\n';
           outToClient.writeBytes(capitalizedSentence);
        } catch (IOException e) {
        }
      }
    }
  }
  
  private static class TcpClient {
    String send(String sentence) throws UnknownHostException, IOException {
      String modifiedSentence;
      Socket clientSocket = new Socket("localhost", 6789);
      DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
      BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      outToServer.writeBytes(sentence + '\n');
      modifiedSentence = inFromServer.readLine();
      clientSocket.close();
      return modifiedSentence;
    }
  }
  
  public void testPacket() throws IOException {
    Thread server = new Thread(new TcpServer(6789));
    server.start();

    TcpClient client = new TcpClient();
    
    String result = client.send("hello world");
    
    assertEquals("HELLO WORLD", result);
  }
}
