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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

/**
 * Utility class for URI encoding and decoding based on RFC 3986.
 * Offers encoding methods for the various URI components.
 *
 * <p>All {@code encode*(String, String)} methods in this class operate in a similar way:
 * <ul>
 * <li>Valid characters for the specific URI component as defined in RFC 3986 stay the same.</li>
 * <li>All other characters are converted into one or more bytes in the given encoding scheme.
 * Each of the resulting bytes is written as a hexadecimal string in the "<code>%<i>xy</i></code>"
 * format.</li>
 * </ul>
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>
 */
public abstract class UriUtils {


	/**
	 * Decode the given encoded URI component.
	 * <p>See {@link StringUtils#uriDecode(String, Charset)} for the decoding rules.
	 * @param source the encoded String
	 * @param encoding the character encoding to use
	 * @return the decoded value
	 * @throws IllegalArgumentException when the given source contains invalid encoded sequences
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 * @see StringUtils#uriDecode(String, Charset)
	 * @see java.net.URLDecoder#decode(String, String)
	 */
	public static String decode(String source, String encoding) throws UnsupportedEncodingException {
		return StringUtils.uriDecode(source, Charset.forName(encoding));
	}


}
