/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.qaware.xff.util.uri;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.Validate;

import java.io.Serializable;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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

	public static final char PATH_DELIMITER = '/';
	public static final String PATH_DELIMITER_STRING = "/";

	/**
	 * Captures URI template variable names
	 */
	private static final Pattern NAMES_PATTERN = Pattern.compile("\\{([^/]+?)\\}");
	private static final int HEY_RADIX = 16;


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
	 * Encode all URI components using their specific encoding rules, and returns the
	 * result as a new {@code UriComponents} instance. This method uses UTF-8 to encode.
	 *
	 * @return the encoded URI components
	 */
	public final UriComponents encode() {
		return encode(StandardCharsets.UTF_8);
	}

	/**
	 * Replace all URI template variables with the values from a given map.
	 * <p>The given map keys represent variable names; the corresponding values
	 * represent variable values. The order of variables is not significant.
	 *
	 * @param uriVariables the map of URI variables
	 * @return the expanded URI components
	 */
	public final UriComponents expand(Map<String, ?> uriVariables) {
		Validate.notNull(uriVariables, "'uriVariables' must not be null");
		return expandInternal(new MapTemplateVariables(uriVariables));
	}

	/**
	 * Replace all URI template variables with the values from a given array.
	 * <p>The given array represents variable values. The order of variables is significant.
	 *
	 * @param uriVariableValues the URI variable values
	 * @return the expanded URI components
	 */
	public final UriComponents expand(Object... uriVariableValues) {
		Validate.notNull(uriVariableValues, "'uriVariableValues' must not be null");
		return expandInternal(new VarArgsTemplateVariables(uriVariableValues));
	}

	/**
	 * Replace all URI template variables with the values from the given
	 * {@link UriTemplateVariables}.
	 *
	 * @param uriVariables the URI template values
	 * @return the expanded URI components
	 */
	public final UriComponents expand(UriTemplateVariables uriVariables) {
		Validate.notNull(uriVariables, "'uriVariables' must not be null");
		return expandInternal(uriVariables);
	}

	//squid:S881  (increment < length) is a standard loop construct
	@SuppressWarnings({"squid:S881"})
	static void verifyUriComponent(/*@Nullable*/ String source, URIComponentType type) {
		if (source == null) {
			return;
		}
		int length = source.length();
		int pos = -1;
		while (++pos < length) {
			char currentChar = source.charAt(pos);
			if (currentChar == '%') {
				pos = verifyPercentEncodeSequence(source, length, pos);
			} else if (!type.isAllowedCharacter(currentChar)) {
				throw new IllegalArgumentException("Invalid character '" + currentChar + "' for " +
						type.name() + " in \"" + source + "\"");
			}
		}
	}

	//squid:S109: "magic number"  required for decoder logic
	@SuppressWarnings({"squid:S109"})
	private static int verifyPercentEncodeSequence(String source, int length, int pos) {
		if ((pos + 2) < length) {
			verifyPercentEncodedChar(source, pos);
			pos += 2;
		} else {
			throw new IllegalArgumentException("Invalid encoded sequence \"" +
					source.substring(pos) + "\"");
		}
		return pos;
	}

	//squid:S109: "magic number"  required for decoder logic
	@SuppressWarnings({"squid:S109"})
	private static void verifyPercentEncodedChar(String source, int pos) {
		char hex1 = source.charAt(pos + 1);
		char hex2 = source.charAt(pos + 2);
		int u = Character.digit(hex1, HEY_RADIX);
		int l = Character.digit(hex2, HEY_RADIX);
		if (u == -1 || l == -1) {
			throw new IllegalArgumentException("Invalid encoded sequence \"" +
					source.substring(pos) + "\"");
		}
	}

	/**
	 * Replace all URI template variables with the values from the given {@link
	 * UriTemplateVariables}
	 *
	 * @param uriVariables URI template values
	 * @return the expanded URI components
	 */
	abstract UriComponents expandInternal(UriTemplateVariables uriVariables);

	@Override
	public final String toString() {
		return toUriString();
	}

	/**
	 * Set all components of the given UriComponentsBuilder.
	 *
	 * @since 4.2
	 */
	protected abstract void copyToUriComponentsBuilder(UriComponentsBuilder builder);


	// Static expansion helpers

	/*@Nullable*/
	static String expandUriComponent(/*@Nullable*/ String source, UriTemplateVariables uriVariables) {
		String checkedSource = source;
		if (checkedSource == null) {
			return null;
		}
		if (checkedSource.indexOf('{') == -1) {
			return checkedSource;
		}
		if (checkedSource.indexOf(':') != -1) {
			checkedSource = sanitizeSource(checkedSource);
		}
		Matcher variableNamesMatcher = NAMES_PATTERN.matcher(checkedSource);
		StringBuffer sb = new StringBuffer();
		while (variableNamesMatcher.find()) {
			String match = variableNamesMatcher.group(1);
			String variableName = getVariableName(match);
			Object variableValue = uriVariables.getValue(variableName);
			if (UriTemplateVariables.SKIP_VALUE.equals(variableValue)) {
				continue;
			}
			String variableValueString = getVariableValueAsString(variableValue);
			String replacement = Matcher.quoteReplacement(variableValueString);
			variableNamesMatcher.appendReplacement(sb, replacement);
		}
		variableNamesMatcher.appendTail(sb);
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
	 * Return the scheme specific part. Can be {@code null}.
	 */
	/*@Nullable*/
	public abstract String getSchemeSpecificPart();

	/**
	 * Return the user info. Can be {@code null}.
	 */
	/*@Nullable*/
	public abstract String getUserInfo();

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
	 * Return the path. Can be {@code null}.
	 */
	/*@Nullable*/
	public abstract String getPath();

	/**
	 * Return the list of path segments. Empty if no path has been set.
	 */
	public abstract List<String> getPathSegments();

	/**
	 * Return the query. Can be {@code null}.
	 */
	/*@Nullable*/
	public abstract String getQuery();

	/**
	 * Return the map of query parameters. Empty if no query has been set.
	 */
	public abstract Map<String, List<String>> getQueryParamsMap();

	/**
	 * Return the map of query parameters. Empty if no query has been set.
	 */
	public abstract MultiValuedMap<String, String> getQueryParams();

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
	 * @return Normalized version
	 */
	public abstract UriComponents normalize();

	/**
	 * Return a URI String from this {@code UriComponents} instance.
	 *
	 * @return URI String
	 */
	public abstract String toUriString();

	/**
	 * Return a {@code URI} from this {@code UriComponents} instance.
	 *
	 * @return {@code URI}
	 */
	public abstract URI toUri();
}
