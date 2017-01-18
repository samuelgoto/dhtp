package com.kumbaya.common;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.kumbaya.common.Flags.Flag;
import junit.framework.TestCase;

public class FlagsTest extends TestCase {

  private static class Foo {
    @Inject @Flag("foo") int foo;
    @Inject @Flag("bar") String bar;
  }
  
  public void testProvision() {
    final String[] args = {"--foo=12345", "--bar=hostname:foobar"};
    
    Foo result = Guice.createInjector(Flags.asModule(args)).getInstance(Foo.class);

    assertEquals(12345, result.foo);
    assertEquals("hostname:foobar", result.bar);
  }
  
}
