package com.kumbaya.router;

import com.google.common.collect.ImmutableList;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

public class TypeLengthValueTest extends TestCase {

  // Follows this:
  // http://named-data.net/doc/ndn-tlv/tlv.html

  private byte[] encode(long value) {
    if (Long.compareUnsigned(value, 253) < 0) {
      ByteBuffer buffer = ByteBuffer.allocate(1);
      buffer.put((byte) (value & 0xFFL));
      return buffer.array();
    } else if ((value & 0xFFFFFFFFFFFF0000L) == 0) {
      ByteBuffer buffer = ByteBuffer.allocate(3);
      buffer.put((byte) 253);
      buffer.putShort((short) (value & 0xFFFFL));
      return buffer.array();
    } else if ((value & 0xFFFFFFFF00000000L) == 0) {
      ByteBuffer buffer = ByteBuffer.allocate(5);
      buffer.put((byte) 254);
      buffer.putInt((int)(value & 0xFFFFFFFFL));
      return buffer.array();
    } else {
      ByteBuffer buffer = ByteBuffer.allocate(9);
      buffer.put((byte) 255);
      buffer.putLong(value);
      return buffer.array();
    }
  }
  
  private long decode(byte[] value) {
    return decode(new ByteArrayInputStream(value));
  }
  
  private long decode(ByteArrayInputStream value) {
    int head = value.read();
    if (head < 253) {
      return head;
    } else if (head == 253) {
      return 
          (((long) value.read()) << 8) | 
          value.read();
    } else if (head == 254) {
      return
          (((long) value.read()) << 24) | 
          ((long) value.read()) << 16 |
          ((long) value.read()) << 8  |
          value.read();
    } else {
      return 
          ((long) value.read()) << 56 |
          ((long) value.read()) << 48 |
          ((long) value.read()) << 40 |
          ((long) value.read()) << 32 |
          ((long) value.read()) << 24 |
          ((long) value.read()) << 16 |
          ((long) value.read()) << 8  |
          value.read();
    }
  }
  
  
  private static class TLV {
    private final long type;
    private final byte[] content;
    
    TLV(long type, byte[] content) {
      this.type = type;
      this.content = content;
    }
    
    static TLV of(long type, byte[] content) {
      return new TLV(type, content);
    }
  }
  
  
  @Test
  public void testEncoding() {
    // Values that fit into a single byte (except 253, 254 and 255 which are reserved).    
    assertEquals((byte) 1, encode(0xABL).length);
    assertEquals((byte) 0xABL, encode(0xABL)[0]);
    
    // Values that fit into two bytes.
    assertEquals((byte) 3, encode(0xABCDL).length);
    assertEquals((byte) 253, encode(0xABCDL)[0]);
    assertEquals((byte) 0xAB, encode(0xABCDL)[1]);
    assertEquals((byte) 0xCD, encode(0xABCDL)[2]);

    // Values that fit into four bytes.
    assertEquals((byte) 5, encode(0xABCDEF01L).length);
    assertEquals((byte) 254, encode(0xABCDEF01L)[0]);
    assertEquals((byte) 0xAB, encode(0xABCDEF01L)[1]);
    assertEquals((byte) 0xCD, encode(0xABCDEF01L)[2]);
    assertEquals((byte) 0xEF, encode(0xABCDEF01L)[3]);
    assertEquals((byte) 0x01, encode(0xABCDEF01L)[4]);
    
    // Values that fit into eight bytes.
    assertEquals((byte) 9, encode(0xABCDEF01CAFEBABEL).length);
    assertEquals((byte) 255, encode(0xABCDEF01CAFEBABEL)[0]);
    assertEquals((byte) 0xAB, encode(0xABCDEF01CAFEBABEL)[1]);
    assertEquals((byte) 0xCD, encode(0xABCDEF01CAFEBABEL)[2]);
    assertEquals((byte) 0xEF, encode(0xABCDEF01CAFEBABEL)[3]);
    assertEquals((byte) 0x01, encode(0xABCDEF01CAFEBABEL)[4]);
    assertEquals((byte) 0xCA, encode(0xABCDEF01CAFEBABEL)[5]);
    assertEquals((byte) 0xFE, encode(0xABCDEF01CAFEBABEL)[6]);
    assertEquals((byte) 0xBA, encode(0xABCDEF01CAFEBABEL)[7]);
    assertEquals((byte) 0xBE, encode(0xABCDEF01CAFEBABEL)[8]);
  }
  
  public void testReservedValues() {
    // Reserved values
    
    // 253 (reserved value)
    assertEquals((byte) 3, encode(253).length);
    assertEquals((byte) 253, encode(253)[0]);
    assertEquals((byte) 0, encode(253)[1]);
    assertEquals((byte) 253, encode(253)[2]);
    
    // 254 (reserved value)
    assertEquals((byte) 3, encode(254).length);
    assertEquals((byte) 253, encode(254)[0]);
    assertEquals((byte) 0, encode(254)[1]);
    assertEquals((byte) 254, encode(254)[2]);
    
    // 255 (reserved value)
    assertEquals((byte) 3, encode(255).length);
    assertEquals((byte) 253, encode(255)[0]);
    assertEquals((byte) 0, encode(255)[1]);
    assertEquals((byte) 255, encode(255)[2]);
  }
  
  public void testBoundaries() {

    // Boundaries
    
    // 0
    assertEquals((byte) 1, encode(0).length);
    assertEquals((byte) 0, encode(0)[0]);

    // 1
    assertEquals((byte) 1, encode(1).length);
    assertEquals((byte) 1, encode(1)[0]);
    
    // max long
    
    assertEquals((byte) 9, encode(0xFFFFFFFFFFFFFFFFL).length);
    assertEquals((byte) 255, encode(0xFFFFFFFFFFFFFFFFL)[0]);
    assertEquals((byte) 0xFF, encode(0xFFFFFFFFFFFFFFFFL)[1]);
    assertEquals((byte) 0xFF, encode(0xFFFFFFFFFFFFFFFFL)[2]);
    assertEquals((byte) 0xFF, encode(0xFFFFFFFFFFFFFFFFL)[3]);
    assertEquals((byte) 0xFF, encode(0xFFFFFFFFFFFFFFFFL)[4]);
    assertEquals((byte) 0xFF, encode(0xFFFFFFFFFFFFFFFFL)[5]);
    assertEquals((byte) 0xFF, encode(0xFFFFFFFFFFFFFFFFL)[6]);
    assertEquals((byte) 0xFF, encode(0xFFFFFFFFFFFFFFFFL)[7]);
    assertEquals((byte) 0xFF, encode(0xFFFFFFFFFFFFFFFFL)[8]);   

    // Upper and lower neighbors of reserved values
    assertEquals((byte) 1, encode(252).length);
    assertEquals((byte) 252, encode(252)[0]);
    
    assertEquals((byte) 3, encode(256).length);
    assertEquals((byte) 253, encode(256)[0]);
    assertEquals((byte) ((256 & 0xFF00L) >> 8), encode(256)[1]);
    assertEquals((byte) ((256 & 0x00FFL) >> 8), encode(256)[2]);

    // Last number that fit into 2 bytes
    assertEquals((byte) 3, encode(0xFFFFL).length);
    assertEquals((byte) 253, encode(0xFFFFL)[0]);
    assertEquals((byte) 0xFF, encode(0xFFFFL)[1]);
    assertEquals((byte) 0xFF, encode(0xFFFFL)[2]);

    // First number that requires more than 2 bytes
    assertEquals((byte) 5, encode(0x10000L).length);
    assertEquals((byte) 254, encode(0x10000L)[0]);
    assertEquals((byte) 0x00, encode(0x10000L)[1]);
    assertEquals((byte) 0x01, encode(0x10000L)[2]);
    assertEquals((byte) 0x00, encode(0x10000L)[3]);
    assertEquals((byte) 0x00, encode(0x10000L)[4]);

    // Last number that fit into 4 bytes
    assertEquals((byte) 5, encode(0xFFFFFFFFL).length);
    assertEquals((byte) 254, encode(0xFFFFFFFFL)[0]);
    assertEquals((byte) 0xFF, encode(0xFFFFFFFFL)[1]);
    assertEquals((byte) 0xFF, encode(0xFFFFFFFFL)[2]);
    assertEquals((byte) 0xFF, encode(0xFFFFFFFFL)[3]);
    assertEquals((byte) 0xFF, encode(0xFFFFFFFFL)[4]);

    // First number that requires more than 4 bytes
    assertEquals((byte) 9, encode(0x100000000L).length);
    assertEquals((byte) 255, encode(0x100000000L)[0]);
    assertEquals((byte) 0x00, encode(0x100000000L)[1]);
    assertEquals((byte) 0x00, encode(0x100000000L)[2]);
    assertEquals((byte) 0x00, encode(0x100000000L)[3]);
    assertEquals((byte) 0x01, encode(0x100000000L)[4]);
    assertEquals((byte) 0x00, encode(0x100000000L)[5]);
    assertEquals((byte) 0x00, encode(0x100000000L)[6]);
    assertEquals((byte) 0x00, encode(0x100000000L)[7]);
    assertEquals((byte) 0x00, encode(0x100000000L)[8]);
  }
  
  public void testDecoding() {
    // random values
    assertEquals(0xAB, decode(encode(0xAB)));
    assertEquals(0xABCD, decode(encode(0xABCD)));
    assertEquals(0xABCDEF, decode(encode(0xABCDEF)));
    assertEquals(0xABCDEF01, decode(encode(0xABCDEF01)));
    assertEquals(0xABCDEF01CAFEBABEL, decode(encode(0xABCDEF01CAFEBABEL)));   
    // min value
    assertEquals(0, decode(encode(0)));
    assertEquals(1, decode(encode(1)));
    // max value
    assertEquals(0xFFFFFFFFFFFFFFFFL, decode(encode(0xFFFFFFFFFFFFFFFFL)));
    // Last number that fit into 2 bytes
    assertEquals(0xFFFFL, decode(encode(0xFFFFL)));    
    // First number that requires more than 2 bytes
    assertEquals(0x10000L, decode(encode(0x10000L)));
    // Last number that fit into 4 bytes
    assertEquals(0xFFFFFFFFL, decode(encode(0xFFFFFFFFL)));
    // First number that requires more than 4 bytes
    assertEquals(0x100000000L, decode(encode(0x100000000L)));
    // reserved values
    assertEquals(253, decode(encode(253)));
    assertEquals(254, decode(encode(254)));
    assertEquals(255, decode(encode(255)));
  }
  
  private byte[] marshall(List<TLV> data) throws IOException {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    
    for (TLV entry : data) {
      byte[] typeTLV  = encode(entry.type); 
      byte[] lengthTLV  = encode(entry.content.length);
      
      stream.write(typeTLV);
      stream.write(lengthTLV);
      stream.write(entry.content);
    }
    
    
    return stream.toByteArray();
  }
  
  private List<TLV> unmarshall(byte[] stream) {
    return unmarshall(new ByteArrayInputStream(stream));
  }
  
  private List<TLV> unmarshall(ByteArrayInputStream stream) {
    List<TLV> result = new ArrayList<TLV>();
    while (stream.available() != 0) {
      long type = decode(stream);
      int length = (int) decode(stream);
      ByteBuffer content = ByteBuffer.allocate(length);
      stream.read(content.array(), 0, length);
      result.add(TLV.of(type, content.array()));
    }
    
    return result;
  }
  
  
  public void testMarshallingAndUnmarshalling() throws IOException {
    TLV data = TLV.of(0xAB, "hello world".getBytes());    
    List<TLV> result = unmarshall(marshall(ImmutableList.of(data)));
    assertEquals(1, result.size());
    assertEquals(data.type, result.get(0).type);
    Assert.assertArrayEquals(data.content, result.get(0).content);   
  }

  public void testMarshallingAndUnmarshalling_multipleFields() throws IOException {
    List<TLV> result = unmarshall(marshall(ImmutableList.of(
        TLV.of(0xAB, "hello".getBytes()),
        TLV.of(0xCD, "world".getBytes()))));
    assertEquals(2, result.size());
    assertEquals(0xAB, result.get(0).type);
    Assert.assertArrayEquals("hello".getBytes(), result.get(0).content); 
    assertEquals(0xCD, result.get(1).type);
    Assert.assertArrayEquals("world".getBytes(), result.get(1).content); 
  }

  public void testMarshallingAndUnmarshalling_longText() throws IOException {
    String text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse rhoncus, "
        + "libero at congue varius, odio arcu rutrum velit, et laoreet arcu ligula sit amet dui. "
        + "Suspendisse ornare nunc quis purus molestie, a finibus purus tempor. Donec id diam sed"
        + " mi fermentum tempus eget et nisl. Vivamus vestibulum tristique nisi vitae dignissim. "
        + "Nulla vehicula nisi nec est convallis aliquam. Sed commodo, nunc placerat posuere posu"
        + "ere, enim nunc venenatis orci, id pellentesque leo nunc eu felis. Donec eu mi dapibus,"
        + " rhoncus nisl varius, auctor felis. Aenean convallis, ligula eget bibendum tristique, "
        + "erat justo feugiat nisi, eu porta nibh quam non neque. Nulla tempus placerat neque, et"
        + " vestibulum eros molestie quis. Integer porttitor vestibulum facilisis. Vestibulum int"
        + "erdum interdum tincidunt. Ut mauris ligula, pretium nec dui ac, convallis porta ipsum."
        + " Proin blandit enim eget risus bibendum, vitae imperdiet magna iaculis. Nullam malesua"
        + "da enim eget risus elementum, a malesuada magna commodo. Sed tempus porttitor leo. Pro"
        + "in sollicitudin dictum nibh, sit amet dignissim diam commodo id.";
    
    TLV data = TLV.of(0xAB, text.getBytes());    
    List<TLV> result = unmarshall(marshall(ImmutableList.of(data)));
    assertEquals(1, result.size());
    assertEquals(data.type, result.get(0).type);
    Assert.assertArrayEquals(data.content, result.get(0).content);
  }
}


