package com.kumbaya.www;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

public class WorldWideWeb {
  public static String get(SocketAddress proxy, String url) throws MalformedURLException, IOException {
    java.net.Proxy p = new java.net.Proxy(java.net.Proxy.Type.HTTP, proxy);
    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection(p);
    connection.setDoOutput(true);
    connection.setDoInput(true);
    connection.setRequestProperty("Content-type", "text/xml");
    connection.setRequestProperty("Accept", "text/xml, application/xml");
    connection.setRequestMethod("GET");

    return read(connection);
  }
  
  public static String get(String url) throws MalformedURLException, IOException {
    return read(new URL(url).openConnection());
  }

  private static String read(URLConnection connection) throws IOException {
    Scanner scanner = new Scanner(connection.getInputStream());
    scanner.useDelimiter("\\Z");
    String result = scanner.next();
    scanner.close();
    return result;
  }
}
