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

package de.qaware.http.server;

import de.qaware.http.HttpHeaders;
import de.qaware.http.InvalidMediaTypeException;
import de.qaware.http.MediaType;
import de.qaware.util.Assert;
import de.qaware.util.LinkedCaseInsensitiveMap;
import de.qaware.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Map;

/**
 * {@link ServerHttpRequest} implementation that is based on a {@link HttpServletRequest}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.0
 */
public class ServletServerHttpRequest implements ServerHttpRequest {


	private final HttpServletRequest servletRequest;

	/*@Nullable*/
	private HttpHeaders headers;


	/**
	 * Construct a new instance of the ServletServerHttpRequest based on the given {@link HttpServletRequest}.
	 *
	 * @param servletRequest the servlet request
	 */
	public ServletServerHttpRequest(HttpServletRequest servletRequest) {
		Assert.notNull(servletRequest, "HttpServletRequest must not be null");
		this.servletRequest = servletRequest;
	}


	@Override
	public URI getURI() {
		try {
			StringBuffer url = this.servletRequest.getRequestURL();
			String query = this.servletRequest.getQueryString();
			if (StringUtils.hasText(query)) {
				url.append('?').append(query);
			}
			return new URI(url.toString());
		} catch (URISyntaxException ex) {
			throw new IllegalStateException("Could not get HttpServletRequest URI: " + ex.getMessage(), ex);
		}
	}

	@Override
	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			this.headers = getHeadersFromServletRequest(servletRequest);
		}
		return this.headers;
	}

	private static HttpHeaders getHeadersFromServletRequest(HttpServletRequest servletRequest) {
		HttpHeaders headers = new HttpHeaders();
		setHeaderNames(headers, servletRequest);
		setContentType(headers, servletRequest);
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

	private static void setContentType(HttpHeaders headers, HttpServletRequest servletRequest) {
		// HttpServletRequest exposes some headers as properties: we should include those if not already present
		try {
			MediaType contentType = headers.getContentType();
			if (contentType == null) {
				String requestContentType = servletRequest.getContentType();
				if (StringUtils.hasLength(requestContentType)) {
					contentType = MediaType.parseMediaType(requestContentType);

					headers.setContentType(contentType);
				}
			}
			if (contentType != null && contentType.getCharset() == null) {
				String requestEncoding = servletRequest.getCharacterEncoding();
				if (StringUtils.hasLength(requestEncoding)) {
					Charset charSet = Charset.forName(requestEncoding);
					Map<String, String> params = new LinkedCaseInsensitiveMap<>();
					params.putAll(contentType.getParameters());
					params.put("charset", charSet.toString());
					MediaType newContentType = new MediaType(contentType.getType(), contentType.getSubtype(), params);

					headers.setContentType(newContentType);
				}
			}
		} catch (InvalidMediaTypeException ex) {
			// Ignore: simply not exposing an invalid content type in HttpHeaders...
		}
	}


}
