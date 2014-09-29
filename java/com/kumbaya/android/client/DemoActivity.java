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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.exceptions.NotBootstrappedException;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.io.Tag;
import org.limewire.security.SecureMessage;
import org.limewire.security.SecureMessageCallback;

import com.google.android.gcm.GCMRegistrar;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.kumbaya.android.R;
import com.kumbaya.android.client.BackgroundService.LocalBinder;
import com.kumbaya.dht.Dht;
import com.kumbaya.dht.DhtModule;
import com.kumbaya.monitor.VarZModule;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

/**
 * Main UI for the demo app.
 */
public class DemoActivity extends Activity {
    private final ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder b) {
            LocalBinder binder = (LocalBinder) b;
            service = Optional.of(binder.getService());
            mDisplay.append(service.get().toString());
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
		}
    };

    private Optional<BackgroundService> service = Optional.absent();
    TextView mDisplay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		
		Intent intent = new Intent(this, BackgroundService.class);
		startService(intent);
		
		Intent i = new Intent(this, BackgroundService.class);
        bindService(i, connection, Context.BIND_AUTO_CREATE);
		
		checkNotNull(SERVER_URL, "SERVER_URL");
        checkNotNull(SENDER_ID, "SENDER_ID");

        setContentView(R.layout.main);

        mDisplay = (TextView) findViewById(R.id.display);
        registerReceiver(mHandleMessageReceiver,
                new IntentFilter(DISPLAY_MESSAGE_ACTION));
        
        EditText editText = (EditText) findViewById(R.id.search);
        editText.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int keyCode,
					KeyEvent event) {
                if (keyCode == EditorInfo.IME_ACTION_SEARCH ||
                	(event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                	
                	new AsyncTask<String, Void, List<String>>() {
						@Override
						protected List<String> doInBackground(String... params) {
		                	try {
								return service.get().get(params[0], 5000);
							} catch (InterruptedException e) {
								return Lists.newArrayList();
							} catch (ExecutionException e) {
								return Lists.newArrayList();
							} catch (TimeoutException e) {
								return Lists.newArrayList();
							} catch (NotBootstrappedException e) {
								return Lists.newArrayList();
							}
						}
						
						@Override
					    protected void onPostExecute(List<String> result) {
							mDisplay.setText("");
							for (String entry : result) {
								mDisplay.append(entry);
							}
					    }
                	}.execute(v.getText().toString());
                	
                	return true;
                }
                return false;
			}
        });
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
            case R.id.options_clear:
                mDisplay.setText(null);
                return true;
            case R.id.options_refresh:
            	mDisplay.setText(service.get().toString());
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
        unbindService(connection);
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