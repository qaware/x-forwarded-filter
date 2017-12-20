/*
 * Copyright 2002-2017 the original author or authors.
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

package de.qaware.http;


import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang3.Validate;

import java.util.*;


/**
 * Represents HTTP request and response headers, mapping string header names to a list of string values.
 * <p>
 * <p>In addition to the normal methods defined by {@link Map}, this class offers the following
 * convenience methods:
 * <ul>
 * <li>{@link #getFirst(String)} returns the first value associated with a given header name</li>
 * <li>{@link #add(String, String)} adds a header value to the list of values for a header name</li>
 * <li>{@link #set(String, String)} sets the header value to a single string value</li>
 * </ul>
 * <p>
 * <p>Inspired by {@code com.sun.net.httpserver.Headers}.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Josh Long
 * @since 3.0
 */
public class HttpHeaders implements Map<String, List<String>> {


	/**
	 * The HTTP {@code Content-Length} header field name.
	 *
	 * @see <a href="http://tools.ietf.org/html/rfc7230#section-3.3.2">Section 3.3.2 of RFC 7230</a>
	 */
	public static final String CONTENT_LENGTH = "Content-Length";


	/**
	 * The HTTP {@code Location} header field name.
	 *
	 * @see <a href="http://tools.ietf.org/html/rfc7231#section-7.1.2">Section 7.1.2 of RFC 7231</a>
	 */
	public static final String LOCATION = "Location";


	private final Map<String, List<String>> headers;


	/**
	 * Constructs a new, empty instance of the {@code HttpHeaders} object.
	 */
	public HttpHeaders() {
		this(new CaseInsensitiveMap<>(8), false);
	}

	/**
	 * Private constructor that can create read-only {@code HttpHeader} instances.
	 */
	private HttpHeaders(Map<String, List<String>> headers, boolean readOnly) {
		Validate.notNull(headers, "'headers' must not be null");
		if (readOnly) {
			Map<String, List<String>> map = new HashMap<>(headers.size());
			for (Entry<String, List<String>> entry : headers.entrySet()) {
				List<String> values = Collections.unmodifiableList(entry.getValue());
				map.put(entry.getKey(), values);
			}
			this.headers = Collections.unmodifiableMap(map);
		} else {
			this.headers = headers;
		}
	}


	/**
	 * Set the length of the body in bytes, as specified by the
	 * {@code Content-Length} header.
	 */
	public void setContentLength(long contentLength) {
		set(CONTENT_LENGTH, Long.toString(contentLength));
	}

	/**
	 * Return the length of the body in bytes, as specified by the
	 * {@code Content-Length} header.
	 * <p>Returns -1 when the content-length is unknown.
	 */
	public long getContentLength() {
		String value = getFirst(CONTENT_LENGTH);
		return (value != null ? Long.parseLong(value) : -1);
	}


	/**
	 * Return the first header value for the given header name, if any.
	 *
	 * @param headerName the header name
	 * @return the first header value, or {@code null} if none
	 */
	/*@Nullable*/
	public String getFirst(String headerName) {
		List<String> headerValues = this.headers.get(headerName);
		return (headerValues != null ? headerValues.get(0) : null);
	}

	/**
	 * Add the given, single header value under the given name.
	 *
	 * @param headerName  the header name
	 * @param headerValue the header value
	 * @throws UnsupportedOperationException if adding headers is not supported
	 * @see #set(String, String)
	 */
	public void add(String headerName, /*@Nullable*/ String headerValue) {
		List<String> headerValues = this.headers.computeIfAbsent(headerName, k -> new LinkedList<>());
		headerValues.add(headerValue);
	}

	public void addAll(String key, List<? extends String> values) {
		List<String> currentValues = this.headers.computeIfAbsent(key, k -> new LinkedList<>());
		currentValues.addAll(values);
	}

	public void addAll(Map<String, List<String>> values) {
		for (Entry<String, List<String>> entry : values.entrySet()) {
			addAll(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Set the given, single header value under the given name.
	 *
	 * @param headerName  the header name
	 * @param headerValue the header value
	 * @throws UnsupportedOperationException if adding headers is not supported
	 * @see #add(String, String)
	 */
	public void set(String headerName, /*@Nullable*/ String headerValue) {
		List<String> headerValues = new LinkedList<>();
		headerValues.add(headerValue);
		this.headers.put(headerName, headerValues);
	}

	@Override
	public int size() {
		return headers.size();
	}

	@Override
	public boolean isEmpty() {
		return headers.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return headers.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return headers.containsValue(value);
	}

	@Override
	public List<String> get(Object key) {
		return headers.get(key);
	}

	@Override
	public List<String> put(String key, List<String> value) {
		return headers.put(key, value);
	}

	@Override
	public List<String> remove(Object key) {
		return headers.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> m) {
		headers.putAll(m);
	}

	@Override
	public void clear() {
		headers.clear();
	}

	@Override
	public Set<String> keySet() {
		return headers.keySet();
	}

	@Override
	public Collection<List<String>> values() {
		return headers.values();
	}

	@Override
	public Set<Entry<String, List<String>>> entrySet() {
		return headers.entrySet();
	}
}
