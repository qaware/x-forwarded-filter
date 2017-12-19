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

package org.springframework.web.util;

import java.io.Serializable;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Represents an immutable collection of URI components, mapping component type to
 * String values. Contains convenience getters for all components. Effectively similar
 * to {@link URI}, but with more powerful encoding options and support for
 * URI template variables.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @see UriComponentsBuilder
 * @since 3.1
 */
@SuppressWarnings("serial")
public abstract class UriComponents implements Serializable {


	/*@Nullable*/
	private final String scheme;

	/*@Nullable*/
	private final String fragment;


	protected UriComponents(/*@Nullable*/ String scheme, /*@Nullable*/ String fragment) {
		this.scheme = scheme;
		this.fragment = fragment;
	}


	// Component getters

	/**
	 * Return the scheme. Can be {@code null}.
	 */
	/*@Nullable*/
	public final String getScheme() {
		return this.scheme;
	}

	/**
	 * Return the fragment. Can be {@code null}.
	 */
	/*@Nullable*/
	public final String getFragment() {
		return this.fragment;
	}

	/**
	 * Return the host. Can be {@code null}.
	 */
	/*@Nullable*/
	public abstract String getHost();

	/**
	 * Return the port. {@code -1} if no port has been set.
	 */
	public abstract int getPort();


	/**
	 * Encode all URI components using their specific encoding rules, and returns the
	 * result as a new {@code UriComponents} instance. This method uses UTF-8 to encode.
	 *
	 * @return the encoded URI components
	 */
	public final UriComponents encode() {
		return encode(StandardCharsets.UTF_8);
	}

	/**
	 * Encode all URI components using their specific encoding rules, and
	 * returns the result as a new {@code UriComponents} instance.
	 *
	 * @param charset the encoding of the values contained in this map
	 * @return the encoded URI components
	 */
	public abstract UriComponents encode(Charset charset);

	/**
	 * Normalize the path removing sequences like "path/..". Note that calling this method will
	 * combine all path segments into a full path before doing the actual normalisation, i.e.
	 * individual path segments will not be normalized individually.
	 *
	 * @see org.springframework.util.StringUtils#cleanPath(String)
	 */
	public abstract UriComponents normalize();

	/**
	 * Return a URI String from this {@code UriComponents} instance.
	 */
	public abstract String toUriString();

	@Override
	public final String toString() {
		return toUriString();
	}


}
