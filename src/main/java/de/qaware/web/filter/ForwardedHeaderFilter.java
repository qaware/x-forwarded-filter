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

import de.qaware.web.util.ForwardedHeader;
import de.qaware.web.util.WebUtilsConstants;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Optional;

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
 * {@link HeaderProcessingStrategy#DONT_EVAL_AND_REMOVE } mode where "Forwarded" and "X-Forwarded-*"
 * headers are only eliminated without being used.
 *
 * @author Michael Frank
 * @author Rossen Stoyanchev
 * @author Eddú Meléndez
 * @author Rob Winch
 * @see <a href="https://tools.ietf.org/html/rfc7239">https://tools.ietf.org/html/rfc7239</a>
 * @since 4.3
 */
public class ForwardedHeaderFilter extends OncePerRequestFilter {
	/**
	 * Use this init param to enable relative redirects as explained in and also
	 * using the same response wrapper as {@link RelativeRedirectFilter} does.
	 * Or if both filters are used, only one will wrap the response.
	 * <p>By default, if this property is set to false, in which case calls to
	 * {@link HttpServletResponse#sendRedirect(String)} are overridden in order
	 * to turn relative into absolute URLs since (which Servlet containers are
	 * also required to do) also taking forwarded headers into consideration.
	 */
	public static final String ENABLE_RELATIVE_REDIRECTS_INIT_PARAM = "enableRelativeRedirects";

	/**
	 * --
	 * Different processing strategies:
	 * <ul>
	 * <li>EVAL_AND_KEEP: Evaluate headers remove afterwards so they will be visible to downstream
	 * filters and the application.
	 * </li>
	 * <li>EVAL_AND_REMOVE: Evaluate headers remove afterwards so they wont be visible to downstream
	 * filters and the application.
	 * </li>
	 * <li>DONT_EVAL_AND_REMOVE: Enables mode in which any "Forwarded" or "X-Forwarded-*" headers
	 * are removed only and the information in them ignored.
	 * </li>
	 * </ul>
	 */
	public static final String HEADER_PROCESSING_STRATEGY = "headerProcessingStrategy";

	/**
	 * selects processing mode how to handle the x-forwarded-proto header.
	 */
	public static final String X_FORWARDED_PREFIX_STRATEGY = "xForwardedProtoStrategy";


	private boolean relativeRedirects;
	private XForwardedPrefixStrategy prefixStrategy;
	private HeaderProcessingStrategy headerProcessingStrategy;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		super.init(filterConfig);
		relativeRedirects = parseBoolean(filterConfig.getInitParameter(ENABLE_RELATIVE_REDIRECTS_INIT_PARAM));
		headerProcessingStrategy = Optional.ofNullable(filterConfig.getInitParameter(HEADER_PROCESSING_STRATEGY))//
				.map(HeaderProcessingStrategy::valueOf)//
				.orElse(HeaderProcessingStrategy.EVAL_AND_REMOVE);
		prefixStrategy = Optional.ofNullable(filterConfig.getInitParameter(X_FORWARDED_PREFIX_STRATEGY))//
				.map(XForwardedPrefixStrategy::valueOf)//
				.orElse(XForwardedPrefixStrategy.REPLACE);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		Enumeration<String> names = request.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			if (ForwardedHeader.isForwardedHeader(name)) {
				return false;
			}
		}
		return true;
	}


	protected void doFilterInternal(HttpServletRequest originalRequest, HttpServletResponse originalResponse, FilterChain filterChain) throws ServletException, IOException {
		HttpServletRequest request = originalRequest;
		HttpServletResponse response = originalResponse;

		if (headerProcessingStrategy.isEvaluateHeaders()) {
			request = new ForwardedHeaderExtractingRequest(request, prefixStrategy);
			if (relativeRedirects) {
				response = RelativeRedirectResponseWrapper.wrapIfNecessary(response, WebUtilsConstants.SEE_OTHER);
			} else {
				response = new ForwardedHeaderExtractingResponse(response, request);
			}
		}

		if (headerProcessingStrategy.isRemoveHeaders()) {
			request = new ForwardedHeaderRemovingRequest(request);
		}

		filterChain.doFilter(request, response);

	}

}
