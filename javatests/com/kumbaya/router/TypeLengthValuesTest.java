package com.kumbaya.router;

import junit.framework.TestCase;
import org.junit.Test;

import static com.kumbaya.router.TypeLengthValues.encode;

import java.io.IOException;

import static com.kumbaya.router.TypeLengthValues.decode;

public class TypeLengthValuesTest extends TestCase {
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
  
  public void testDecoding() throws IOException {
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
}


