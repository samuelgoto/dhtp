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

import static com.kumbaya.android.client.CommonUtilities.DISPLAY_MESSAGE_ACTION;
import static com.kumbaya.android.client.CommonUtilities.EXTRA_MESSAGE;
import static com.kumbaya.android.client.CommonUtilities.SENDER_ID;
import static com.kumbaya.android.client.CommonUtilities.SERVER_URL;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.exceptions.NotBootstrappedException;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.io.Tag;
import org.limewire.security.SecureMessage;
import org.limewire.security.SecureMessageCallback;

import com.google.android.gcm.GCMRegistrar;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.kumbaya.android.R;
import com.kumbaya.android.client.BackgroundService.LocalBinder;
import com.kumbaya.dht.Dht;
import com.kumbaya.dht.DhtModule;
import com.kumbaya.monitor.VarZModule;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

/**
 * Main UI for the demo app.
 */
public class DemoActivity extends FragmentActivity {
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
	DemoCollectionPagerAdapter mDemoCollectionPagerAdapter;
	ViewPager mViewPager;
	private final CreateFragment createFragment = new CreateFragment();
	private final DebugFragment debugFragment = new DebugFragment();
	private final SearchFragment searchFragment = new SearchFragment();


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = new Intent(this, BackgroundService.class);
		startService(intent);

		Intent i = new Intent(this, BackgroundService.class);
		bindService(i, connection, Context.BIND_AUTO_CREATE);

		checkNotNull(SERVER_URL, "SERVER_URL");
		checkNotNull(SENDER_ID, "SENDER_ID");

		setContentView(R.layout.main);

		registerReceiver(mHandleMessageReceiver,
				new IntentFilter(DISPLAY_MESSAGE_ACTION));
		
		// ViewPager and its adapters use support library
        // fragments, so use getSupportFragmentManager.
		Fragment fragments[] = {
				createFragment,
				searchFragment, 
				debugFragment};

        mDemoCollectionPagerAdapter =
                new DemoCollectionPagerAdapter(
                        getSupportFragmentManager(),
                        fragments);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mDemoCollectionPagerAdapter);
        mViewPager.setCurrentItem(1);
        mViewPager.setPageTransformer(true, new ZoomOutPageTransformer());
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
            	if (position == 2) {
        			debugFragment.setText(service.get().toString());
            	}
            }
        });
        
		/*
        EditText editText = (EditText) findViewById(R.id.search);
        editText.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int keyCode,
					KeyEvent event) {
                if (keyCode == EditorInfo.IME_ACTION_SEARCH ||
                	(event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {

                	return true;
                }
                return false;
			}
        });
		 */
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

				new AsyncTask<String, Void, List<String>>() {
					@Override
					protected List<String> doInBackground(String... params) {
						try {
							return service.get().get(params[0], 5000);
						} catch (InterruptedException e) {
							return Lists.newArrayList();
						} catch (ExecutionException e) {
							return Lists.newArrayList();
						} catch (TimeoutException e) {
							return Lists.newArrayList();
						} catch (NotBootstrappedException e) {
							return Lists.newArrayList();
						}
					}

					@Override
					protected void onPostExecute(List<String> result) {
						searchFragment.setText("");
						for (String entry : result) {
							searchFragment.appendText(entry);
						}
					}
				}.execute(query);

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

	@Override
	protected void onDestroy() {
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

	private final BroadcastReceiver mHandleMessageReceiver =
			new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// String newMessage = intent.getExtras().getString(EXTRA_MESSAGE);
			// mDisplay.append(newMessage + "\n");
		}
	};


	// Since this is an object collection, use a FragmentStatePagerAdapter,
	// and NOT a FragmentPagerAdapter.
	public class DemoCollectionPagerAdapter extends FragmentStatePagerAdapter {
		private final Fragment fragments[];
		
		public DemoCollectionPagerAdapter(FragmentManager fm, Fragment fragments[]) {
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

	// Instances of this class are fragments representing a single
	// object in our collection.
	public static class DebugFragment extends Fragment {
		private TextView textView;

		public void setText(String text) {
			textView.setText(text);
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater,
				ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(
					R.layout.debug_fragment, container, false);
			textView = ((TextView) rootView.findViewById(
					R.id.debug_log));
			return rootView;
		}
	}
	
	public static class SearchFragment extends Fragment {
		TextView mDisplay;
		
		public void setText(String text) {
			mDisplay.setText(text);
		}

		public void appendText(String text) {
			mDisplay.append(text);
		}

		@Override
		public View onCreateView(LayoutInflater inflater,
				ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(
					R.layout.search_fragment, container, false);
			
			mDisplay = (TextView) rootView.findViewById(R.id.serp);

			return rootView;
		}
	}
	
	public static class CreateFragment extends Fragment {

		@Override
		public View onCreateView(LayoutInflater inflater,
				ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(
					R.layout.create_fragment, container, false);
			
			return rootView;
		}
	}

	public class ZoomOutPageTransformer implements ViewPager.PageTransformer {
	    private static final float MIN_SCALE = 0.85f;
	    private static final float MIN_ALPHA = 0.5f;

	    public void transformPage(View view, float position) {
	        int pageWidth = view.getWidth();
	        int pageHeight = view.getHeight();

	        if (position < -1) { // [-Infinity,-1)
	            // This page is way off-screen to the left.
	            view.setAlpha(0);

	        } else if (position <= 1) { // [-1,1]
	            // Modify the default slide transition to shrink the page as well
	            float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
	            float vertMargin = pageHeight * (1 - scaleFactor) / 2;
	            float horzMargin = pageWidth * (1 - scaleFactor) / 2;
	            if (position < 0) {
	                view.setTranslationX(horzMargin - vertMargin / 2);
	            } else {
	                view.setTranslationX(-horzMargin + vertMargin / 2);
	            }

	            // Scale the page down (between MIN_SCALE and 1)
	            view.setScaleX(scaleFactor);
	            view.setScaleY(scaleFactor);

	            // Fade the page relative to its size.
	            view.setAlpha(MIN_ALPHA +
	                    (scaleFactor - MIN_SCALE) /
	                    (1 - MIN_SCALE) * (1 - MIN_ALPHA));

	        } else { // (1,+Infinity]
	            // This page is way off-screen to the right.
	            view.setAlpha(0);
	        }
	    }
	}
}