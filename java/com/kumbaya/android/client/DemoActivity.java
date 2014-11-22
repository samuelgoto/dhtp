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

import java.io.File;
import java.sql.Date;
import java.util.List;
import java.util.concurrent.Executor;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.kumbaya.android.R;
import com.kumbaya.android.R.drawable;
import com.kumbaya.android.R.id;
import com.kumbaya.android.client.BackgroundService.LocalBinder;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
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

	private final DemoActivity context = this;

	private final ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder b) {
			LocalBinder binder = (LocalBinder) b;
			service = Optional.of(binder.getService());

			service.get().waitForBootstrap().addListener(new Runnable() {
				@Override
				public void run() {
					// Announcing ourselves to the network.
					TelephonyManager manager = (TelephonyManager) getSystemService(
							Context.TELEPHONY_SERVICE);
					String phoneNumber = manager.getLine1Number();
					String countryCode = manager.getSimCountryIso().toUpperCase();
					phoneNumber = countryCode + " " + phoneNumber;
					// TODO(goto): canonicalize the phone number serialization and
					// store a different value here.
					ListenableFuture<Void> result = service.get().put(
							phoneNumber, phoneNumber);
					Log.i(TAG, "Bootstraped!");
					bootFragment.setText("Bootstrapped! Now announcing ourselves...");
					result.addListener(new Runnable() {
						@Override
						public void run() {
							Log.i(TAG, "Announced!");
							mViewPager.setCurrentItem(1);
						}
					}, executor);
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
	private ContactsFragment contactsFragment;

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

		checkNotNull(SERVER_URL, "SERVER_URL");
		checkNotNull(SENDER_ID, "SENDER_ID");

		setContentView(R.layout.main);

		ActionBar bar = getActionBar();
		bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#259b24")));

		bootFragment = new BootFragment();
		searchFragment = new SearchFragment();
		createFragment = new CreateFragment();
		debugFragment = new DebugFragment();
		contactsFragment = new ContactsFragment();

		Fragment fragments[] = {
				bootFragment,
				searchFragment, 
				createFragment,
				debugFragment,
				contactsFragment};

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

				if (position == 0 && service.isPresent() && service.get().isBootstraped()) {
					// mViewPager.setCurrentItem(1);
				} else if (position == 3 && service.isPresent()) {
					/*
					new AsyncTask<Void, Void, String>() {
						@Override
						protected String doInBackground(Void... params) {
							TelephonyManager manager = (TelephonyManager) getSystemService(
									Context.TELEPHONY_SERVICE);
							String mPhoneNumber = manager.getLine1Number();
							String countryCode = manager.getSimCountryIso().toUpperCase();

							final String localAreaCode;
							if (manager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
								GsmCellLocation cellLocation = (GsmCellLocation) manager.getCellLocation();
								localAreaCode = String.valueOf(cellLocation.getLac());
							} else if (manager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
								CdmaCellLocation cellLocation = (CdmaCellLocation) manager.getCellLocation();
								localAreaCode = String.valueOf(cellLocation.toString());
							} else {
								localAreaCode = "UNKNOWN";
							}

							ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

							// NOTE(goto): this should always return false on emulators according to
							// http://stackoverflow.com/questions/7876302/enabling-wifi-on-android-emulator
							final NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

							// Do some of the heavy lifting asynchronously.
							String result = "";
							// NOTE(goto): this is the number I get in my
							// emulator: 15555215554 for the phone number.
							// I get something like 650394932 when I run this with 
							// a real phone + the country code is correct.
							result += "Phone number: " + mPhoneNumber + "\n";
							result += "Country code: " + countryCode + "\n";
							result += "Area code: " + localAreaCode + "\n";
							result += "Call log: " + getCallLog().size() + "\n";
							result += "Contacts: " + readContacts().size() + "\n";
							result += "Wifi connected: " + wifi.isConnected() + "\n";
							return result;
						}

					    protected void onPostExecute(String result) {
							String current = debugFragment.getText();
							debugFragment.setText(current + "\n" + result);
					    }
					}.execute();
					 */

					String result = service.get().toString();

					debugFragment.setText(result);
				}
			}
		});

		registerReceiver(mHandleMessageReceiver,
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
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void search(String query) {
		mViewPager.setCurrentItem(1);

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
		unregisterReceiver(mHandleMessageReceiver);
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
			return fragments.length;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return "OBJECT " + (position + 1);
		}
	}

	public static class ContactsFragment extends Fragment implements 
	LoaderCallbacks<Cursor> {
		private static final String TAG = "ContactsFragment";
		private static final int CALLS_LOG_LOADER = 1;
		private static final int CONTACTS_LOADER = 2;

		public void setText(int id, String text) {
			if (getView() == null) {
				Log.e(TAG, "View not available.");
				return;
			}

			final int view;
			switch (id) {
			case CALLS_LOG_LOADER:
				view = R.id.calls_log_view;
				break;
			case CONTACTS_LOADER:
				view = R.id.contacts_view;
				break;
			default:
				throw new UnsupportedOperationException("Invalid id" + id);
			}
			TextView textView = ((TextView) getView().findViewById(view));
			textView.setText(text);
		}

		public String getText() {
			if (getView() == null) {
				Log.e(TAG, "View not available.");
				return "";
			}

			TextView textView = ((TextView) getView().findViewById(
					R.id.contacts_view));
			return textView.getText().toString();
		}

		@Override
		public View onCreateView(LayoutInflater inflater,
				ViewGroup container, Bundle savedInstanceState) {
			getLoaderManager().initLoader(CALLS_LOG_LOADER, null, this);
			getLoaderManager().initLoader(CONTACTS_LOADER, null, this);
			return inflater.inflate(
					R.layout.contacts_fragment, container, false);
		}

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			switch (id) {
			case CALLS_LOG_LOADER: {
				return new CursorLoader(
						getActivity(),
						CallLog.Calls.CONTENT_URI,
						null,
						null,
						null,
						null);		
			}
			case CONTACTS_LOADER: {
				String[] projection = new String[] {
						ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
		                ContactsContract.CommonDataKinds.Phone.NUMBER};
				
				return new CursorLoader(
						getActivity(),
						ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
						projection,
						null,
						null,
						null);
			}
			}
			throw new UnsupportedOperationException("Can't create a loader of id: " + id);
		}

		public List<Phone> readContacts(Cursor cur) {
			ImmutableList.Builder<Phone> result = ImmutableList.builder();

			int nameColumn = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
			int numberColumn = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

			if (cur.getCount() > 0) {
				while (cur.moveToNext()) {
					String name   = cur.getString(nameColumn);
					String number = cur.getString(numberColumn);
					result.add(Phone.of(number, name));
				}
			}

			return result.build();
		}

		private List<String> getCallLog(Cursor managedCursor) {
			ImmutableList.Builder<String> result = ImmutableList.builder();
			StringBuffer sb = new StringBuffer();
			int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
			int type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);
			int date = managedCursor.getColumnIndex(CallLog.Calls.DATE);
			int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);
			sb.append("Call Details :");
			while (managedCursor.moveToNext()) {
				String phNumber = managedCursor.getString(number);
				String callType = managedCursor.getString(type);
				String callDate = managedCursor.getString(date);
				Date callDayTime = new Date(Long.valueOf(callDate));
				String callDuration = managedCursor.getString(duration);
				String dir = null;
				int dircode = Integer.parseInt(callType);
				switch (dircode) {
				case CallLog.Calls.OUTGOING_TYPE:
					dir = "OUTGOING";
					break;

				case CallLog.Calls.INCOMING_TYPE:
					dir = "INCOMING";
					break;

				case CallLog.Calls.MISSED_TYPE:
					dir = "MISSED";
					break;
				}
				result.add(phNumber);
			}
			return result.build();
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
			switch (loader.getId()) {
			case CALLS_LOG_LOADER: {
				List<String> result = getCallLog(cursor);
				setText(CALLS_LOG_LOADER, "Number of calls: " + result.size());
				break;
			}
			case CONTACTS_LOADER: {
				List<Phone> result = readContacts(cursor);
				setText(CONTACTS_LOADER, "Number of contacts: " + result.size());
				break;
			}
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
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

		public String getText() {
			if (getView() == null) {
				Log.e(TAG, "View not available.");
				return "";
			}

			TextView textView = ((TextView) getView().findViewById(
					R.id.debug_log));
			return textView.getText().toString();
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

		public void setText(String text) {
			if (getView() == null) {
				Log.e(TAG, "View not available.");
				return;
			}
			TextView mDisplay = (TextView) getView().findViewById(R.id.boot_status);
			mDisplay.setText(text);
		}

		@Override
		public View onCreateView(LayoutInflater inflater,
				ViewGroup container, Bundle savedInstanceState) {
			return inflater.inflate(R.layout.booting_fragment, container, false);
		}
	}
}