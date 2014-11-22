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

import java.sql.Date;
import java.util.List;
import java.util.concurrent.Executor;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.kumbaya.android.R;
import com.kumbaya.android.client.sdk.BackgroundService;
import com.kumbaya.android.client.sdk.BackgroundService.LocalBinder;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main UI for the demo app.
 */
public class MainActivity extends FragmentActivity {
	private static final String TAG = "MainActivity";

	private final Executor executor = Executors.mainLooperExecutor();

	private final MainActivity context = this;
	
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
	private SearchFragment searchFragment;

	private final BroadcastReceiver mHandleMessageReceiver =
			new BroadcastReceiver() {
		@Override
		public void onReceive(Context c, Intent intent) {
			String message = intent.getExtras().getString("type");
			String origin = intent.getExtras().getString("origin");

			final TextView text = (TextView) context.findViewById(R.id.progress_status);
			text.setText(message + " from: " + origin);
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					text.setText("");
				}
			}, 4000);
		}
	};

	static class Phone {
		private final String owner;
		private final String number;

		Phone(String number, String owner) {
			this.owner = owner;
			this.number = number;
		}

		static Phone of(String number, String owner) {
			return new Phone(number, owner);
		}

		String owner() {
			return owner;
		}

		String number() {
			return number;
		}

		@Override
		public String toString() {
			return "Phone: " + number + ", owner: " + owner;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.i(TAG, "Creating the activity.");
		setContentView(R.layout.main);

		ActionBar bar = getActionBar();
		bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#259b24")));
		
		searchFragment = new SearchFragment();
		createFragment = new CreateFragment();

		Fragment fragments[] = {
				searchFragment, 
				createFragment};

		mDemoCollectionPagerAdapter =
				new PagerAdapter(getSupportFragmentManager(), fragments);
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mDemoCollectionPagerAdapter);
		mViewPager.setCurrentItem(0);
		mViewPager.setPageTransformer(true, new ZoomOutPageTransformer());
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				TextView text = (TextView) context.findViewById(R.id.progress_status);
				text.setText("");
			}
		});

		registerReceiver(mHandleMessageReceiver,
				new IntentFilter(BackgroundService.UPDATE_ACTION));

		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				Intent i = new Intent(context, BackgroundService.class);
				bindService(i, connection, Context.BIND_AUTO_CREATE);
				return null;
			}
		}.execute();
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
				search(query);

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
			Log.i(TAG, "Kicking off the debug activity");
			Intent intent = new Intent(context, DebugActivity.class);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void search(String query) {
		mViewPager.setCurrentItem(0);

		searchFragment.setQuery(query);

		EditText queryEditText = (EditText) findViewById(R.id.query); 
		InputMethodManager imm = (InputMethodManager)getSystemService(
				Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(queryEditText.getWindowToken(), 0);

		final ListenableFuture<List<String>> result = service.get().get(query, 5000);
		result.addListener(new Runnable() {
			@Override
			public void run() {
				try {
					List<String> list = result.get();
					if (list.isEmpty()) {
						searchFragment.setText("No value found :(");
					} else {
						searchFragment.setText("");
						for (String entry : list) {
							searchFragment.appendText(entry);
						}
					}
				} catch (Exception e) {
					searchFragment.setText(
							"An error occurred while searching for your key: " + e);
				}
			}
		}, executor);
	}

	/** Called when the user touches the button */
	public void createValue() {
		final String key = createFragment.getKey();
		final String value = createFragment.getValue();

		EditText valueEditText = (EditText) findViewById(R.id.value); 
		InputMethodManager imm = (InputMethodManager)getSystemService(
				Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(valueEditText.getWindowToken(), 0);

		ListenableFuture<Void> result = service.get().put(key, value);

		final MainActivity context = this;

		Futures.addCallback(result, new FutureCallback<Void>() {
			public void onSuccess(Void explosion) {
				Toast.makeText(context, "Value created!", Toast.LENGTH_SHORT).show();

				context.search(key);
			}
			public void onFailure(Throwable thrown) {
				Toast.makeText(context, "Failed to create value :(", Toast.LENGTH_SHORT).show();
			}
		}, executor);
	}

	@Override
	protected void onDestroy() {
		Log.i(TAG, "Destroying the activity.");
		unregisterReceiver(mHandleMessageReceiver);
		unbindService(connection);
		super.onDestroy();
	}

	static class PagerAdapter extends FragmentStatePagerAdapter {
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
			return fragments.length;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return "OBJECT " + (position + 1);
		}
	}

	public static class SearchFragment extends Fragment {
		private static final String TAG = "SearchFragment";
		private MainActivity search;

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);

			search = (MainActivity) activity;
		}

		@Override
		public void setMenuVisibility(final boolean visible) {
			super.setMenuVisibility(visible);
			if (!visible) {
				setQuery("");
				setText("");
			}
		}

		public void setQuery(String query) {
			if (getView() == null) {
				Log.e(TAG, "View not available.");
				return;
			}
			EditText text = (EditText) getView().findViewById(R.id.query);
			text.setText(query);
		}

		public String getQuery() {
			if (getView() == null) {
				Log.e(TAG, "View not available.");
				return null;
			}
			EditText query = (EditText) getView().findViewById(R.id.query);
			return query.getText().toString();
		}

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
			View fragment = inflater.inflate(R.layout.search_fragment, container, false);

			EditText editText = (EditText) fragment.findViewById(R.id.query);

			editText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_DONE) {
						search.search(getQuery());
						return true;
					}
					return false;
				}
			});
			return fragment;
		}
	}

	public static class CreateFragment extends Fragment {
		private static final String TAG = "CreateFragment";
		private MainActivity creator;

		@Override
		public void setMenuVisibility(final boolean visible) {
			super.setMenuVisibility(visible);

			if (getView() == null) {
				Log.e(TAG, "View not available.");
				return;
			}

			if (!visible) {
				EditText keyView = (EditText) getView().findViewById(R.id.key);
				keyView.setText("");
				EditText valueView = (EditText) getView().findViewById(R.id.value);
				valueView.setText("");
			}
		}

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
		public void onAttach(Activity activity) {
			super.onAttach(activity);

			creator = (MainActivity) activity;
		}

		@Override
		public View onCreateView(LayoutInflater inflater,
				ViewGroup container, Bundle savedInstanceState) {
			View fragment = inflater.inflate(R.layout.create_fragment, container, false);

			EditText editText = (EditText) fragment.findViewById(R.id.value);

			editText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_DONE) {
						creator.createValue();
						return true;
					}
					return false;
				}
			});

			return fragment;
		}
	}
}