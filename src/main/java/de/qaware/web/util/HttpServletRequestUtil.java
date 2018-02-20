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

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Helper Class containing methods to extract information from HttpServletRequest's
 */
public final class HttpServletRequestUtil {

	private HttpServletRequestUtil() {
		//utility class
	}

	/**
	 * Reconstructs full url: e.g. http://server:port/foo/bar;xxx=yyy?param1=value1&param2=value2
	 * <pre>
	 * request.getRequestURL() = ttp://server:port/foo/bar;xxx=yyy
	 * request.getQueryString() = param1=value1&param2=value2
	 * request.getRequestURL() + "?" + request.getQueryString();
	 * </pre>
	 * <p>
	 * Difference of this method to {@link HttpServletRequest#getRequestURI()} <br/>
	 * {@link HttpServletRequest#getRequestURI()} returns: /foo/bar;xxx=yyy?param1=value1&param2=value2
	 *
	 * @param request {@see HttpServletRequest}
	 * @return request.getRequestURL() + "?" + request.getQueryString();
	 */
	public static URI getURI(HttpServletRequest request) {
		try {
			StringBuffer url = request.getRequestURL();
			String query = request.getQueryString();
			if (!isBlank(query)) {
				url.append('?').append(query);
			}
			return new URI(url.toString());
		} catch (URISyntaxException ex) {
			throw new IllegalStateException("Could not get HttpServletRequest URI: " + ex.getMessage(), ex);
		}
	}


	/**
	 * Extract headers from the given request
	 *
	 * @param servletRequest {@see HttpServletRequest}
	 * @return {@see HttpHeaders} extracted from {@see HttpServletRequest}
	 */
	public static HttpHeaders getHeaders(HttpServletRequest servletRequest) {
		HttpHeaders headers = new HttpHeaders();
		setHeaderNames(headers, servletRequest);
		setContentLength(headers, servletRequest);
		return headers;
	}

	private static void setHeaderNames(HttpHeaders headers, HttpServletRequest servletRequest) {
		for (Enumeration<?> headerNames = servletRequest.getHeaderNames(); headerNames.hasMoreElements(); ) {
			String headerName = (String) headerNames.nextElement();
			for (Enumeration<?> headerValues = servletRequest.getHeaders(headerName);
			     headerValues.hasMoreElements(); ) {
				String headerValue = (String) headerValues.nextElement();
				headers.add(headerName, headerValue);
			}
		}
	}

	private static void setContentLength(HttpHeaders headers, HttpServletRequest servletRequest) {
		if (headers.getContentLength() < 0) {
			int requestContentLength = servletRequest.getContentLength();
			if (requestContentLength != -1) {
				headers.setContentLength(requestContentLength);
			}
		}
	}

	/**
	 * Returns first token only. e.g: value="123, 345, 678"  with delim="," will return "123"
	 * or more formally: return value.substring(0,value.indexOf(delim));
	 *
	 * @param value     value to be split
	 * @param delimiter delimiter
	 * @return first token or the original string
	 */
	public static String getFirstValueToken(String value, String delimiter) {
		if (value == null) {
			return null;
		}
		int pos = value.indexOf(delimiter);
		return (pos == -1) ? value : value.substring(0, pos);
	}
}
