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

package de.qaware.web.filter;

import de.qaware.web.util.HttpServletRequestUtil;
import de.qaware.web.util.UrlPathHelper;
import de.qaware.web.util.uri.UriComponentsBase;
import de.qaware.web.util.uri.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import static de.qaware.web.util.ForwardedHeader.X_FORWARDED_PREFIX;
import static de.qaware.web.util.uri.UriComponentsBase.PATH_DELIMITER_STRING;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Extract and use "Forwarded" or "X-Forwarded-*" headers.
 */
class ForwardedHeaderExtractingRequest extends HttpServletRequestWrapper {

	private static final int HTTPS_PORT = 443;
	private static final int HTTP_PORT = 80;
	/*@Nullable*/
	private final String scheme;

	private final boolean secure;

	/*@Nullable*/
	private final String host;

	private final int port;

	private final String contextPath;

	private final String requestUri;

	private final String requestUrl;

	@SuppressWarnings("squid:S3358")//nested ternary op is more readable in this case
	public ForwardedHeaderExtractingRequest(HttpServletRequest request, XForwardedPrefixStrategy prefixStrategy) {
		super(request);

		UrlPathHelper pathHelper = new UrlPathHelper();
		pathHelper.setUrlDecode(false);
		pathHelper.setRemoveSemicolonContent(false);

		UriComponentsBase uriComponents = UriComponentsBuilder.fromHttpRequest(request).build();
		int portFromUri = uriComponents.getPort();

		this.scheme = uriComponents.getScheme();
		this.secure = "https".equals(scheme);
		this.host = uriComponents.getHost();
		this.port = (portFromUri == -1 ? (this.secure ? HTTPS_PORT : HTTP_PORT) : portFromUri);

		this.contextPath = adaptFromXForwardedPrefix(request, prefixStrategy);

		this.requestUri = this.contextPath + pathHelper.getPathWithinApplication(request);
		this.requestUrl = this.scheme + "://" + this.host + (portFromUri == -1 ? "" : (":" + portFromUri)) + this.requestUri;
	}

	private static String adaptFromXForwardedPrefix(HttpServletRequest request, XForwardedPrefixStrategy prefixStrategy) {
		String prefix = getForwardedPrefix(request);
		if (prefix == null) {
			return request.getContextPath();
		}
		switch (prefixStrategy) {
			case PREPEND:
				return prefix + request.getContextPath();
			case REPLACE:
				return prefix;
			default:
				throw new UnsupportedOperationException("Implementation for enum case is missing: " + prefixStrategy);
		}

	}

	/*@Nullable*/
	private static String getForwardedPrefix(HttpServletRequest request) {
		String prefix = HttpServletRequestUtil.getHeaders(request).getFirst(X_FORWARDED_PREFIX.headerName());
		if (isNotBlank(prefix)) {
			prefix = HttpServletRequestUtil.getFirstValueToken(prefix, ",");
			while (prefix.endsWith(PATH_DELIMITER_STRING)) {
				prefix = prefix.substring(0, prefix.length() - 1);
			}
		}
		return prefix;
	}

	@Override
	/*@Nullable*/
	public String getScheme() {
		return this.scheme;
	}

	@Override
	/*@Nullable*/
	public String getServerName() {
		return this.host;
	}

	@Override
	public int getServerPort() {
		return this.port;
	}

	@Override
	public boolean isSecure() {
		return this.secure;
	}

	@Override
	public String getContextPath() {
		return this.contextPath;
	}

	@Override
	public String getRequestURI() {
		return this.requestUri;
	}

	@Override
	public StringBuffer getRequestURL() {
		return new StringBuffer(this.requestUrl);
	}
}
