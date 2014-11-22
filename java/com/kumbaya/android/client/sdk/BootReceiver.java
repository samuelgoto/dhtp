package com.kumbaya.android.client.sdk;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver{
	@SuppressWarnings("hiding")
	private static final String TAG = "BootReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Booting the Kumbaya service.");

		Intent i = new Intent(context, BackgroundService.class);
		context.startService(i);
	}
}