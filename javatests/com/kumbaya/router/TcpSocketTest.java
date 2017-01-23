package com.kumbaya.router;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import com.kumbaya.router.Packets.Data;

import junit.framework.TestCase;

public class TcpSocketTest extends TestCase {


  public void testBrokenPipe_writes10MBs() throws UnknownHostException, IOException, IllegalArgumentException, IllegalAccessException, InstantiationException {
    final ServerSocket welcomeSocket = new ServerSocket(8080);

    Thread server = new Thread(new Runnable() {
      @Override
      public void run() {
        try {

          while (true) { 
            Socket connectionSocket = welcomeSocket.accept();

            BufferedReader inFromClient = new BufferedReader(
                new InputStreamReader(connectionSocket.getInputStream()));
            int bytes = Integer.parseInt(inFromClient.readLine());

            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
            // Writes as many bytes as the client asks.
            byte[] content = new byte[bytes];
            Data result = new Data();
            result.getName().setName("http://example.com/largefile");
            result.getMetadata().setContentType("text/html");
            result.setContent(content);
            Serializer.serialize(outToClient, result);
            outToClient.close();
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });

    server.setName("server");
    server.start();

    int numBytes = 10 * 1000 * 1000;
    for (int i = 0; i < 10; i++) {
      int length = send(numBytes).getContent().length;
      assertEquals(numBytes, length);
    }
    
    welcomeSocket.close();
  }

  private Data send(int numBytes) throws UnknownHostException, IOException, IllegalArgumentException, IllegalAccessException, InstantiationException {
    Socket clientSocket = new Socket("localhost", 8080);
    DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());

    outToServer.writeBytes(Integer.toString(numBytes) + "\n");

    InputStream stream = clientSocket.getInputStream();

    Data foo = Serializer.unserialize(stream);
    
    clientSocket.close();

    return foo;
  }
}
