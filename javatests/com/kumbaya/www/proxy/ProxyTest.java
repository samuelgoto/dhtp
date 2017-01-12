package com.kumbaya.www.proxy;

import java.net.MalformedURLException;
import junit.framework.TestCase;

public class ProxyTest extends TestCase {
  public void testAssemblingProxied() throws MalformedURLException {
    assertEquals("http://localhost:6060/helloworld", 
        Proxy.MyProxyServlet.assemble("http://localhost:6060/helloworld"));
  }

  public void testAssemblingProxied_ofSpecialCase() throws MalformedURLException {
    assertEquals("http://kumbaya.io/helloworld", 
        Proxy.MyProxyServlet.assemble("http://kumbaya.io/helloworld"));
  }

  public void testAssemblingRedirector() throws MalformedURLException {
    assertEquals("http://localhost:6060/helloworld",
        Proxy.MyProxyServlet.assemble("http://localhost-6060.kumbaya.io:8080/helloworld"));
  }

  public void testAssemblingRedirector_noPort() throws MalformedURLException {
    assertEquals("http://localhost/helloworld",
        Proxy.MyProxyServlet.assemble("http://localhost.kumbaya.io:8080/helloworld"));
  }
  public void testAssemblingRedirector_subDomains() throws MalformedURLException {
    assertEquals("http://photos.sgo.to/helloworld",
        Proxy.MyProxyServlet.assemble("http://photos.sgo.to.kumbaya.io:8080/helloworld"));
  }
  public void testAssemblingRedirector_complexPath() throws MalformedURLException {
    assertEquals("http://photos.sgo.to/dir1/dir2/file.html#fragment",
        Proxy.MyProxyServlet.assemble("http://photos.sgo.to.kumbaya.io:8080/dir1/dir2/file.html#fragment"));
  }
}
