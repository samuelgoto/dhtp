package com.kumbaya.android.client;

import java.util.List;

import org.limewire.inject.Providers;

import android.database.Cursor;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.kumbaya.android.client.DebugActivity.CallsLogLoader;
import com.kumbaya.android.client.DebugActivity.ContactsLoader;
import com.kumbaya.android.client.MainActivity.Phone;
import com.kumbaya.android.client.sdk.E164;

public class ContactsTest extends AndroidTestCase {
	
	@SmallTest
	public void testReadContacts() {
		ContactsLoader loader = new ContactsLoader(
				Providers.of(getContext()));
		Cursor cursor = loader.load().loadInBackground();
		List<Phone> phones = loader.read(cursor);
		System.out.println(phones);
	}
	
	@SmallTest
	public void testCallsLog() {
		CallsLogLoader loader = new CallsLogLoader(
				Providers.of(getContext()));
		Cursor cursor = loader.load().loadInBackground();
		List<E164> phones = loader.read(cursor);
		System.out.println(phones);
	}
	
	@SmallTest
	public void testLocalNumber() {
		System.out.println(E164.localNumber(getContext()));
	}
}
