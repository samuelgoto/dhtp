package com.kumbaya.common;

import java.io.IOException;

public interface Server {
  public void start() throws IOException;

  void stop() throws IOException;
}
