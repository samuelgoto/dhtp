package com.kumbaya.www;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Optional;
import com.kumbaya.www.WorldWideWeb.Resource;
import java.net.InetAddress;
import org.junit.Ignore;
import org.junit.Test;

public class WorldWideWebTest {

  @Test
  public void testFetchingBasic() throws Exception {
    Optional<Resource> result = WorldWideWeb.get("http://sgo.to");
    assertTrue(result.isPresent());
  }

  @Test
  public void testHanging() throws Exception {
    Optional<Resource> result =
        WorldWideWeb.get("https://cloud.google.com/compute/docs/startupscript");
    assertTrue(result.isPresent());
  }

  @Ignore("depends on an external resource")
  @Test
  public void testTimingOut() throws Exception {
    Optional<Resource> result = WorldWideWeb.get("http://104.199.183.97/index.php");
    assertFalse(result.isPresent());
  }

  @Test
  public void testDnsFails() throws Exception {
    WorldWideWeb.get("http://thisdomainshouldnotexithopefully.com");
  }

  @Test
  public void testDnsResolution() throws Exception {
    String url = "http://sgo.to";

    Optional<Resource> foo = WorldWideWeb.get(InetAddress.getByName("192.30.252.153"), url);

    assertTrue(foo.isPresent());
  }
}
