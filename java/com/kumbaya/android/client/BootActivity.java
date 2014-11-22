package com.kumbaya.android.client;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;

public class BootActivity extends Activity {
	private static final String TAG = "DemoActivity";
    private final Activity context = this;

	private final ServiceConnection connection = new ServiceConnection() {
        @Override
		public void onServiceConnected(ComponentName name, IBinder b) {
			LocalBinder binder = (LocalBinder) b;
			final BackgroundService service = binder.getService();
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

					// TODO(goto): moves this to the runner instead.
					Intent intent = new Intent(context, DemoActivity.class);
					startActivity(intent);

					result.addListener(new Runnable() {
						@Override
						public void run() {
						}
					}, MoreExecutors.sameThreadExecutor());
				}
			}, MoreExecutors.sameThreadExecutor());
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
        setContentView(R.layout.booting_fragment);
        
		registerReceiver(updateReceiver,
				new IntentFilter(BackgroundService.UPDATE_ACTION));
        
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				Intent intent = new Intent(context, BackgroundService.class);
				startService(intent);

				Intent i = new Intent(context, BackgroundService.class);
				bindService(i, connection, Context.BIND_AUTO_CREATE);
				return null;
			}
		}.execute();        
    }
    
	@Override
	protected void onDestroy() {
		Log.i(TAG, "Destroying the activity.");
		unregisterReceiver(updateReceiver);
		super.onDestroy();
	}
}
