package com.kumbaya.dht;

import com.google.inject.Inject;
import com.google.inject.Provider;

import org.limewire.mojito.Context;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.io.MessageDispatcherFactory;

class MessageDispatcherFactoryImpl implements MessageDispatcherFactory {
    private final Provider<MessageDispatcher> result;
    
    @Inject
    MessageDispatcherFactoryImpl(Provider<MessageDispatcher> result) {
    	this.result = result;
    }
    
    @Override
    public MessageDispatcher create(Context context) {
        return result.get();
    }
}