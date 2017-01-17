package com.kumbaya.www;

import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class WorldWideWeb {
  private static final CloseableHttpClient httpclient = HttpClients.createDefault();

  public static Optional<String> get(InetSocketAddress proxy, String url) throws MalformedURLException, IOException {
    HttpHost p = new HttpHost(proxy.getHostName(), proxy.getPort());
    RequestConfig config = RequestConfig.custom()
        .setProxy(p)
        .build();
    HttpGet request = new HttpGet(url);
    request.setConfig(config);
    return get(request);
  }

  public static Optional<String> get(HttpGet request) throws IOException {
    try {
      CloseableHttpResponse response = httpclient.execute(request);
      try {
        if (response.getStatusLine().getStatusCode() == 200) {
          return Optional.of(new String(ByteStreams.toByteArray(response.getEntity().getContent())));
        } else {
          return Optional.absent();
        }
      } finally {
        response.close();
      }
    } catch (UnknownHostException | HttpHostConnectException e) {
      // Should we catch this too HttpHostConnectException?
      return Optional.absent();
    }
  }


  public static Optional<String> get(String url) throws IOException {
    return get(new HttpGet(url));
  }
}