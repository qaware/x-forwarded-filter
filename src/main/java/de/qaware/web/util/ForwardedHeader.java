package de.qaware.web.util;

import org.apache.commons.lang3.StringUtils;

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
	 *
	 * @param name
	 * @return ForwardedHeader for name (ignores case). null if header name is not a supported forwarded header
	 */
	public static ForwardedHeader forName(String name) {
		return headerLookup.get(StringUtils.defaultString(name).toLowerCase());
	}

	public static boolean isForwardedHeader(String name) {
		return headerLookup.containsKey(StringUtils.defaultString(name).toLowerCase());
	}
	/**
	 * Http header name represented by this enum
	 * @return
	 */
	public String headerName(){
		return httpHeaderName;
	}

	@Override
	public String toString() {
		return headerName();
	}

	private static Map<String, ForwardedHeader> generateLookup() {
		return Stream.of(values()).collect(Collectors.toMap(header -> header.toString().toLowerCase(), Function.identity()));
	}
}
