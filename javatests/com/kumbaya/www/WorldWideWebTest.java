package com.kumbaya.www;

import com.google.common.base.Optional;
import com.kumbaya.www.WorldWideWeb.Resource;
import junit.framework.TestCase;

public class WorldWideWebTest extends TestCase {
  public void testFetchingBasic() throws Exception {
     Optional<Resource> result = WorldWideWeb.get("http://sgo.to");
     assertTrue(result.isPresent());
  }
  
  public void testHanging() throws Exception {
    Optional<Resource> result = WorldWideWeb.get(
        "https://cloud.google.com/compute/docs/startupscript");
    assertTrue(result.isPresent());
  }
  
  public void testTimingOut() throws Exception {
    Optional<Resource> result = WorldWideWeb.get(
        "http://104.199.183.97/index.php");
    assertFalse(result.isPresent()); 
  }
  
  public void testDnsFails() throws Exception {
    WorldWideWeb.get("http://thisdomainshouldnotexithopefully.com");
  }
}
