package com.kumbaya.android.client;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.widget.TextView;

import com.kumbaya.android.R;

class BroadcastReceivers {
	static BroadcastReceiver updateReceiver(final int viewId) {
		return new BroadcastReceiver() {
			@Override
			public void onReceive(Context c, Intent intent) {
				String message = intent.getExtras().getString("message");

				final TextView text = (TextView)
						((Activity) c).findViewById(viewId);
				text.setText(message);
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						text.setText("");
					}
				}, 4000);
			}
		};
	}

	static BroadcastReceiver startActivity(final Class<?> clazz) {
		return new BroadcastReceiver() {
			@Override
			public void onReceive(Context c, Intent intent) {
				Intent main = new Intent(c, clazz);
				c.startActivity(main);
			}
		};
	}
}
