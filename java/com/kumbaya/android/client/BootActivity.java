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
	private final Executor executor = Executors.mainLooperExecutor();
    private final Activity context = this;

	private final ServiceConnection connection = new ServiceConnection() {
        @Override
		public void onServiceConnected(ComponentName name, IBinder b) {
			LocalBinder binder = (LocalBinder) b;
			final BackgroundService service = binder.getService();
			// TODO(goto): deal with timeouts.
			service.waitForBootstrap().addListener(new Runnable() {
				@Override
				public void run() {
					Log.i(TAG, "Bootstraped!");

					// Announcing ourselves to the network.
					TelephonyManager manager = (TelephonyManager) getSystemService(
							Context.TELEPHONY_SERVICE);
					String phoneNumber = manager.getLine1Number();
					String countryCode = manager.getSimCountryIso().toUpperCase();
					phoneNumber = countryCode + " " + phoneNumber;
					// TODO(goto): canonicalize the phone number serialization and
					// store a different value here.
					ListenableFuture<Void> result = service.put(
							phoneNumber, phoneNumber);

					final TextView bootStatus = (TextView) context.findViewById(
							R.id.boot_status);
					bootStatus.setText("Bootstrapped! Now announcing ourselves...");

					result.addListener(new Runnable() {
						@Override
						public void run() {
							Log.i(TAG, "Announced!");
							Intent intent = new Intent(context, MainActivity.class);
							startActivity(intent);
						}
					}, executor);
				}
			}, executor);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
		}
	};

	private final BroadcastReceiver updateReceiver =
			new BroadcastReceiver() {
		@Override
		public void onReceive(Context c, Intent intent) {
			String message = intent.getExtras().getString("type");
			String origin = intent.getExtras().getString("origin");

			final TextView text = (TextView) context.findViewById(
					R.id.boot_progress);
			text.setText(message + " from: " + origin);
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
        new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				startService();
			}
        }, 100);
    }
	
	private void startService() {
		Log.i(TAG, "Starting service");
		registerReceiver(updateReceiver,
				new IntentFilter(BackgroundService.UPDATE_ACTION));

		Intent intent = new Intent(context, BackgroundService.class);
		startService(intent);
		
		bind();

		Log.i(TAG, "Done");
	}
	
	private void bind() {
		Intent i = new Intent(context, BackgroundService.class);
		bindService(i, connection, Context.BIND_AUTO_CREATE);
	}
    
	@Override
	protected void onResume() {
	    super.onResume();
	    Log.d(TAG, "onResume");
	    bind();
	}

	@Override
	protected void onPause() {
	    super.onPause();
	    Log.d(TAG, "onPause");
	    unbindService(connection);
	}
	
	@Override
	protected void onDestroy() {
		Log.i(TAG, "Destroying the activity.");
		unregisterReceiver(updateReceiver);
		unbindService(connection);
		super.onDestroy();
	}
}
