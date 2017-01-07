package com.kumbaya.router;

import com.kumbaya.router.Marshaller.TLV;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import junit.framework.TestCase;
import org.junit.Assert;

public class MarshallerTest extends TestCase {
  public void testMarhsalling() throws Exception {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Marshaller.marshall(stream, TLV.of(0xAB, "hello".getBytes()));
    TLV result = Marshaller.unmarshall(new ByteArrayInputStream(stream.toByteArray()));
    assertEquals(0xAB, result.type);
    Assert.assertArrayEquals("hello".getBytes(), result.content);
  }

  public void testEmptyArray() throws Exception {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    Marshaller.marshall(stream, TLV.of(0xAB, "".getBytes()));
    TLV result = Marshaller.unmarshall(new ByteArrayInputStream(stream.toByteArray()));
    assertEquals(0xAB, result.type);
    assertEquals(0, result.content.length);
  }
}
