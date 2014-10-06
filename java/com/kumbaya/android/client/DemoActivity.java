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

import static com.kumbaya.android.client.CommonUtilities.SENDER_ID;
import static com.kumbaya.android.client.CommonUtilities.SERVER_URL;

import java.util.List;
import java.util.concurrent.Executor;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.kumbaya.android.R;
import com.kumbaya.android.client.BackgroundService.LocalBinder;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main UI for the demo app.
 */
public class DemoActivity extends FragmentActivity {
    private static final String TAG = "DemoActivity";

    private final Executor executor = Executors.mainLooperExecutor();
    
	private final ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder b) {
			LocalBinder binder = (LocalBinder) b;
			service = Optional.of(binder.getService());
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
		}
	};

	private Optional<BackgroundService> service = Optional.absent();
	PagerAdapter mDemoCollectionPagerAdapter;
	ViewPager mViewPager;
	private CreateFragment createFragment;
	private DebugFragment debugFragment;
	private SearchFragment searchFragment;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        Log.i(TAG, "Creating the activity.");
		
		checkNotNull(SERVER_URL, "SERVER_URL");
		checkNotNull(SENDER_ID, "SENDER_ID");

		setContentView(R.layout.main);
		
		createFragment = new CreateFragment();
		debugFragment = new DebugFragment();
		searchFragment = new SearchFragment();

		Fragment fragments[] = {
				createFragment,
				searchFragment, 
				debugFragment};

        mDemoCollectionPagerAdapter =
                new PagerAdapter(getSupportFragmentManager(), fragments);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mDemoCollectionPagerAdapter);
        mViewPager.setCurrentItem(1);
        mViewPager.setPageTransformer(true, new ZoomOutPageTransformer());
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
            	if (position == 2 && service.isPresent()) {
        			debugFragment.setText(service.get().toString());
            	}
            }
        });

        Intent intent = new Intent(this, BackgroundService.class);
		startService(intent);

		Intent i = new Intent(this, BackgroundService.class);
		bindService(i, connection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);

		final MenuItem menuItem = menu.findItem(R.id.action_search);
		SearchView searchView = (SearchView) menu.findItem(R.id.action_search)
				.getActionView();
		searchView.setOnQueryTextListener(new OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				menuItem.collapseActionView();
		        mViewPager.setCurrentItem(1);

		        final ListenableFuture<List<String>> result = service.get().get(
		        		query, 5000);
		        
		        result.addListener(new Runnable() {
					@Override
					public void run() {
						try {
							List<String> list = result.get();
							searchFragment.setText("");
							for (String entry : list) {
								searchFragment.appendText(entry);
							}
						} catch (Exception e) {
							searchFragment.setText(
									"An error occurred while searching for your key: " + e);
						}
					}
		        }, executor);

				return false;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				return false;
			}
		});

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.action_search:
			return true;
		case R.id.options_debug:
			debugFragment.setText(service.get().toString());
			return true;
		case R.id.options_exit:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/** Called when the user touches the button */
	public void createValue(View view) {
		final String key = createFragment.getKey();
		final String value = createFragment.getValue();
	
		ListenableFuture<Void> result = service.get().put(key, value);

		final Context context = this;
		
		Futures.addCallback(result, new FutureCallback<Void>() {
			  public void onSuccess(Void explosion) {
					Toast.makeText(context, "Value created!", Toast.LENGTH_SHORT).show();
			  }
			  public void onFailure(Throwable thrown) {
					Toast.makeText(context, "Failed to create value :(", Toast.LENGTH_SHORT).show();
			  }
		}, executor);
	}
	
	@Override
	protected void onDestroy() {
        Log.i(TAG, "Destroying the activity.");

		unbindService(connection);
		super.onDestroy();
	}

	private void checkNotNull(Object reference, String name) {
		if (reference == null) {
			throw new NullPointerException(
					getString(R.string.error_config, name));
		}
	}

	private static class PagerAdapter extends FragmentStatePagerAdapter {
		private final Fragment fragments[];
		
		public PagerAdapter(FragmentManager fm, Fragment fragments[]) {
			super(fm);
			this.fragments = fragments;
		}

		@Override
		public Fragment getItem(int i) {
			return fragments[i];
		}

		@Override
		public int getCount() {
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return "OBJECT " + (position + 1);
		}
	}

	public static class DebugFragment extends Fragment {
		private static final String TAG = "DebugFragment";

		public void setText(String text) {
			if (getView() == null) {
		        Log.e(TAG, "View not available.");
				return;
			}

			TextView textView = ((TextView) getView().findViewById(
					R.id.debug_log));
			textView.setText(text);
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater,
				ViewGroup container, Bundle savedInstanceState) {
			return inflater.inflate(
					R.layout.debug_fragment, container, false);
		}
	}
	
	public static class SearchFragment extends Fragment {
		private static final String TAG = "SearchFragment";
		
		public void setText(String text) {
			if (getView() == null) {
		        Log.e(TAG, "View not available.");
				return;
			}
			TextView mDisplay = (TextView) getView().findViewById(R.id.serp);
			mDisplay.setText(text);
		}

		public void appendText(String text) {
			if (getView() == null) {
		        Log.e(TAG, "View not available.");
				return;
			}
			TextView mDisplay = (TextView) getView().findViewById(R.id.serp);
			mDisplay.append(text);
		}

		@Override
		public View onCreateView(LayoutInflater inflater,
				ViewGroup container, Bundle savedInstanceState) {
	        Log.i(TAG, "Creating view.");
			return inflater.inflate(R.layout.search_fragment, container, false);
		}
	}
	
	public static class CreateFragment extends Fragment {
		private static final String TAG = "CreateFragment";

		public String getKey() {
			if (getView() == null) {
		        Log.e(TAG, "View not available.");
				return null;
			}

			EditText keyView = (EditText) getView().findViewById(R.id.key);
			return keyView.getText().toString();
		}
		
		public String getValue() {
			if (getView() == null) {
		        Log.e(TAG, "View not available.");
				return null;
			}
			
			EditText valueView = (EditText) getView().findViewById(R.id.value);
			return valueView.getText().toString();
		}

		@Override
		public View onCreateView(LayoutInflater inflater,
				ViewGroup container, Bundle savedInstanceState) {
			return inflater.inflate(R.layout.create_fragment, container, false);
		}
	}
}