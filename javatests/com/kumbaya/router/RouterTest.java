package com.kumbaya.router;

import com.google.inject.Guice;
import com.kumbaya.common.Flags;
import org.junit.Test;

public class RouterTest {

  @Test
  public void startsAndStops() throws Exception {
    Router router = create();
    router.start();
    router.stop();
  }

  Router create() {
    return Guice
        .createInjector(new Router.Module(),
            Flags.asModule(
                new String[] {"--forwarding=localhost:8080", "--host=localhost", "--port=8080"}))
        .getInstance(Router.class);
  }
}
