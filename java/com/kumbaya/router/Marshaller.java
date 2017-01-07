package com.kumbaya.router;

import static com.kumbaya.router.TypeLengthValues.decode;
import static com.kumbaya.router.TypeLengthValues.encode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

class Marshaller {
  static void marshall(ByteArrayOutputStream stream, TLV data) throws IOException {
    stream.write(encode(data.type));
    stream.write(encode(data.content.length));
    stream.write(data.content);
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

  static TLV unmarshall(ByteArrayInputStream stream) {
    long type = decode(stream);
    int length = (int) decode(stream);

    ByteBuffer content = ByteBuffer.allocate(length);
    stream.read(content.array(), 0, length);

    return TLV.of(type, content.array());
  }
}
