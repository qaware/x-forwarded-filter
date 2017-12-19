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
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

/**
 * Represents an immutable collection of URI components, mapping component type to
 * String values. Contains convenience getters for all components. Effectively similar
 * to {@link URI}, but with more powerful encoding options and support for
 * URI template variables.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see UriComponentsBuilder
 */
@SuppressWarnings("serial")
public abstract class UriComponents implements Serializable {

	/** Captures URI template variable names */
	private static final Pattern NAMES_PATTERN = Pattern.compile("\\{([^/]+?)\\}");


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
	 * @return the encoded URI components
	 */
	public final UriComponents encode() {
		return encode(StandardCharsets.UTF_8);
	}

	/**
	 * Encode all URI components using their specific encoding rules, and
	 * returns the result as a new {@code UriComponents} instance.
	 * @param charset the encoding of the values contained in this map
	 * @return the encoded URI components
	 */
	public abstract UriComponents encode(Charset charset) ;

	/**
	 * Normalize the path removing sequences like "path/..". Note that calling this method will
	 * combine all path segments into a full path before doing the actual normalisation, i.e.
	 * individual path segments will not be normalized individually.
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


	// Static expansion helpers

	/*@Nullable*/
	static String expandUriComponent(/*@Nullable*/ String source, UriTemplateVariables uriVariables) {
		if (source == null) {
			return null;
		}
		if (source.indexOf('{') == -1) {
			return source;
		}
		if (source.indexOf(':') != -1) {
			source = sanitizeSource(source);
		}
		Matcher matcher = NAMES_PATTERN.matcher(source);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			String match = matcher.group(1);
			String variableName = getVariableName(match);
			Object variableValue = uriVariables.getValue(variableName);
			if (UriTemplateVariables.SKIP_VALUE.equals(variableValue)) {
				continue;
			}
			String variableValueString = getVariableValueAsString(variableValue);
			String replacement = Matcher.quoteReplacement(variableValueString);
			matcher.appendReplacement(sb, replacement);
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	/**
	 * Remove nested "{}" such as in URI vars with regular expressions.
	 */
	private static String sanitizeSource(String source) {
		int level = 0;
		StringBuilder sb = new StringBuilder();
		for (char c : source.toCharArray()) {
			if (c == '{') {
				level++;
			}
			if (c == '}') {
				level--;
			}
			if (level > 1 || (level == 1 && c == '}')) {
				continue;
			}
			sb.append(c);
		}
		return sb.toString();
	}

	private static String getVariableName(String match) {
		int colonIdx = match.indexOf(':');
		return (colonIdx != -1 ? match.substring(0, colonIdx) : match);
	}

	private static String getVariableValueAsString(/*@Nullable*/ Object variableValue) {
		return (variableValue != null ? variableValue.toString() : "");
	}


	/**
	 * Defines the contract for URI Template variables
	 * @see HierarchicalUriComponents#expand
	 */
	public interface UriTemplateVariables {

		Object SKIP_VALUE = UriTemplateVariables.class;

		/**
		 * Get the value for the given URI variable name.
		 * If the value is {@code null}, an empty String is expanded.
		 * If the value is {@link #SKIP_VALUE}, the URI variable is not expanded.
		 * @param name the variable name
		 * @return the variable value, possibly {@code null} or {@link #SKIP_VALUE}
		 */
		/*@Nullable*/
		Object getValue(/*@Nullable*/ String name);
	}


}
