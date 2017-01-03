package com.kumbaya.router;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import junit.framework.TestCase;
import org.junit.Test;

public class UdpTest extends TestCase {
  private class UdpServer implements Runnable {

    @Override
    public void run() {
      try {
        DatagramSocket serverSocket = new DatagramSocket(9876);
        byte[] receiveData = new byte[1024];
        byte[] sendData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        serverSocket.receive(receivePacket);
        String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
        System.out.println("RECEIVED: " + sentence);
        InetAddress IPAddress = receivePacket.getAddress();
        int port = receivePacket.getPort();
        String capitalizedSentence = sentence.toUpperCase();
        sendData = capitalizedSentence.getBytes();
        DatagramPacket sendPacket =
            new DatagramPacket(sendData, sendData.length, IPAddress, port);
        serverSocket.send(sendPacket);
        serverSocket.close();
      } catch (SocketException e) {
      } catch (IOException e) {
      }
    }
  }

  private class UdpClient {    
    public String send(BufferedReader inFromUser) throws IOException {
      DatagramSocket clientSocket = new DatagramSocket();
      InetAddress IPAddress = InetAddress.getByName("localhost");
      byte[] sendData = new byte[1024];
      byte[] receiveData = new byte[1024];
      String sentence = inFromUser.readLine();
      sendData = sentence.getBytes();
      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
      clientSocket.send(sendPacket);
      DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
      clientSocket.receive(receivePacket);
      String modifiedSentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
      System.out.println("FROM SERVER:" + modifiedSentence);
      clientSocket.close();
      return modifiedSentence;
    }
  }

  @Test
  public void testServer() throws IOException {
    Thread server = new Thread(new UdpServer());
    server.start();

    UdpClient client = new UdpClient();
    String result = client.send(new BufferedReader(new StringReader("hello world")));
    assertEquals("HELLO WORLD", result);
  }  
}
