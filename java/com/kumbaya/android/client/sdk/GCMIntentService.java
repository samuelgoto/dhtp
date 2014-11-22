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
package com.kumbaya.android.client.sdk;

import static com.kumbaya.android.client.sdk.CommonUtilities.SENDER_ID;
import static com.kumbaya.android.client.sdk.CommonUtilities.displayMessage;

import java.net.InetSocketAddress;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.kumbaya.android.R;
import com.kumbaya.android.client.sdk.BackgroundService.LocalBinder;

/**
 * IntentService responsible for handling GCM messages.
 */
public class GCMIntentService extends GCMBaseIntentService {

    @SuppressWarnings("hiding")
    private static final String TAG = "GCMIntentService";

    public GCMIntentService() {
        super(SENDER_ID);
    }

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int result = super.onStartCommand(intent, flags, startId);
		return result;
	}

	BackgroundService mService;
	
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            // mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // mBound = false;
        }
    };
    
    @Override
    protected void onRegistered(Context context, String registrationId) {
        Log.i(TAG, "Device registered: regId = " + registrationId);
        displayMessage(context, getString(R.string.gcm_registered,
                registrationId));
        ServerUtilities.register(context, registrationId);
    }

    @Override
    protected void onUnregistered(Context context, String registrationId) {
        Log.i(TAG, "Device unregistered locally.");
        displayMessage(context, getString(R.string.gcm_unregistered));
        ServerUtilities.unregister(context, registrationId);
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
        Log.i(TAG, "Received message.");
    	if (intent == null) {
    		return;
    	}
        Bundle extras = intent.getExtras();
        final String message = extras.get("debug").toString();
        final byte[] body = Base64.decode(extras.getString("body").getBytes(), 0);
        final String hostname = extras.getString("X-Node-Host");
        final int port = Integer.parseInt(extras.getString("X-Node-Port"));
        // final String message = getString(R.string.gcm_message);
        // displayMessage(context, message);
        // notifies user
        // generateNotification(context, message);
        
        // NOTE(goto): is there a more efficient way to do this?
		Intent i = new Intent(this, BackgroundService.class);
        ServiceConnection connection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
	            LocalBinder binder = (LocalBinder) service;
	            BackgroundService dht = binder.getService();
				InetSocketAddress src = InetSocketAddress.createUnresolved(
						hostname, Integer.valueOf(port));
		        dht.handleMessage(src, body);
		        unbindService(this);
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
			}
        };
        bindService(i, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDeletedMessages(Context context, int total) {
        Log.i(TAG, "Received deleted messages notification");
        String message = getString(R.string.gcm_deleted, total);
        displayMessage(context, message);
        // notifies user
        generateNotification(context, message);
    }

    @Override
    public void onError(Context context, String errorId) {
        Log.i(TAG, "Received error: " + errorId);
        displayMessage(context, getString(R.string.gcm_error, errorId));
    }

    @Override
    protected boolean onRecoverableError(Context context, String errorId) {
        // log message
        Log.i(TAG, "Received recoverable error: " + errorId);
        displayMessage(context, getString(R.string.gcm_recoverable_error,
                errorId));
        return super.onRecoverableError(context, errorId);
    }

    /**
     * Issues a notification to inform the user that server has sent a message.
     */
    private static void generateNotification(Context context, String message) {
        // int icon = R.drawable.abc_ab_share_pack_holo_dark;
        // long when = System.currentTimeMillis();
        // NotificationManager notificationManager = (NotificationManager)
        //        context.getSystemService(Context.NOTIFICATION_SERVICE);
        // Notification notification = new Notification(icon, message, when);
        // String title = context.getString(R.string.app_name);
        // Intent notificationIntent = new Intent(context, DemoActivity.class);
        // set intent so it does not start a new activity
        // notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
        //        Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // PendingIntent intent =
        ///        PendingIntent.getActivity(context, 0, notificationIntent, 0);
        // notification.setLatestEventInfo(context, title, message, intent);
        // notification.flags |= Notification.FLAG_AUTO_CANCEL;
        // notificationManager.notify(0, notification);
    }
}
