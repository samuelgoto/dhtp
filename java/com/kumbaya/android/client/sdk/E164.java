package com.kumbaya.android.client.sdk;

import android.content.Context;
import android.telephony.TelephonyManager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.NumberParseException.ErrorType;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

public class E164 {
	private final String country;
	private final String areaCode;
	private final String subscriber;
	
	E164(String country, String areaCode, String subscriber) {
		this.country = country;
		this.areaCode = areaCode;
		this.subscriber = subscriber;
	}
	
	public String country() {
		return country;
	}
	
	public String areaCode() {
		return areaCode;
	}
	
	public String subscriber() {
		return subscriber;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("+");
		builder.append(String.valueOf(country));
		builder.append("-");
		builder.append(areaCode);
		builder.append("-");
		builder.append(subscriber);
		return builder.toString();
	}
	
	public static E164 normalize(
			String defaultCountry, 
			String defaultAreaCode, 
			String number) throws NumberParseException {
		Preconditions.checkArgument(
				!Strings.isNullOrEmpty(defaultCountry));
		
		PhoneNumberUtil phoneNumbers = PhoneNumberUtil.getInstance();
		
		final PhoneNumber result = phoneNumbers.parse(
				number, defaultCountry);

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
			// Either a default area code is passed or there is an
			// area code in the phone number.
			if (Strings.isNullOrEmpty(defaultAreaCode)) {
				throw new NumberParseException(
						ErrorType.TOO_SHORT_NSN,
						"No area code found nor a default area code used:" + number);
			}
			
			areaCode = defaultAreaCode;
			subscriber = nationalSignificantNumber;
		}

		E164 normalized = new E164(
				String.valueOf(result.getCountryCode()), areaCode, subscriber);
		
		// Preconditions.checkArgument(phoneNumbers.isValidNumber(
		//		phoneNumbers.parse(normalized.toString(), defaultCountry)),
		//		"Invalid phone number: " + number);

		return normalized;
	}
	
	public static Optional<E164> localNumber(Context context) {
		TelephonyManager manager = (TelephonyManager) context.getSystemService(
				Context.TELEPHONY_SERVICE);

		String phoneNumber = manager.getLine1Number();
		String countryCode = manager.getSimCountryIso().toUpperCase();
		try {
			return Optional.of(normalize(countryCode, "", phoneNumber));
		} catch (NumberParseException e) {
			return Optional.absent();
		}
	}
}
