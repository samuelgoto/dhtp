package com.kumbaya.android.client;

import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.telephony.TelephonyManager;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.kumbaya.android.client.DebugActivity.CallsLogLoader;
import com.kumbaya.android.client.DebugActivity.ContactsLoader;
import com.kumbaya.android.client.MainActivity.Phone;
import com.kumbaya.android.client.sdk.E164;

public class ContactsTest extends AndroidTestCase {
	
	@SmallTest
	public void testReadContacts() {
		ContactsLoader loader = new ContactsLoader();
		Cursor cursor = loader.load(getContext()).loadInBackground();
		List<Phone> phones = loader.read(cursor);
		System.out.println(phones);
	}
	
	@SmallTest
	public void testCallsLog() {
		CallsLogLoader loader = new CallsLogLoader(getContext());
		Cursor cursor = loader.load(getContext()).loadInBackground();
		List<E164> phones = loader.read(cursor);
		System.out.println(phones);
	}
	
	@SmallTest
	public void testLocalNumber() {
		System.out.println(E164.localNumber(getContext()));
	}
}
