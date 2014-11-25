package com.kumbaya.android.client.sdk;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.v4.content.CursorLoader;
import android.telephony.PhoneNumberUtils;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.kumbaya.android.client.DebugActivity;
import com.kumbaya.android.client.DebugActivity.ContactsFragment;
import com.kumbaya.android.client.MainActivity.Phone;

public class ContactsTest extends AndroidTestCase {
	
	@SmallTest
	public void testReadContacts() {
		Cursor cursor = ContactsFragment.contactsLoader(getContext()).loadInBackground();
		List<Phone> phones = ContactsFragment.readContacts(cursor);
		System.out.println(phones);
	}
	
	@SmallTest
	public void testCallsLog() {
		Cursor cursor = ContactsFragment.callsLogLoader(getContext()).loadInBackground();
		List<String> phones = ContactsFragment.getCallLog(cursor);
		System.out.println(phones);
	}
	
	@SmallTest
	public void testPhoneNumberUtil() {
		assertEquals("1 234 567-8910", PhoneNumberUtils.formatNumber("1 234 567-8910"));
		assertEquals("+1 234 567-8910", PhoneNumberUtils.formatNumber("+1 234 567-8910"));
		assertEquals("1-234-567-890", PhoneNumberUtils.formatNumber("1234567890"));
	}
}
