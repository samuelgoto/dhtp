package com.kumbaya.www.gateway;

import com.kumbaya.common.Server;
import java.io.IOException;
import java.net.SocketAddress;

public class Gateway implements Server {
  @Override
  public void bind(SocketAddress address) throws IOException {
  }

  @Override
  public void close() {
  }
}
