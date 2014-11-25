package com.kumbaya.android.client.sdk;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

public class TelephoneNumbersTest {
	
	@Test
	public void testPhoneNumberUtil() throws Exception {
		// complete number, different formats.
		assertThat("US", "234", "1 234 567-8910").equals("+1 (234) 5678910");
		assertThat("US", "234", "+1 234 567-8910").equals("+1 (234) 5678910");
		assertThat("US", "234", "12345678910").equals("+1 (234) 5678910");
		assertThat("US", "234", "+12345678910").equals("+1 (234) 5678910");
		assertThat("US", "234", "1 (234) 567-8910").equals("+1 (234) 5678910");

		// no local area code, no country code
		assertThat("US", "234", "567-8910").equals("+1 (234) 5678910");
		assertThat("US", "234", "5678910").equals("+1 (234) 5678910");
		
		// no country code, with local area
		assertThat("US", "234", "(234) 567-8910").equals("+1 (234) 5678910");
		assertThat("US", "234", "234 567-8910").equals("+1 (234) 5678910");
		assertThat("US", "234", "234 5678910").equals("+1 (234) 5678910");
		assertThat("US", "234", "2345678910").equals("+1 (234) 5678910");
		
		// no local area, with country code
		assertThat("US", "234", "+1 567-8910").equals("+1 (234) 5678910");
		assertThat("US", "234", "1 567-8910").equals("+1 (234) 5678910");
		assertThat("US", "234", "1 5678910").equals("+1 (234) 5678910");
		assertThat("US", "234", "15678910").equals("+1 (234) 5678910");

		// parsing without a default country code
		try {
			assertThat("", "234", "+1 (234) 567-8910").equals("+1 (234) 5678910");
			Assert.fail("We expect this isn't implemented yet");
		} catch (UnsupportedOperationException e) {
			// TODO(goto): implement this.
			// TODO(goto): implement parsing without a local area code too.
		}
	}
	
	String normalize(String defaultCountry, String defaultAreaCode, String number) throws NumberParseException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(defaultAreaCode));

		PhoneNumberUtil phoneNumbers = PhoneNumberUtil.getInstance();
		final PhoneNumber result;
		if (!Strings.isNullOrEmpty(defaultCountry)) {
			result = phoneNumbers.parse(number, defaultCountry);
		} else {
			throw new UnsupportedOperationException("default country code expected");
		}
		int areaCodeLength = phoneNumbers.getLengthOfGeographicalAreaCode(
				result);
		final String areaCode;
		final String subscriber;
		String nationalSignificantNumber = phoneNumbers
				.getNationalSignificantNumber(result);
		if (areaCodeLength > 0) {
			areaCode = nationalSignificantNumber.substring(0, areaCodeLength);
			subscriber = nationalSignificantNumber.substring(areaCodeLength);
		} else {
			areaCode = defaultAreaCode;
			subscriber = nationalSignificantNumber;
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append("+");
		builder.append(String.valueOf(result.getCountryCode()));
		builder.append(" (");
		builder.append(areaCode);
		builder.append(") ");
		builder.append(subscriber);
		return  builder.toString();
	}	
	
	private static class AssertThat<K> {
		private final K result;
		AssertThat(K result) {
			this.result = result;
		}
		
		void equals(K expected) throws NumberParseException {
			Assert.assertEquals(expected, result);
		}
	}
	
	private AssertThat<String> assertThat(String defaultCountry, String defaultAreaCode, String number) throws NumberParseException {
		return new AssertThat<String>(normalize(defaultCountry, defaultAreaCode, number));
	}
}
