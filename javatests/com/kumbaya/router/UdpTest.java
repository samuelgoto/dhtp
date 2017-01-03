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
    private final int port;
    
    UdpServer(int port) {
      this.port = port;
    }

    @Override
    public void run() {
      try {
        DatagramSocket serverSocket = new DatagramSocket(port);
        byte[] receiveData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        serverSocket.receive(receivePacket);
        Packet message = Packet.of(receivePacket);
        String capitalizedSentence = new String(message.getData()).toUpperCase();
        Packet response = new Packet(capitalizedSentence.getBytes(), message.getIPAddress(), message.getPort());
        serverSocket.send(response.datagram());
        serverSocket.close();
      } catch (SocketException e) {
      } catch (IOException e) {
      }
    }
  }
  
  private static class Packet {
    private final byte[] data;
    private final InetAddress IPAddress;
    private final int port;
    
    Packet(byte[] data, InetAddress IPAddress, int port) {
      this.data = data;
      this.IPAddress = IPAddress;
      this.port = port;
    }
    
    DatagramPacket datagram() {
      DatagramPacket sendPacket = new DatagramPacket(data, data.length, IPAddress, port);
      return sendPacket;
    }
    
    static Packet of(DatagramPacket receivePacket) {
      String modifiedSentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
      InetAddress IPAddress = receivePacket.getAddress();
      int port = receivePacket.getPort();
      return new Packet(modifiedSentence.getBytes(), IPAddress, port);
    }
    
    byte[] getData() {
      return data;
    }
    
    InetAddress getIPAddress() {
      return IPAddress;
    }
    
    int getPort() {
      return port;
    }
  }

  private class UdpClient {    
    private final InetAddress IPAddress;
    private final int port;
    
    UdpClient(InetAddress IPAddress, int port) {
      this.IPAddress = IPAddress;
      this.port = port;
    }
    
    public byte[] send(byte[] inFromUser) throws IOException {
      DatagramSocket clientSocket = new DatagramSocket();
      clientSocket.send(new Packet(inFromUser, IPAddress, port).datagram());
      byte[] receiveData = new byte[1024];
      DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
      clientSocket.receive(receivePacket);
      Packet received = Packet.of(receivePacket);
      clientSocket.close();
      return received.getData();
    }
  }

  @Test
  public void testServer() throws IOException {
    Thread server = new Thread(new UdpServer(9876));
    server.start();

    UdpClient client = new UdpClient(InetAddress.getByName("localhost"), 9876);
    byte[] result = client.send("hello world".getBytes());
    assertEquals("HELLO WORLD", new String(result));
  }  
}
