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

package org.springframework.http;

import org.springframework.util.AssertM;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.io.Serializable;
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
public class HttpHeaders implements MultiValueMap<String, String>, Serializable {

	private static final long serialVersionUID = -8578554704772377436L;

	/**
	 * The HTTP {@code Content-Length} header field name.
	 *
	 * @see <a href="http://tools.ietf.org/html/rfc7230#section-3.3.2">Section 3.3.2 of RFC 7230</a>
	 */
	public static final String CONTENT_LENGTH = "Content-Length";
	/**
	 * The HTTP {@code Content-Type} header field name.
	 *
	 * @see <a href="http://tools.ietf.org/html/rfc7231#section-3.1.1.5">Section 3.1.1.5 of RFC 7231</a>
	 */
	public static final String CONTENT_TYPE = "Content-Type";
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
		this(new LinkedCaseInsensitiveMap<>(8, Locale.ENGLISH), false);
	}

	/**
	 * Private constructor that can create read-only {@code HttpHeader} instances.
	 */
	private HttpHeaders(Map<String, List<String>> headers, boolean readOnly) {
		AssertM.notNull(headers, "'headers' must not be null");
		if (readOnly) {
			Map<String, List<String>> map =
					new LinkedCaseInsensitiveMap<>(headers.size(), Locale.ENGLISH);
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
	 * Set the {@linkplain MediaType media type} of the body,
	 * as specified by the {@code Content-Type} header.
	 */
	public void setContentType(/*@Nullable*/ MediaType mediaType) {
		if (mediaType != null) {
			AssertM.isTrue(!mediaType.isWildcardType(), "'Content-Type' cannot contain wildcard type '*'");
			AssertM.isTrue(!mediaType.isWildcardSubtype(), "'Content-Type' cannot contain wildcard subtype '*'");
			set(CONTENT_TYPE, mediaType.toString());
		} else {
			set(CONTENT_TYPE, null);
		}
	}

	/**
	 * Return the {@linkplain MediaType media type} of the body, as specified
	 * by the {@code Content-Type} header.
	 * <p>Returns {@code null} when the content-type is unknown.
	 */
	/*@Nullable*/
	public MediaType getContentType() {
		String value = getFirst(CONTENT_TYPE);
		return (StringUtils.hasLength(value) ? MediaType.parseMediaType(value) : null);
	}


	// MultiValueMap implementation

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
	 * @see #put(String, List)
	 * @see #set(String, String)
	 */
	@Override
	public void add(String headerName, /*@Nullable*/ String headerValue) {
		List<String> headerValues = this.headers.computeIfAbsent(headerName, k -> new LinkedList<>());
		headerValues.add(headerValue);
	}

	/**
	 * Set the given, single header value under the given name.
	 *
	 * @param headerName  the header name
	 * @param headerValue the header value
	 * @throws UnsupportedOperationException if adding headers is not supported
	 * @see #put(String, List)
	 * @see #add(String, String)
	 */
	public void set(String headerName, /*@Nullable*/ String headerValue) {
		List<String> headerValues = new LinkedList<>();
		headerValues.add(headerValue);
		this.headers.put(headerName, headerValues);
	}


	// Map implementation

	@Override
	public int size() {
		return this.headers.size();
	}

	@Override
	public boolean isEmpty() {
		return this.headers.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return this.headers.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.headers.containsValue(value);
	}

	@Override
	/*@Nullable*/
	public List<String> get(Object key) {
		return this.headers.get(key);
	}

	@Override
	public List<String> put(String key, List<String> value) {
		return this.headers.put(key, value);
	}

	@Override
	public List<String> remove(Object key) {
		return this.headers.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		this.headers.putAll(map);
	}

	@Override
	public void clear() {
		this.headers.clear();
	}

	@Override
	public Set<String> keySet() {
		return this.headers.keySet();
	}

	@Override
	public Collection<List<String>> values() {
		return this.headers.values();
	}

	@Override
	public Set<Entry<String, List<String>>> entrySet() {
		return this.headers.entrySet();
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof HttpHeaders)) {
			return false;
		}
		HttpHeaders otherHeaders = (HttpHeaders) other;
		return this.headers.equals(otherHeaders.headers);
	}

	@Override
	public int hashCode() {
		return this.headers.hashCode();
	}

	@Override
	public String toString() {
		return this.headers.toString();
	}


}
