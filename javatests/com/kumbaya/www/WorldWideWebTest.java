package com.kumbaya.www;

import com.google.common.base.Optional;
import junit.framework.TestCase;

public class WorldWideWebTest extends TestCase {
  public void testFetchingBasic() throws Exception {
     Optional<String> result = WorldWideWeb.get("http://sgo.to");
     assertTrue(result.isPresent());
  }
  
  public void testHanging() throws Exception {
    Optional<String> result = WorldWideWeb.get(
        "https://cloud.google.com/compute/docs/startupscript");
    assertTrue(result.isPresent());
  }
  
  public void testDnsFails() throws Exception {
    WorldWideWeb.get("http://thisdomainshouldnotexithopefully.com");
  }
}
