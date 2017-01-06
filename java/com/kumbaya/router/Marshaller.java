package com.kumbaya.router;

import static com.kumbaya.router.TypeLengthValues.decode;
import static com.kumbaya.router.TypeLengthValues.encode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

class Marshaller {

  static byte[] marshall(List<TLV> data) throws IOException {
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
  
  static List<TLV> unmarshall(byte[] stream) {
    return unmarshall(new ByteArrayInputStream(stream));
  }
  
  static List<TLV> unmarshall(ByteArrayInputStream stream) {
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
  
  static class TLV {
    final long type;
    final byte[] content;
    
    TLV(long type, byte[] content) {
      this.type = type;
      this.content = content;
    }
    
    static TLV of(long type, byte[] content) {
      return new TLV(type, content);
    }
  }
}
