package com.kumbaya.router;

import java.io.IOException;
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
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        serverSocket.receive(receivePacket);
        Message message = Message.deserialize(receivePacket);
        String capitalizedSentence = message.getMessage().toUpperCase();
        Message response = new Message(capitalizedSentence, message.getIPAddress(), message.getPort());
        serverSocket.send(response.serialize());
        serverSocket.close();
      } catch (SocketException e) {
      } catch (IOException e) {
      }
    }
  }
  
  private static class Message {
    private final String message;
    private final InetAddress IPAddress;
    private final int port;
    
    Message(String message, InetAddress IPAddress, int port) {
      this.message = message;
      this.IPAddress = IPAddress;
      this.port = port;
    }
    
    DatagramPacket serialize() {
      byte[] sendData = new byte[1024];
      String sentence = message;
      sendData = sentence.getBytes();
      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
      return sendPacket;
    }
    
    static Message deserialize(DatagramPacket receivePacket) {
      String modifiedSentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
      InetAddress IPAddress = receivePacket.getAddress();
      int port = receivePacket.getPort();
      return new Message(modifiedSentence, IPAddress, port);
    }
    
    String getMessage() {
      return message;
    }
    
    InetAddress getIPAddress() {
      return IPAddress;
    }
    
    int getPort() {
      return port;
    }
  }

  private class UdpClient {    
    public String send(String inFromUser) throws IOException {
      DatagramSocket clientSocket = new DatagramSocket();
      clientSocket.send(new Message(inFromUser, InetAddress.getByName("localhost"), 9876).serialize());
      byte[] receiveData = new byte[1024];
      DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
      clientSocket.receive(receivePacket);
      Message received = Message.deserialize(receivePacket);
      clientSocket.close();
      return received.getMessage();
    }
  }

  @Test
  public void testServer() throws IOException {
    Thread server = new Thread(new UdpServer());
    server.start();

    UdpClient client = new UdpClient();
    String result = client.send("hello world");
    assertEquals("HELLO WORLD", result);
  }  
}
