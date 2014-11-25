package com.kumbaya.android.client.sdk;

import org.junit.Assert;
import org.junit.Test;

import com.google.i18n.phonenumbers.NumberParseException;

public class E164Test {
	
	@Test
	public void normalize() throws Exception {
		String expected = "+1-234-5678910";
		
		// complete number, different formats.
		assertThat("US", "234", "1 234 567-8910").equals(expected);
		assertThat("US", "234", "+1 234 567-8910").equals(expected);
		assertThat("US", "234", "12345678910").equals(expected);
		assertThat("US", "234", "+12345678910").equals(expected);
		assertThat("US", "234", "1 (234) 567-8910").equals(expected);

		// no local area code, no country code
		assertThat("US", "234", "567-8910").equals(expected);
		assertThat("US", "234", "5678910").equals(expected);
		
		// no country code, with local area
		assertThat("US", "234", "(234) 567-8910").equals(expected);
		assertThat("US", "234", "234 567-8910").equals(expected);
		assertThat("US", "234", "234 5678910").equals(expected);
		assertThat("US", "234", "2345678910").equals(expected);
		
		// no local area, with country code
		assertThat("US", "234", "+1 567-8910").equals(expected);
		assertThat("US", "234", "1 567-8910").equals(expected);
		assertThat("US", "234", "1 5678910").equals(expected);
		assertThat("US", "234", "15678910").equals(expected);

		// parsing without a default local area code.
		assertThat("US", "", "234 567-8910").equals(expected);
		assertThat("US", "", "234567-8910").equals(expected);
		assertThat("US", "", "2345678910").equals(expected);
		
		// parsing without a default country code
		try {
			assertThat("", "234", "+1 (234) 567-8910")
				.equals("+1 (234) 5678910");
			Assert.fail("This is a programming mistake. " +
					"Default country codes are required");
		} catch (IllegalArgumentException e) {
			// expected.
		}
		
		try {
			assertThat("US", "", "+5519123456789").equals("+55 19 123456789");
			// NOTE(goto): this is failing to parse a brazilian number.
			Assert.fail("Knonwn issue");
		} catch (NumberParseException e) {
			// expected known issue
		}
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
	
	private AssertThat<E164> assertThat(String defaultCountry, String defaultAreaCode, String number) throws NumberParseException {
		return new AssertThat<E164>(
				E164.normalize(defaultCountry, defaultAreaCode, number));
	}
}
