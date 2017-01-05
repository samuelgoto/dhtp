package com.kumbaya.router;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;

class TypeLengthValues {

  // Follows this:
  // http://named-data.net/doc/ndn-tlv/tlv.html

  static byte[] encode(long value) {
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
  
  static long decode(byte[] value) {
    return decode(new ByteArrayInputStream(value));
  }
  
  static long decode(ByteArrayInputStream value) {
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
}
