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
import com.kumbaya.android.R.drawable;
import com.kumbaya.android.R.id;
import com.kumbaya.android.client.BackgroundService.LocalBinder;

import android.app.Activity;
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
public class DemoActivity extends FragmentActivity {
	private static final String TAG = "DemoActivity";

	private final Executor executor = Executors.mainLooperExecutor();

	private final Activity context = this;

	private final ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder b) {
			LocalBinder binder = (LocalBinder) b;
			service = Optional.of(binder.getService());

			service.get().waitForBootstrap().addListener(new Runnable() {
				@Override
				public void run() {
					Log.i(TAG, "Bootstraped!");
					mViewPager.setCurrentItem(1);
				}
			}, executor);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
		}
	};

	private Optional<BackgroundService> service = Optional.absent();
	PagerAdapter mDemoCollectionPagerAdapter;
	ViewPager mViewPager;
	private BootFragment bootFragment;
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

		bootFragment = new BootFragment();
		searchFragment = new SearchFragment();
		createFragment = new CreateFragment();
		debugFragment = new DebugFragment();

		Fragment fragments[] = {
				bootFragment,
				searchFragment, 
				createFragment,
				debugFragment};

		mDemoCollectionPagerAdapter =
				new PagerAdapter(getSupportFragmentManager(), fragments);
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mDemoCollectionPagerAdapter);
		mViewPager.setCurrentItem(0);
		mViewPager.setPageTransformer(true, new ZoomOutPageTransformer());
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				if (position == 0 && service.isPresent() && service.get().isBootstraped()) {
					// mViewPager.setCurrentItem(1);
				} else if (position == 3 && service.isPresent()) {
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
	public void searchValue(View view) {
		final String query = searchFragment.getQuery();
		
		EditText queryEditText = (EditText) findViewById(R.id.query); 
		InputMethodManager imm = (InputMethodManager)getSystemService(
			      Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(queryEditText.getWindowToken(), 0);
		
		search(query);
	}

	public void search(String query) {
		mViewPager.setCurrentItem(1);

		searchFragment.setQuery(query);

		final ListenableFuture<List<String>> result = service.get().get(query, 5000);
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

		final DemoActivity context = this;

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
			return 4;
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
		private DemoActivity search;

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
			
			search = (DemoActivity) activity;
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
		private DemoActivity creator;

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
			
			creator = (DemoActivity) activity;
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

	public static class BootFragment extends Fragment {
		private static final String TAG = "BootFragment";
		@Override
		public View onCreateView(LayoutInflater inflater,
				ViewGroup container, Bundle savedInstanceState) {
			return inflater.inflate(R.layout.booting_fragment, container, false);
		}
	}
}