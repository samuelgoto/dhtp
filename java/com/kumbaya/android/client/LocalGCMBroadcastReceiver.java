package com.kumbaya.android.client;

import android.content.Context;

import com.google.android.gcm.GCMBroadcastReceiver;

public class LocalGCMBroadcastReceiver extends GCMBroadcastReceiver {
    @Override
	protected String getGCMIntentServiceClassName(Context context) { 
		return "com.kumbaya.android.client.GCMIntentService"; 
	}
}
