package com.kumbaya.android.client;

import java.util.concurrent.Executor;

import com.google.common.util.concurrent.ListenableFuture;
import com.kumbaya.android.R;
import com.kumbaya.android.client.sdk.BackgroundService;
import com.kumbaya.android.client.sdk.BackgroundService.LocalBinder;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;

public class BootActivity extends Activity {
	private static final String TAG = "BootActivity";
    private final Activity context = this;

	private final BroadcastReceiver bootstrapedReceiver =
			new BroadcastReceiver() {
		@Override
		public void onReceive(Context c, Intent intent) {
			Intent main = new Intent(context, MainActivity.class);
			startActivity(main);
		}
	};

	private final BroadcastReceiver updateReceiver =
			new BroadcastReceiver() {
		@Override
		public void onReceive(Context c, Intent intent) {
			String message = intent.getExtras().getString("message");

			final TextView text = (TextView) context.findViewById(
					R.id.boot_progress);
			text.setText(message);
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					text.setText("");
				}
			}, 4000);
		}
	};

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getActionBar().hide();
        
        setContentView(R.layout.booting_fragment);
        
        // Creating and starting the background services takes some time,
        // so do it off the UI thread.
		Log.i(TAG, "Kicking off the service creation");
		Log.i(TAG, "Starting service");

		registerReceiver(updateReceiver,
				new IntentFilter(BackgroundService.UPDATE_ACTION));

		registerReceiver(bootstrapedReceiver,
				new IntentFilter(BackgroundService.BOOTSTRAPED_ACTION));
		
		Intent intent = new Intent(context, BackgroundService.class);
		startService(intent);
		
		Log.i(TAG, "Done");
    }
	
	@Override
	protected void onDestroy() {
		Log.i(TAG, "Destroying the activity.");
		unregisterReceiver(updateReceiver);
		unregisterReceiver(bootstrapedReceiver);
		super.onDestroy();
	}
}
