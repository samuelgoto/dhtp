package com.kumbaya.android.client.sdk;

import org.junit.Assert;
import org.junit.ComparisonFailure;
import org.junit.Test;

import com.google.i18n.phonenumbers.NumberParseException;

public class E164Test {
	
	@Test
	public void normalize() throws Exception {
		E164 expected = new E164(1, 234, "5678910");
		
		// complete number, different formats.
		assertThat("1", "234", "1 234 567-8910").equalsTo(expected);
		assertThat("US", "234", "1 234 567-8910").equalsTo(expected);
		assertThat("US", "234", "+1 234 567-8910").equalsTo(expected);
		assertThat("US", "234", "12345678910").equalsTo(expected);
		assertThat("US", "234", "+12345678910").equalsTo(expected);
		assertThat("US", "234", "1 (234) 567-8910").equalsTo(expected);

		// no local area code, no country code
		assertThat("US", "234", "567-8910").equalsTo(expected);
		assertThat("US", "234", "5678910").equalsTo(expected);
		
		// no country code, with local area
		assertThat("1", "234", "(234) 567-8910").equalsTo(expected);
		assertThat("US", "234", "(234) 567-8910").equalsTo(expected);
		assertThat("US", "234", "234 567-8910").equalsTo(expected);
		assertThat("US", "234", "234 5678910").equalsTo(expected);
		assertThat("US", "234", "2345678910").equalsTo(expected);
		
		// no local area, with country code
		assertThat("US", "234", "+1 567-8910").equalsTo(expected);
		assertThat("US", "234", "1 567-8910").equalsTo(expected);
		assertThat("US", "234", "1 5678910").equalsTo(expected);
		assertThat("US", "234", "15678910").equalsTo(expected);

		// parsing without a default local area code.
		assertThat("US", "", "234 567-8910").equalsTo(expected);
		assertThat("US", "", "234567-8910").equalsTo(expected);
		assertThat("US", "", "2345678910").equalsTo(expected);
		
		// parsing without a default country code
		try {
			assertThat("", "234", "+1 (234) 567-8910")
				.equalsTo(new E164(1, 234, "5678910"));
			Assert.fail("This is a programming mistake. " +
					"Default country codes are required");
		} catch (IllegalArgumentException e) {
			// expected.
		}
		
		try {
			assertThat("US", "", "+5519123456789").equalsTo(
					new E164(55, 19, "123456789"));
			// NOTE(goto): this is failing to parse a brazilian number.
			Assert.fail("Knonwn issue");
		} catch (NumberParseException e) {
			// expected known issue
		}
		
		try {
			assertThat("US", "650", "+39 331 123 4567").equalsTo(
					new E164(39, 331, "1234567"));
			// +39-650-3319454778
			Assert.fail("Expected known issue.");
		} catch (ComparisonFailure e) {
			// expected known issue. it picks the default local area code
			// instead of using the one embedded in the number.
			System.out.println(e);
		}
	}
	
	private static class AssertThat<K> {
		private final K result;
		AssertThat(K result) {
			this.result = result;
		}
		
		void equalsTo(K expected) throws NumberParseException {
			Assert.assertEquals(expected.toString(), result.toString());
		}
	}
	
	private AssertThat<E164> assertThat(String defaultCountry, String defaultAreaCode, String number) throws NumberParseException {
		return new AssertThat<E164>(
				E164.normalize(defaultCountry, defaultAreaCode, number));
	}
}
