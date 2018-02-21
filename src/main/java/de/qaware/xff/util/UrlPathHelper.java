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
package de.qaware.xff.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;


/**
 * Helper class for URL path matching. Provides support for URL paths in
 * RequestDispatcher includes and support for consistent URL decoding.
 * <p>
 *
 * @author Michael Frank
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rossen Stoyanchev
 * @since 14.01.2004
 */
public class UrlPathHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(UrlPathHelper.class);

	public static final String PATH_DELIMITER_STRING = "/";
	private static final int HEX_RADIX = 16;
	private static final String JSESSIONID = ";jsessionid=";

	private boolean urlDecode = true;

	private boolean removeSemicolonContent = true;

	private String defaultEncoding = WebUtilsConstants.DEFAULT_CHARACTER_ENCODING;


	/**
	 * Whether the context path and request URI should be decoded -- both of
	 * which are returned <i>un-decoded</i> by the Servlet API, in contrast to
	 * the servlet path.
	 * <p>Either the request encoding or the default Servlet spec encoding
	 * (ISO-8859-1) is used when set to "true".
	 * <p>By default this is set to {@literal true}.
	 * <p><strong>Note:</strong> Be aware the servlet path will not match when
	 * compared to encoded paths. Therefore use of {@code urlDecode=false} is
	 * not compatible with a prefix-based Servlet mapping and likewise implies
	 * also setting {@code alwaysUseFullPath=true}.
	 *
	 * @see #getContextPath
	 * @see #getRequestUri
	 * @see WebUtilsConstants#DEFAULT_CHARACTER_ENCODING
	 * @see javax.servlet.ServletRequest#getCharacterEncoding()
	 * @see java.net.URLDecoder#decode(String, String)
	 */
	public void setUrlDecode(boolean urlDecode) {
		this.urlDecode = urlDecode;
	}

	/**
	 * Set if ";" (semicolon) content should be stripped from the request URI.
	 * <p>Default is "true".
	 *
	 * @param removeSemicolonContent true=remove content after ';' in URI
	 */
	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
		this.removeSemicolonContent = removeSemicolonContent;
	}

	/**
	 * Return the default character encoding to use for URL decoding.
	 */
	public String getDefaultEncoding() {
		return this.defaultEncoding;
	}

	/**
	 * Set the default character encoding to use for URL decoding.
	 * Default is ISO-8859-1, according to the Servlet spec.
	 * <p>If the request specifies a character encoding itself, the request
	 * encoding will override this setting. This also allows for generically
	 * overriding the character encoding in a filter that invokes the
	 * {@code ServletRequest.setCharacterEncoding} method.
	 *
	 * @param defaultEncoding the character encoding to use
	 * @see #determineEncoding
	 * @see javax.servlet.ServletRequest#getCharacterEncoding()
	 * @see javax.servlet.ServletRequest#setCharacterEncoding(String)
	 * @see WebUtilsConstants#DEFAULT_CHARACTER_ENCODING
	 */
	public void setDefaultEncoding(String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * Return the path within the web application for the given request.
	 * <p>Detects include request URL if called within a RequestDispatcher include.
	 *
	 * @param request current HTTP request
	 * @return the path within the web application
	 */
	public String getPathWithinApplication(HttpServletRequest request) {
		String contextPath = getContextPath(request);
		String requestUri = getRequestUri(request);
		String path = getRemainingPath(requestUri, contextPath, true);
		if (path != null) {
			// Normal case: URI contains context path.
			return (StringUtils.isNotEmpty(path) ? path : PATH_DELIMITER_STRING);
		} else {
			return requestUri;
		}
	}

	/**
	 * Match the given "mapping" to the start of the "requestUri" and if there
	 * is a match return the extra part. This method is needed because the
	 * context path and the servlet path returned by the HttpServletRequest are
	 * stripped of semicolon content unlike the requestUri.
	 */
	private String getRemainingPath(String requestUri, String mapping, boolean ignoreCase) {
		int index1 = 0;
		int index2 = 0;
		for (; (index1 < requestUri.length()) && (index2 < mapping.length()); index1++, index2++) {
			char c1 = requestUri.charAt(index1);
			char c2 = mapping.charAt(index2);
			if (c1 == ';') {
				index1 = requestUri.indexOf('/', index1);
				if (index1 == -1) {
					return null;
				}
				c1 = requestUri.charAt(index1);
			}
			if (c1 == c2
					|| (ignoreCase && (Character.toLowerCase(c1) == Character.toLowerCase(c2)))) {
				continue;
			}

			return null;
		}
		if (index2 != mapping.length()) {
			return null;
		} else if (index1 == requestUri.length()) {
			return "";
		} else if (requestUri.charAt(index1) == ';') {
			index1 = requestUri.indexOf('/', index1);
		}
		return (index1 != -1 ? requestUri.substring(index1) : "");
	}

	/**
	 * Sanitize the given path with the following rules:
	 * <ul>
	 * <li>replace all "//" by "/"</li>
	 * </ul>
	 */
	private String getSanitizedPath(final String path) {
		String sanitized = path;
		while (true) {
			int index = sanitized.indexOf("//");
			if (index < 0) {
				break;
			} else {
				sanitized = sanitized.substring(0, index) + sanitized.substring(index + 1);
			}
		}
		return sanitized;
	}

	/**
	 * Return the request URI for the given request, detecting an include request
	 * URL if called within a RequestDispatcher include.
	 * <p>As the value returned by {@code request.getRequestURI()} is <i>not</i>
	 * decoded by the servlet container, this method will decode it.
	 * <p>The URI that the web container resolves <i>should</i> be correct, but some
	 * containers like JBoss/Jetty incorrectly include ";" strings like ";jsessionid"
	 * in the URI. This method cuts off such incorrect appendices.
	 *
	 * @param request current HTTP request
	 * @return the request URI
	 */
	public String getRequestUri(HttpServletRequest request) {
		String uri = (String) request.getAttribute(WebUtilsConstants.INCLUDE_REQUEST_URI_ATTRIBUTE);
		if (uri == null) {
			uri = request.getRequestURI();
		}
		return decodeAndCleanUriString(request, uri);
	}

	/**
	 * Return the context path for the given request, detecting an include request
	 * URL if called within a RequestDispatcher include.
	 * <p>As the value returned by {@code request.getContextPath()} is <i>not</i>
	 * decoded by the servlet container, this method will decode it.
	 *
	 * @param request current HTTP request
	 * @return the context path
	 */
	public String getContextPath(HttpServletRequest request) {
		String contextPath = (String) request.getAttribute(WebUtilsConstants.INCLUDE_CONTEXT_PATH_ATTRIBUTE);
		if (contextPath == null) {
			contextPath = request.getContextPath();
		}
		if (PATH_DELIMITER_STRING.equals(contextPath)) {
			// Invalid case, but happens for includes on Jetty: silently adapt it.
			contextPath = "";
		}
		return decodeRequestString(request, contextPath);
	}


	/**
	 * Decode the supplied URI string and strips any extraneous portion after a ';'.
	 */
	private String decodeAndCleanUriString(HttpServletRequest request, final String uri) {
		String cleanedUri = removeSemicolonContent(uri);
		cleanedUri = decodeRequestString(request, cleanedUri);
		cleanedUri = getSanitizedPath(cleanedUri);
		return cleanedUri;
	}

	/**
	 * Decode the given source string with a URLDecoder. The encoding will be taken
	 * from the request, falling back to the default "ISO-8859-1".
	 * <p>The default implementation uses {@code URLDecoder.decode(input, enc)}.
	 *
	 * @param request current HTTP request
	 * @param source  the String to decode
	 * @return the decoded String
	 * @see WebUtilsConstants#DEFAULT_CHARACTER_ENCODING
	 * @see javax.servlet.ServletRequest#getCharacterEncoding
	 * @see java.net.URLDecoder#decode(String, String)
	 * @see java.net.URLDecoder#decode(String)
	 */
	public String decodeRequestString(HttpServletRequest request, String source) {
		if (this.urlDecode) {
			return decodeInternal(request, source);
		}
		return source;
	}

	private String decodeInternal(HttpServletRequest request, String source) {
		String charset = determineEncoding(request);
		//this method behaves differently then javas URLDecoder class
		// it does NOT transform '+' into '  ' which is very important!
		return uriDecode(source, charset);
	}


	/**
	 * Decode the given encoded URI component value. Based on the following rules:
	 * <ul>
	 * <li>Alphanumeric characters {@code "a"} through {@code "z"}, {@code "A"} through {@code "Z"},
	 * and {@code "0"} through {@code "9"} stay the same.</li>
	 * <li>Special characters {@code "-"}, {@code "_"}, {@code "."}, and {@code "*"} stay the same.</li>
	 * <li>A sequence "{@code %<i>xy</i>}" is interpreted as a hexadecimal representation of the character.</li>
	 * <li>ie>Does NOT! convert  '+' into ' ' (space)</li>
	 * </ul>
	 *
	 * @param source  the encoded String
	 * @param charset the character set
	 * @return the decoded value
	 * @throws IllegalArgumentException when the given source contains invalid encoded sequences
	 * @see java.net.URLDecoder#decode(String, String)
	 * @since 5.0
	 */


	//squid:S109: "magic number"  required for decoder logic
	//squid:S881  (increment < length) is a standard loop construct
	@SuppressWarnings({"squid:S109", "squid:S881"})
	String uriDecode(String source, String charset) {
		Validate.notNull(charset, "Charset must not be null");

		final int length = source.length();
		if (length == 0) {
			return source;
		}

		ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
		boolean changed = false;
		int pos = -1;
		while (++pos < length) {
			char currentChar = source.charAt(pos);
			//format: %xy
			if (currentChar == '%') {
				//process the next 2 chars following the '%' at once to decode the original value
				bos.write(decodePercentEncodedChar(source, pos));
				pos += 2;
				changed = true;
			} else {
				bos.write(currentChar);
			}
		}

		return (changed ? decodeToString(source, bos, charset) : source);
	}

	private String decodeToString(String source, ByteArrayOutputStream bos, String providedCharset) {
		try {
			return bos.toString(providedCharset);
		} catch (UnsupportedEncodingException | IllegalArgumentException ex) {
			final String defaultCharset = getDefaultEncoding();
			if (defaultCharset.equals(providedCharset)) {
				throw new AssertionError("Could not decode request string. Default encoding '" + defaultCharset + "' should always be available.", ex);
			}
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn("Could not decode request string with encoding '" + providedCharset +
						"': falling back to default encoding" + defaultCharset + ". Request string: [" + source + "]; exception message: " + ex.getMessage(), ex);
			}
			return decodeToString(source, bos, defaultCharset);
		}
	}


	/**
	 * process the next 2 chars following the '%' at once to decode the original value
	 */
	@SuppressWarnings("squid:S109")//"magic number"  required for decoder logic
	private static char decodePercentEncodedChar(String source, int pos) {
		//process the next 2 chars following the '%' at once to decode the original value
		checkBoundsOfSequence(source, pos + 2);
		char hex1 = source.charAt(pos + 1);
		char hex2 = source.charAt(pos + 2);
		int high4Bits = Character.digit(hex1, HEX_RADIX);
		int low4Bits = Character.digit(hex2, HEX_RADIX);
		if (high4Bits == -1 || low4Bits == -1) {
			throw illegalEncodingSequence(source, pos);
		}
		//reverse the '%xy' encoding to reconstruct the original char
		return (char) ((high4Bits << 4) + low4Bits);
	}


	private static void checkBoundsOfSequence(String source, int pos) {
		if (pos >= source.length()) {
			throw illegalEncodingSequence(source, pos);
		}
	}

	private static IllegalArgumentException illegalEncodingSequence(String source, int pos) {
		return new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(pos) + "\"");
	}


	/**
	 * Determine the encoding for the given request.
	 * Can be overridden in subclasses.
	 * <p>The default implementation checks the request encoding,
	 * falling back to the default encoding specified for this resolver.
	 *
	 * @param request current HTTP request
	 * @return the encoding for the request (never {@code null})
	 * @see javax.servlet.ServletRequest#getCharacterEncoding()
	 */
	protected String determineEncoding(HttpServletRequest request) {
		String enc = request.getCharacterEncoding();
		if (enc == null) {
			enc = getDefaultEncoding();
		}
		return enc;
	}

	/**
	 * Remove ";" (semicolon) content from the given request URI if the
	 * {@linkplain #setRemoveSemicolonContent(boolean) removeSemicolonContent}
	 * property is set to "true". Note that "jsessionid" is always removed.
	 *
	 * @param requestUri the request URI string to remove ";" content from
	 * @return the updated URI string
	 */
	public String removeSemicolonContent(String requestUri) {
		return (this.removeSemicolonContent ?
				removeSemicolonContentInternal(requestUri) : removeJsessionid(requestUri));
	}

	private String removeSemicolonContentInternal(final String requestUri) {
		String cleanedUri = requestUri;
		int semicolonIndex = cleanedUri.indexOf(';');
		while (semicolonIndex != -1) {
			int slashIndex = cleanedUri.indexOf('/', semicolonIndex);
			String start = cleanedUri.substring(0, semicolonIndex);
			cleanedUri = (slashIndex != -1) ? (start + cleanedUri.substring(slashIndex)) : start;
			semicolonIndex = cleanedUri.indexOf(';', semicolonIndex);
		}
		return cleanedUri;
	}

	private String removeJsessionid(final String uri) {
		String cleanedUri = uri;
		int startIndex = cleanedUri.toLowerCase(Locale.ENGLISH).indexOf(JSESSIONID);
		if (startIndex != -1) {
			int endIndex = cleanedUri.indexOf(';', startIndex + JSESSIONID.length());
			String start = cleanedUri.substring(0, startIndex);
			cleanedUri = (endIndex != -1) ? (start + cleanedUri.substring(endIndex)) : start;
		}
		return cleanedUri;
	}

}
