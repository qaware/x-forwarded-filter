package de.qaware.web.filter;/*
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

import de.qaware.web.util.WebUtilsConstants;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

import static de.qaware.web.filter.ForwardedHeaderConstants.ENABLE_RELATIVE_REDIRECTS_INIT_PARAM;
import static de.qaware.web.filter.ForwardedHeaderConstants.REMOVE_ONLY_INIT_PARAM;
import static java.lang.Boolean.parseBoolean;

/**
 * Extract values from "Forwarded" and "X-Forwarded-*" headers in order to wrap
 * and override the following from the request and response:
 * {@link HttpServletRequest#getServerName() getServerName()},
 * {@link HttpServletRequest#getServerPort() getServerPort()},
 * {@link HttpServletRequest#getScheme() getScheme()},
 * {@link HttpServletRequest#isSecure() isSecure()}, and
 * {@link HttpServletResponse#sendRedirect(String) sendRedirect(String)}.
 * In effect the wrapped request and response reflect the client-originated
 * protocol and address.
 * <p>
 * <p><strong>Note:</strong> This filter can also be used in a
 * {@link ForwardedHeaderFilter#removeOnly } mode where "Forwarded" and "X-Forwarded-*"
 * headers are only eliminated without being used.
 *
 * @author Rossen Stoyanchev
 * @author Eddú Meléndez
 * @author Rob Winch
 * @see <a href="https://tools.ietf.org/html/rfc7239">https://tools.ietf.org/html/rfc7239</a>
 * @since 4.3
 */
public class ForwardedHeaderFilter extends OncePerRequestFilter {

	private boolean removeOnly;
	private boolean relativeRedirects;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		super.init(filterConfig);
		relativeRedirects = parseBoolean(filterConfig.getInitParameter(ENABLE_RELATIVE_REDIRECTS_INIT_PARAM));
		removeOnly = parseBoolean(filterConfig.getInitParameter(REMOVE_ONLY_INIT_PARAM));
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		Enumeration<String> names = request.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			if (ForwardedHeaderConstants.FORWARDED_HEADER_NAMES.contains(name)) {
				return false;
			}
		}
		return true;
	}


	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		if (this.removeOnly) {
			ForwardedHeaderRemovingRequest theRequest = new ForwardedHeaderRemovingRequest(request);
			filterChain.doFilter(theRequest, response);
		} else {
			HttpServletRequest theRequest = new ForwardedHeaderExtractingRequest(request);
			HttpServletResponse theResponse = (this.relativeRedirects ?
					RelativeRedirectResponseWrapper.wrapIfNecessary(response, WebUtilsConstants.SEE_OTHER) :
					new ForwardedHeaderExtractingResponse(response, theRequest));
			filterChain.doFilter(theRequest, theResponse);
		}
	}

}
