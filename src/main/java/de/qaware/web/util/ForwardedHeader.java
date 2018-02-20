/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
		this.httpHeaderName = httpHeaderName;
	}

	/**
	 * ForwardedHeader for name (ignores case). null if header name is not a supported forwarded header
	 *
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
	 * @return true if given header is a "forwarded" header
	 */
	public static boolean isForwardedHeader(String name) {
		return headerLookup.containsKey(StringUtils.defaultString(name).toLowerCase(Locale.ENGLISH));
	}

	/**
	 * Http header name represented by this enum
	 *
	 * @return the name of the header
	 */
	public String headerName() {
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
