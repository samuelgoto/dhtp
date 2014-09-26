/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kumbaya.android.client;

import static com.kumbaya.android.client.CommonUtilities.DISPLAY_MESSAGE_ACTION;
import static com.kumbaya.android.client.CommonUtilities.EXTRA_MESSAGE;
import static com.kumbaya.android.client.CommonUtilities.SENDER_ID;
import static com.kumbaya.android.client.CommonUtilities.SERVER_URL;

import java.io.IOException;
import java.net.SocketAddress;

import org.eclipse.jetty.client.HttpClient;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.io.Tag;
import org.limewire.security.SecureMessage;
import org.limewire.security.SecureMessageCallback;

import com.google.android.gcm.GCMRegistrar;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.kumbaya.dht.Dht;
import com.kumbaya.dht.DhtModule;
import com.kumbaya.monitor.VarZModule;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

/**
 * Main UI for the demo app.
 */
public class DemoActivity extends Activity {
    TextView mDisplay;

    static class Foo {
    	public void bar() {
    		
    	}
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		
		Intent intent = new Intent(this, BackgroundService.class);
		startService(intent);
		
		checkNotNull(SERVER_URL, "SERVER_URL");
        checkNotNull(SENDER_ID, "SENDER_ID");

        setContentView(R.layout.main);
        mDisplay = (TextView) findViewById(R.id.display);
        registerReceiver(mHandleMessageReceiver,
                new IntentFilter(DISPLAY_MESSAGE_ACTION));
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            /*
             * Typically, an application registers automatically, so options
             * below are disabled. Uncomment them if you want to manually
             * register or unregister the device (you will also need to
             * uncomment the equivalent options on options_menu.xml).
             */
            /*
            case R.id.options_register:
                GCMRegistrar.register(this, SENDER_ID);
                return true;
            case R.id.options_unregister:
                GCMRegistrar.unregister(this);
                return true;
             */
            case R.id.options_clear:
                mDisplay.setText(null);
                return true;
            case R.id.options_exit:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mHandleMessageReceiver);
        super.onDestroy();
    }

    private void checkNotNull(Object reference, String name) {
        if (reference == null) {
            throw new NullPointerException(
                    getString(R.string.error_config, name));
        }
    }

    private final BroadcastReceiver mHandleMessageReceiver =
            new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String newMessage = intent.getExtras().getString(EXTRA_MESSAGE);
            mDisplay.append(newMessage + "\n");
        }
    };

}