package com.kumbaya.android.client;

import java.util.concurrent.Executor;

import android.os.Handler;
import android.os.Looper;

public class Executors {
	public static Executor mainLooperExecutor() {
		return new Executor() {
	        private final Handler mHandler = new Handler(Looper.getMainLooper());

	        @Override
	        public void execute(Runnable command) {
	            mHandler.post(command);
	        }
	    };
	}
}
