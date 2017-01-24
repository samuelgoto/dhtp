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
  public static int DEFAULT_CONNECTION_TIMEOUT_MS = (int) TimeUnit.SECONDS.toMillis(5); 
  private static int CONNECTION_TIMEOUT_MS = DEFAULT_CONNECTION_TIMEOUT_MS; // Timeout in milliseconds.
  
  public static class Resource {
    private final int status;
    private final byte[] content;
    private final Optional<String> contentType;
    
    Resource(int status, byte[] content, Optional<String> contentType) {
      this.status = status;
      this.content = content;
      this.contentType = contentType;
    }
    
    static Resource of(String content) {
      return of(200, content.getBytes(), Optional.of("text/html"));
    }
    
    static Resource of(int status, byte[] content, Optional<String> contentType) {
      return new Resource(status, content, contentType);
    }
    
    public int status() {
      return status;
    }
    
    public byte[] content() {
      return content;
    }
    
    public Optional<String> contentType() {
      return contentType;
    }
  }
  
  private static final CloseableHttpClient httpclient = HttpClientBuilder.create()
      .setMaxConnTotal(200)
      .setMaxConnPerRoute(50)
      .build();
  
  public static void setTimeout(long milliseconds) {
    CONNECTION_TIMEOUT_MS = (int) milliseconds;
  }

  public static Optional<Resource> get(InetSocketAddress proxy, String url) throws MalformedURLException, IOException {
    HttpHost p = new HttpHost(proxy.getHostName(), proxy.getPort());
    RequestConfig.Builder config = RequestConfig.custom()
        .setProxy(p);
    HttpGet request = new HttpGet(url);
    return get(request, config);
  }

  private static Optional<Resource> get(HttpGet request, RequestConfig.Builder config) throws IOException {
    config.setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS);
    config.setConnectTimeout(CONNECTION_TIMEOUT_MS);
    config.setSocketTimeout(CONNECTION_TIMEOUT_MS);
    
    request.setConfig(config.build());
    
    try {
      CloseableHttpResponse response = httpclient.execute(request);
      try {
        if (response.getStatusLine().getStatusCode() == 200) {
          // Defaults to returning text/html as the content type.
          Optional<String> contentType = Optional.absent();
          if (response.containsHeader("Content-Type")) {
            contentType = Optional.of(response.getFirstHeader("Content-Type").getValue());
          }
          byte[] content = ByteStreams.toByteArray(response.getEntity().getContent()); 
          return Optional.of(Resource.of(200, content, contentType));
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


  public static Optional<Resource> get(String url) throws IOException {
    return get(new HttpGet(url), RequestConfig.custom());
  }
}