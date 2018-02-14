package de.qaware.web.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Supported forwarded headers and their (caseInsensitive) name inside the http headers
 */
public enum ForwardedHeader {
	FORWARDED("Forwarded"),
	X_FORWARDED_HOST("X-Forwarded-Host"),
	X_FORWARDED_PORT("X-Forwarded-Port"),
	X_FORWARDED_PROTO("X-Forwarded-Proto"),
	X_FORWARDED_PREFIX("X-Forwarded-Prefix");

	private String httpHeaderName;
	private static Map<String, ForwardedHeader> headerLookup = generateLookup();

	ForwardedHeader(String httpHeaderName) {
		this.httpHeaderName =httpHeaderName;
	}

	/**
	 * ForwardedHeader for name (ignores case). null if header name is not a supported forwarded header
	 * @param name the name
	 * @return ForwardedHeader for name (ignores case). null if header name is not a supported forwarded header
	 */
	public static ForwardedHeader forName(String name) {
		return headerLookup.get(StringUtils.defaultString(name).toLowerCase(Locale.ENGLISH));
	}

	/**
	 * Checks if the provided  header name is a supported. Same as{@see forName}!=null
	 *
	 * @param name headerName
	 * @return
	 */
	public static boolean isForwardedHeader(String name) {
		return headerLookup.containsKey(StringUtils.defaultString(name).toLowerCase(Locale.ENGLISH));
	}
	/**
	 * Http header name represented by this enum
	 * @return the name of the header
	 */
	public String headerName(){
		return httpHeaderName;
	}

	@Override
	public String toString() {
		return headerName();
	}

	private static Map<String, ForwardedHeader> generateLookup() {
		return Stream.of(values()).collect(Collectors.toMap(header -> header.toString().toLowerCase(Locale.ENGLISH), Function.identity()));
	}
}
