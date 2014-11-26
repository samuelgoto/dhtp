package com.kumbaya.android.client;

import com.kumbaya.android.R;
import com.kumbaya.android.client.sdk.BackgroundService;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

public class BootActivity extends Activity {
	private static final String TAG = "BootActivity";
    private final Activity context = this;

	private final BroadcastReceiver bootReceiver =
			BroadcastReceivers.startActivity(MainActivity.class);

	private final BroadcastReceiver updateReceiver =
			BroadcastReceivers.updateReceiver(R.id.boot_progress);
	
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

		registerReceiver(bootReceiver,
				new IntentFilter(BackgroundService.BOOTSTRAPED_ACTION));
		
		Intent intent = new Intent(context, BackgroundService.class);
		startService(intent);
		
		Log.i(TAG, "Done");
    }
	
	@Override
	protected void onDestroy() {
		Log.i(TAG, "Destroying the activity.");
		unregisterReceiver(updateReceiver);
		unregisterReceiver(bootReceiver);
		super.onDestroy();
	}
}
