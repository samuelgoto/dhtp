package com.kumbaya.common.testing;

import com.google.common.base.Optional;

public abstract class Supplier<T> {
  private Optional<T> instance = Optional.absent();
  
  public T get() {
    if (instance.isPresent()) {
      return instance.get();
    } else {
      instance = Optional.of(build());
      return instance.get();
    }      
  }
  
  public Supplier<T> clear() {
    this.instance = Optional.absent();
    return this;
  }
  
  protected abstract T build();
}
