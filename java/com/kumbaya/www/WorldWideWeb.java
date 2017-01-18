package com.kumbaya.www;

import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class WorldWideWeb {
  private static final Log logger = LogFactory.getLog(WorldWideWeb.class);
  private static int CONNECTION_TIMEOUT_MS = (int) TimeUnit.SECONDS.toMillis(5); // Timeout in milliseconds.
  
  private static final CloseableHttpClient httpclient = HttpClientBuilder.create()
      .setMaxConnTotal(200)
      .setMaxConnPerRoute(50)
      .build();
  
  public static void setTimeout(long milliseconds) {
    CONNECTION_TIMEOUT_MS = (int) milliseconds;
  }

  public static Optional<String> get(InetSocketAddress proxy, String url) throws MalformedURLException, IOException {
    HttpHost p = new HttpHost(proxy.getHostName(), proxy.getPort());
    RequestConfig.Builder config = RequestConfig.custom()
        .setProxy(p);
    HttpGet request = new HttpGet(url);
    return get(request, config);
  }

  private static Optional<String> get(HttpGet request, RequestConfig.Builder config) throws IOException {
    config.setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS);
    config.setConnectTimeout(CONNECTION_TIMEOUT_MS);
    config.setSocketTimeout(CONNECTION_TIMEOUT_MS);
    
    request.setConfig(config.build());
    
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
      logger.info("DNS resolution error: " + request.getURI(), e);
      return Optional.absent();
    } catch (SocketTimeoutException e) {
      logger.info("Timeout: " + request.getURI(), e);
      return Optional.absent();
    } catch (NoHttpResponseException e) {
      logger.info("No response: " + request.getURI(), e);
      return Optional.absent();
    }
  }


  public static Optional<String> get(String url) throws IOException {
    return get(new HttpGet(url), RequestConfig.custom());
  }
}