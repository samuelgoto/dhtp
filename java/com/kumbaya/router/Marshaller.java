package com.kumbaya.router;

import static com.kumbaya.router.TypeLengthValues.decode;
import static com.kumbaya.router.TypeLengthValues.encode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;

class Marshaller {
  static void marshall(OutputStream stream, TLV data) throws IOException {
    ByteStreams.copy(new ByteArrayInputStream(encode(data.type)), stream);
    ByteStreams.copy(new ByteArrayInputStream(encode(data.content.length)), stream);
    ByteStreams.copy(new ByteArrayInputStream(data.content), stream);
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

  static TLV unmarshall(InputStream stream) throws IOException {
    long type = decode(stream);
    int length = (int) decode(stream);

    byte[] content = ByteStreams.toByteArray(ByteStreams.limit(stream, length));

    Preconditions.checkArgument(content.length == length, 
        "Stream length doesn't match declared length");
    
    return TLV.of(type, content);
  }
}
