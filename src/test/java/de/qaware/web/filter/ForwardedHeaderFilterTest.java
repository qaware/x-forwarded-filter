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

package de.qaware.web.filter;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import static de.qaware.web.filter.ForwardedHeaderFilter.ENABLE_RELATIVE_REDIRECTS_INIT_PARAM;
import static de.qaware.web.filter.ForwardedHeaderFilter.HEADER_PROCESSING_STRATEGY;
import static de.qaware.web.filter.ForwardedHeaderFilter.X_FORWARDED_PREFIX_STRATEGY;
import static de.qaware.web.util.ForwardedHeader.FORWARDED;
import static de.qaware.web.util.ForwardedHeader.X_FORWARDED_HOST;
import static de.qaware.web.util.ForwardedHeader.X_FORWARDED_PORT;
import static de.qaware.web.util.ForwardedHeader.X_FORWARDED_PREFIX;
import static de.qaware.web.util.ForwardedHeader.X_FORWARDED_PROTO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ForwardedHeaderFilter}.
 *
 * @author Rossen Stoyanchev
 * @author Eddú Meléndez
 * @author Rob Winch
 */
public class ForwardedHeaderFilterTest {

	public static final String UNIT_TEST_FORWARED_FILTER = "unitTestForwardedFilter";

	private ForwardedHeaderFilter filter;

	private MockHttpServletRequest request;

	private MockFilterChain filterChain;

	@Before
	@SuppressWarnings("serial")
	public void setup() throws Exception {
		this.filter = new ForwardedHeaderFilter();
		initFilter(UNIT_TEST_FORWARED_FILTER, null);
		this.request = new MockHttpServletRequest();
		this.request.setScheme("http");
		this.request.setServerName("localhost");
		this.request.setServerPort(80);
		this.filterChain = new MockFilterChain(new HttpServlet() {
		});
	}


	private ForwardedHeaderFilter initFilter(String filterName, Map<String, String> params) throws ServletException {
		MockFilterConfig config = new MockFilterConfig(filterName);
		if (params != null) {
			params.forEach(config::addInitParameter);
		}
		filter.init(config);
		return filter;
	}

	@Test
	public void contextPathEmpty() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "");
		assertEquals("", filterAndGetContextPath());
	}

	@Test
	public void contextPathWithTrailingSlash() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/foo/bar/");
		assertEquals("/foo/bar", filterAndGetContextPath());
	}

	@Test
	public void contextPathWithTrailingSlashes() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/foo/bar/baz///");
		assertEquals("/foo/bar/baz", filterAndGetContextPath());
	}

	@Test
	public void contextPathWithForwardedPrefix() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/prefix");
		this.request.setContextPath("/mvc-showcase");

		String actual = filterAndGetContextPath();
		assertEquals("/prefix", actual);
	}

	@Test
	public void contextPathWithForwardedPrefixTrailingSlash() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/prefix/");
		this.request.setContextPath("/mvc-showcase");

		String actual = filterAndGetContextPath();
		assertEquals("/prefix", actual);
	}

	@Test
	public void contextPathPreserveEncoding() throws Exception {
		this.request.setContextPath("/app%20");
		this.request.setRequestURI("/app%20/path/");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertEquals("/app%20", actual.getContextPath());
		assertEquals("/app%20/path/", actual.getRequestURI());
		assertEquals("http://localhost/app%20/path/", actual.getRequestURL().toString());
	}

	@Test
	public void requestUri() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/");
		this.request.setContextPath("/app");
		this.request.setRequestURI("/app/path");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertEquals("", actual.getContextPath());
		assertEquals("/path", actual.getRequestURI());
	}

	@Test
	public void requestUriWithTrailingSlash() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/");
		this.request.setContextPath("/app");
		this.request.setRequestURI("/app/path/");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertEquals("", actual.getContextPath());
		assertEquals("/path/", actual.getRequestURI());
	}

	@Test
	public void requestUriPreserveEncoding() throws Exception {
		this.request.setContextPath("/app");
		this.request.setRequestURI("/app/path%20with%20spaces/");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertEquals("/app", actual.getContextPath());
		assertEquals("/app/path%20with%20spaces/", actual.getRequestURI());
		assertEquals("http://localhost/app/path%20with%20spaces/", actual.getRequestURL().toString());
	}

	@Test
	public void requestUriEqualsContextPath() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/");
		this.request.setContextPath("/app");
		this.request.setRequestURI("/app");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertEquals("", actual.getContextPath());
		assertEquals("/", actual.getRequestURI());
	}

	@Test
	public void requestUriRootUrl() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/");
		this.request.setContextPath("/app");
		this.request.setRequestURI("/app/");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertEquals("", actual.getContextPath());
		assertEquals("/", actual.getRequestURI());
	}

	@Test
	public void requestUriPreserveSemicolonContent() throws Exception {
		this.request.setContextPath("");
		this.request.setRequestURI("/path;a=b/with/semicolon");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertEquals("", actual.getContextPath());
		assertEquals("/path;a=b/with/semicolon", actual.getRequestURI());
		assertEquals("http://localhost/path;a=b/with/semicolon", actual.getRequestURL().toString());
	}

	@Test
	public void caseInsensitiveXForwardedPrefix() throws Exception {
		this.request = new MockHttpServletRequest() {
			// Make it case-sensitive (SPR-14372)
			@Override
			public String getHeader(String header) {
				Enumeration<String> names = getHeaderNames();
				while (names.hasMoreElements()) {
					String name = names.nextElement();
					if (name.equals(header)) {
						return super.getHeader(header);
					}
				}
				return null;
			}
		};
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/prefix");
		this.request.setRequestURI("/path");
		HttpServletRequest actual = filterAndGetWrappedRequest();

		assertEquals("/prefix/path", actual.getRequestURI());
	}

	@Test
	public void shouldFilter() throws Exception {
		testShouldFilter(FORWARDED.headerName());
		testShouldFilter(X_FORWARDED_HOST.headerName());
		testShouldFilter(X_FORWARDED_PORT.headerName());
		testShouldFilter(X_FORWARDED_PROTO.headerName());
	}

	@Test
	public void shouldNotFilter() throws Exception {
		assertTrue(this.filter.shouldNotFilter(new MockHttpServletRequest()));
	}

	@Test
	public void xForwardedRequest() throws Exception {
		this.request.setRequestURI("/mvc-showcase");
		this.request.addHeader(X_FORWARDED_PROTO.headerName(), "https");
		this.request.addHeader(X_FORWARDED_HOST.headerName(), "84.198.58.199");
		this.request.addHeader(X_FORWARDED_PORT.headerName(), "443");
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/prefix");
		this.request.addHeader("notToBeRemoved", "notToBeRemoved");

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		HttpServletRequest actual = (HttpServletRequest) this.filterChain.getRequest();

		//===========================================================
		//Assert HeaderProcessingStrategy is default: EVAL_AND_REMOVE
		//===========================================================
		assertHeadersAREProcessed(actual);
		assertHeadersAREremovedFromRequest(actual);
	}

	@Test
	public void xForwardedRequestCaseInsensitive() throws Exception {
		this.request.setRequestURI("/mvc-showcase");
		this.request.addHeader(X_FORWARDED_PROTO.headerName().toUpperCase(), "https");
		this.request.addHeader(X_FORWARDED_HOST.headerName().toUpperCase(), "84.198.58.199");
		this.request.addHeader(X_FORWARDED_PORT.headerName().toUpperCase(), "443");
		this.request.addHeader(X_FORWARDED_PREFIX.headerName().toUpperCase(), "/prefix");
		this.request.addHeader("notToBeRemoved", "notToBeRemoved");

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		HttpServletRequest actual = (HttpServletRequest) this.filterChain.getRequest();

		//===========================================================
		//Assert HeaderProcessingStrategy is default: EVAL_AND_REMOVE
		//===========================================================
		assertHeadersAREProcessed(actual);
		assertHeadersAREremovedFromRequest(actual);
	}

	@Test
	public void xForwardedRequestWithMultipleHeaders() throws Exception {
		this.request.setRequestURI("/mvc-showcase");
		this.request.addHeader(X_FORWARDED_PROTO.headerName(), "https");
		this.request.addHeader(X_FORWARDED_HOST.headerName(), "84.198.58.199");
		this.request.addHeader(X_FORWARDED_PORT.headerName(), "443");
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/prefix");

		this.request.addHeader(X_FORWARDED_PROTO.headerName(), "secondProto");
		this.request.addHeader(X_FORWARDED_HOST.headerName(), "secondHost");
		this.request.addHeader(X_FORWARDED_PORT.headerName(), "secondPort");
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/secondPrefix");
		this.request.addHeader("notToBeRemoved", "notToBeRemoved");

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		HttpServletRequest actual = (HttpServletRequest) this.filterChain.getRequest();

		//===========================================================
		//Assert HeaderProcessingStrategy is default: EVAL_AND_REMOVE
		//===========================================================
		assertHeadersAREProcessed(actual);
		assertHeadersAREremovedFromRequest(actual);
	}


	@Test
	public void xForwardedRequestWithCommaSpaceSeparatedValues() throws Exception {
		this.request.setRequestURI("/mvc-showcase");
		this.request.addHeader(X_FORWARDED_PROTO.headerName(), "https, secondProto");
		this.request.addHeader(X_FORWARDED_HOST.headerName(), "84.198.58.199, 1.2.3.4");
		this.request.addHeader(X_FORWARDED_PORT.headerName(), "443, 123");
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/prefix, /secondPrefix");

		this.request.addHeader(X_FORWARDED_PROTO.headerName(), "thirdroto");
		this.request.addHeader(X_FORWARDED_HOST.headerName(), "thirdHost");
		this.request.addHeader(X_FORWARDED_PORT.headerName(), "thirdPort");
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/thirdPrefix");
		this.request.addHeader("notToBeRemoved", "notToBeRemoved");

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		HttpServletRequest actual = (HttpServletRequest) this.filterChain.getRequest();

		//===========================================================
		//Assert HeaderProcessingStrategy is default: EVAL_AND_REMOVE
		//===========================================================
		assertHeadersAREProcessed(actual);
		assertHeadersAREremovedFromRequest(actual);
	}

	@Test
	public void xForwardedRequestWithMultipleHeadersANDCommaSpaceSeparatedValues() throws Exception {
		this.request.setRequestURI("/mvc-showcase");
		this.request.addHeader(X_FORWARDED_PROTO.headerName(), "https, secondProto");
		this.request.addHeader(X_FORWARDED_HOST.headerName(), "84.198.58.199, 1.2.3.4");
		this.request.addHeader(X_FORWARDED_PORT.headerName(), "443, 123");
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/prefix, /secondPrefix");
		this.request.addHeader("notToBeRemoved", "notToBeRemoved");

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		HttpServletRequest actual = (HttpServletRequest) this.filterChain.getRequest();

		//===========================================================
		//Assert HeaderProcessingStrategy is default: EVAL_AND_REMOVE
		//===========================================================
		assertHeadersAREProcessed(actual);
		assertHeadersAREremovedFromRequest(actual);
	}


	@Test
	public void xForwardedRequestInRemoveOnlyMode() throws Exception {
		this.request.setRequestURI("/mvc-showcase");
		this.request.addHeader(X_FORWARDED_PROTO.headerName(), "https");
		this.request.addHeader(X_FORWARDED_HOST.headerName(), "84.198.58.199");
		this.request.addHeader(X_FORWARDED_PORT.headerName(), "443");
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/prefix");
		this.request.addHeader("notToBeRemoved", "notToBeRemoved");

		//Set HEADER_PROCESSING_STRATEGY
		this.filter = initFilter(UNIT_TEST_FORWARED_FILTER, Collections.singletonMap(HEADER_PROCESSING_STRATEGY, HeaderProcessingStrategy.DONT_EVAL_AND_REMOVE.name()));

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		HttpServletRequest actual = (HttpServletRequest) this.filterChain.getRequest();

		//===========================================================
		//Assert HeaderProcessingStrategy is:     DONT_EVAL_AND_REMOVE
		//===========================================================
		assertForwardedHeadersAreNOTProcessed(actual);
		assertHeadersAREremovedFromRequest(actual);
	}


	@Test
	public void xForwardedRequestInEvalAndKeepMode() throws Exception {
		this.request.setRequestURI("/mvc-showcase");
		this.request.addHeader(X_FORWARDED_PROTO.headerName(), "https");
		this.request.addHeader(X_FORWARDED_HOST.headerName(), "84.198.58.199");
		this.request.addHeader(X_FORWARDED_PORT.headerName(), "443");
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/prefix");
		this.request.addHeader("notToBeRemoved", "notToBeRemoved");

		//Set HEADER_PROCESSING_STRATEGY
		this.filter = initFilter(UNIT_TEST_FORWARED_FILTER, Collections.singletonMap(HEADER_PROCESSING_STRATEGY, HeaderProcessingStrategy.EVAL_AND_KEEP.name()));

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		HttpServletRequest actual = (HttpServletRequest) this.filterChain.getRequest();

		//===========================================================
		//Assert HeaderProcessingStrategy is:            EVAL_AND_KEEP
		//===========================================================
		assertHeadersAREProcessed(actual);
		assertXHeadersNOTremovedFromRequest(actual);
	}


	@Test
	public void rfc7239ForwardedRequest() throws Exception {
		this.request.setRequestURI("/mvc-showcase");
		this.request.addHeader(FORWARDED.headerName(), "Forwarded: for=192.0.2.60; proto=https; host=84.198.58.199:443");
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/prefix");
		this.request.addHeader("notToBeRemoved", "notToBeRemoved");

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		HttpServletRequest actual = (HttpServletRequest) this.filterChain.getRequest();

		//===========================================================
		//Assert HeaderProcessingStrategy is default: EVAL_AND_REMOVE
		//===========================================================
		assertHeadersAREProcessed(actual);
		assertHeadersAREremovedFromRequest(actual);
	}

	@Test
	public void rfc7239ForwardedRequestInRemoveOnlyMode() throws Exception {
		this.request.setRequestURI("/mvc-showcase");
		this.request.addHeader(FORWARDED.headerName(), "Forwarded: for=192.0.2.60; proto=https; host=84.198.58.199:443");
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/prefix");
		this.request.addHeader("notToBeRemoved", "notToBeRemoved");

		//Set HEADER_PROCESSING_STRATEGY
		this.filter = initFilter(UNIT_TEST_FORWARED_FILTER, Collections.singletonMap(HEADER_PROCESSING_STRATEGY, HeaderProcessingStrategy.DONT_EVAL_AND_REMOVE.name()));

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		HttpServletRequest actual = (HttpServletRequest) this.filterChain.getRequest();

		//===========================================================
		//Assert HeaderProcessingStrategy is:     DONT_EVAL_AND_REMOVE
		//===========================================================
		assertForwardedHeadersAreNOTProcessed(actual);
		assertHeadersAREremovedFromRequest(actual);
	}

	@Test
	public void rfc7239ForwardedRequestInEvalAndKeepMode() throws Exception {
		this.request.setRequestURI("/mvc-showcase");
		this.request.addHeader(FORWARDED.headerName(), "Forwarded: for=192.0.2.60; proto=https; host=84.198.58.199:443");
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/prefix");
		this.request.addHeader("notToBeRemoved", "notToBeRemoved");

		//Set HEADER_PROCESSING_STRATEGY
		this.filter = initFilter(UNIT_TEST_FORWARED_FILTER, Collections.singletonMap(HEADER_PROCESSING_STRATEGY, HeaderProcessingStrategy.EVAL_AND_KEEP.name()));

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		HttpServletRequest actual = (HttpServletRequest) this.filterChain.getRequest();

		//===========================================================
		//Assert HeaderProcessingStrategy is:            EVAL_AND_KEEP
		//===========================================================
		assertHeadersAREProcessed(actual);
		//Headers must NOT be removed in this mode
		assertNotNull(actual.getHeader(FORWARDED.headerName()));
		assertNotNull(actual.getHeader(X_FORWARDED_PREFIX.headerName()));
		assertEquals("notToBeRemoved", actual.getHeader("notToBeRemoved"));
	}

	@Test
	public void rfc7239ForwardedRequestCaseInsensitive() throws Exception {
		this.request.setRequestURI("/mvc-showcase");
		this.request.addHeader(FORWARDED.headerName().toUpperCase(), "FORWARDED: FOR=192.0.2.60; PROTO=https; HOST=84.198.58.199:443");
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/prefix");
		this.request.addHeader("notToBeRemoved", "notToBeRemoved");

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		HttpServletRequest actual = (HttpServletRequest) this.filterChain.getRequest();

		//===========================================================
		//Assert HeaderProcessingStrategy is default: EVAL_AND_REMOVE
		//===========================================================
		assertHeadersAREProcessed(actual);
		assertHeadersAREremovedFromRequest(actual);
	}

	@Test
	public void rfc7239ForwardedRequestWithCommaSpaceDelimitedValues() throws Exception {
		this.request.setRequestURI("/mvc-showcase");
		this.request.addHeader(FORWARDED.headerName(), "Forwarded: for=192.0.2.60; proto=https; host=84.198.58.199:443, for=2.2.2.2; proto=secondProto; host=22.22.22.22:22");
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/prefix");
		this.request.addHeader("notToBeRemoved", "notToBeRemoved");

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		HttpServletRequest actual = (HttpServletRequest) this.filterChain.getRequest();

		//===========================================================
		//Assert HeaderProcessingStrategy is default: EVAL_AND_REMOVE
		//===========================================================
		assertHeadersAREProcessed(actual);
		assertHeadersAREremovedFromRequest(actual);
	}

	@Test
	public void rfc7239ForwardedRequestWithMultipleHeadersANDCommaSpaceSeparatedValues() throws Exception {
		this.request.setRequestURI("/mvc-showcase");
		this.request.addHeader(FORWARDED.headerName(), "Forwarded: for=192.0.2.60; proto=https; host=84.198.58.199:443, for=2.2.2.2; proto=secondProto; host=22.22.22.22:22");
		this.request.addHeader(FORWARDED.headerName(), "Forwarded: for=3.3.3.3; proto=thirdProto; host=33.33.33:33, for=44.44.44.44; proto=fourthProto; host=77.77.77:77");
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/prefix");
		this.request.addHeader("notToBeRemoved", "notToBeRemoved");

		this.filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);
		HttpServletRequest actual = (HttpServletRequest) this.filterChain.getRequest();

		//===========================================================
		//Assert HeaderProcessingStrategy is default: EVAL_AND_REMOVE
		//===========================================================
		assertHeadersAREProcessed(actual);
		assertHeadersAREremovedFromRequest(actual);
	}


	@Test
	public void requestUriWithForwardedPrefix() throws Exception {
		//Assume default processing strategy is {@link XForwardedPrefixStrategy#REPLACE}
		initFilter(UNIT_TEST_FORWARED_FILTER, Collections.singletonMap(X_FORWARDED_PREFIX_STRATEGY, null));

		this.request.setContextPath("/shouldBeReplaced");
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/prefix");
		this.request.setRequestURI("/mvc-showcase");

		HttpServletRequest actual = filterAndGetWrappedRequest();
		assertEquals("/prefix", actual.getContextPath());
		assertEquals("http://localhost/prefix/mvc-showcase", actual.getRequestURL().toString());
	}

	@Test
	public void requestUriWithForwardedPrefixPrependStrategy() throws Exception {
		//Assume default processing strategy is {@link XForwardedPrefixStrategy#REPLACE}
		this.request.setContextPath("/shouldBePrependedByPrefix");
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/prefix");
		this.request.setRequestURI("/mvc-showcase");
		initFilter(UNIT_TEST_FORWARED_FILTER, Collections.singletonMap(X_FORWARDED_PREFIX_STRATEGY, XForwardedPrefixStrategy.PREPEND.name()));

		HttpServletRequest actual = filterAndGetWrappedRequest();
		assertEquals("/prefix/shouldBePrependedByPrefix", actual.getContextPath());
		assertEquals("http://localhost/prefix/shouldBePrependedByPrefix/mvc-showcase", actual.getRequestURL().toString());
	}

	@Test
	public void requestUriWithForwardedPrefixTrailingSlash() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/prefix/");
		this.request.setRequestURI("/mvc-showcase");

		HttpServletRequest actual = filterAndGetWrappedRequest();
		assertEquals("http://localhost/prefix/mvc-showcase", actual.getRequestURL().toString());
	}

	@Test
	public void requestURLNewStringBuffer() throws Exception {
		this.request.addHeader(X_FORWARDED_PREFIX.headerName(), "/prefix/");
		this.request.setRequestURI("/mvc-showcase");

		HttpServletRequest actual = filterAndGetWrappedRequest();
		actual.getRequestURL().append("?key=value");
		assertEquals("http://localhost/prefix/mvc-showcase", actual.getRequestURL().toString());
	}

	@Test
	public void sendRedirectWithAbsolutePath() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO.headerName(), "https");
		this.request.addHeader(X_FORWARDED_HOST.headerName(), "example.com");
		this.request.addHeader(X_FORWARDED_PORT.headerName(), "443");

		String redirectedUrl = sendRedirect("/foo/bar");
		assertEquals("https://example.com/foo/bar", redirectedUrl);
	}

	@Test
	public void sendRedirectWithContextPath() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO.headerName(), "https");
		this.request.addHeader(X_FORWARDED_HOST.headerName(), "example.com");
		this.request.addHeader(X_FORWARDED_PORT.headerName(), "443");
		this.request.setContextPath("/context");

		String redirectedUrl = sendRedirect("/context/foo/bar");
		assertEquals("https://example.com/context/foo/bar", redirectedUrl);
	}

	@Test
	public void sendRedirectWithRelativePath() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO.headerName(), "https");
		this.request.addHeader(X_FORWARDED_HOST.headerName(), "example.com");
		this.request.addHeader(X_FORWARDED_PORT.headerName(), "443");
		this.request.setRequestURI("/parent/");

		String redirectedUrl = sendRedirect("foo/bar");
		assertEquals("https://example.com/parent/foo/bar", redirectedUrl);
	}

	@Test
	public void sendRedirectWithFileInPathAndRelativeRedirect() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO.headerName(), "https");
		this.request.addHeader(X_FORWARDED_HOST.headerName(), "example.com");
		this.request.addHeader(X_FORWARDED_PORT.headerName(), "443");
		this.request.setRequestURI("/context/a");

		String redirectedUrl = sendRedirect("foo/bar");
		assertEquals("https://example.com/context/foo/bar", redirectedUrl);
	}

	@Test
	public void sendRedirectWithRelativePathIgnoresFile() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO.headerName(), "https");
		this.request.addHeader(X_FORWARDED_HOST.headerName(), "example.com");
		this.request.addHeader(X_FORWARDED_PORT.headerName(), "443");
		this.request.setRequestURI("/parent");

		String redirectedUrl = sendRedirect("foo/bar");
		assertEquals("https://example.com/foo/bar", redirectedUrl);
	}

	@Test
	public void sendRedirectWithLocationDotDotPath() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO.headerName(), "https");
		this.request.addHeader(X_FORWARDED_HOST.headerName(), "example.com");
		this.request.addHeader(X_FORWARDED_PORT.headerName(), "443");

		String redirectedUrl = sendRedirect("parent/../foo/bar");
		assertEquals("https://example.com/foo/bar", redirectedUrl);
	}

	@Test
	public void sendRedirectWithLocationHasScheme() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO.headerName(), "https");
		this.request.addHeader(X_FORWARDED_HOST.headerName(), "example.com");
		this.request.addHeader(X_FORWARDED_PORT.headerName(), "443");

		String location = "http://other.info/foo/bar";
		String redirectedUrl = sendRedirect(location);
		assertEquals(location, redirectedUrl);
	}

	@Test
	public void sendRedirectWithLocationSlashSlash() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO.headerName(), "https");
		this.request.addHeader(X_FORWARDED_HOST.headerName(), "example.com");
		this.request.addHeader(X_FORWARDED_PORT.headerName(), "443");

		String location = "//other.info/foo/bar";
		String redirectedUrl = sendRedirect(location);
		assertEquals("https:" + location, redirectedUrl);
	}

	@Test
	public void sendRedirectWithLocationSlashSlashParentDotDot() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO.headerName(), "https");
		this.request.addHeader(X_FORWARDED_HOST.headerName(), "example.com");
		this.request.addHeader(X_FORWARDED_PORT.headerName(), "443");

		String location = "//other.info/parent/../foo/bar";
		String redirectedUrl = sendRedirect(location);
		assertEquals("https:" + location, redirectedUrl);
	}

	@Test
	public void sendRedirectWithNoXForwardedAndAbsolutePath() throws Exception {
		String redirectedUrl = sendRedirect("/foo/bar");
		assertEquals("/foo/bar", redirectedUrl);
	}

	@Test
	public void sendRedirectWithNoXForwardedAndDotDotPath() throws Exception {
		String redirectedUrl = sendRedirect("../foo/bar");
		assertEquals("../foo/bar", redirectedUrl);
	}

	@Test
	public void sendRedirectWhenRequestOnlyAndXForwardedThenUsesRelativeRedirects() throws Exception {
		this.request.addHeader(X_FORWARDED_PROTO.headerName(), "https");
		this.request.addHeader(X_FORWARDED_HOST.headerName(), "example.com");
		this.request.addHeader(X_FORWARDED_PORT.headerName(), "443");

		initFilter(UNIT_TEST_FORWARED_FILTER, Collections.singletonMap(ENABLE_RELATIVE_REDIRECTS_INIT_PARAM, "true"));
		filter.doFilter(this.request, new MockHttpServletResponse(), this.filterChain);

		String location = sendRedirect("/a");

		assertEquals("/a", location);
	}

	@Test
	public void sendRedirectWhenRequestOnlyAndNoXForwardedThenUsesRelativeRedirects() throws Exception {
		initFilter(UNIT_TEST_FORWARED_FILTER, Collections.singletonMap(ENABLE_RELATIVE_REDIRECTS_INIT_PARAM, "true"));

		String location = sendRedirect("/a");

		assertEquals("/a", location);
	}

	private void assertHeadersAREProcessed(HttpServletRequest actual) {
		assertEquals("https://84.198.58.199/prefix/mvc-showcase", actual.getRequestURL().toString());
		assertEquals("https", actual.getScheme());
		assertEquals("84.198.58.199", actual.getServerName());
		assertEquals(443, actual.getServerPort());
		assertEquals("/prefix", actual.getContextPath());
		assertTrue(actual.isSecure());
	}

	private void assertForwardedHeadersAreNOTProcessed(HttpServletRequest actual) {
		assertEquals("http://localhost/mvc-showcase", actual.getRequestURL().toString());
		assertEquals("http", actual.getScheme());
		assertEquals("localhost", actual.getServerName());
		assertEquals(80, actual.getServerPort());
		assertEquals("", actual.getContextPath());
		assertFalse(actual.isSecure());
	}

	private void assertHeadersAREremovedFromRequest(HttpServletRequest actual) {
		assertNull(actual.getHeader(FORWARDED.headerName()));
		assertNull(actual.getHeader(X_FORWARDED_PROTO.headerName()));
		assertNull(actual.getHeader(X_FORWARDED_HOST.headerName()));
		assertNull(actual.getHeader(X_FORWARDED_PORT.headerName()));
		assertNull(actual.getHeader(X_FORWARDED_PREFIX.headerName()));
		assertEquals("notToBeRemoved", actual.getHeader("notToBeRemoved"));

	}

	private void assertXHeadersNOTremovedFromRequest(HttpServletRequest actual) {
		assertNotNull(actual.getHeader(X_FORWARDED_PROTO.headerName()));
		assertNotNull(actual.getHeader(X_FORWARDED_HOST.headerName()));
		assertNotNull(actual.getHeader(X_FORWARDED_PORT.headerName()));
		assertNotNull(actual.getHeader(X_FORWARDED_PREFIX.headerName()));
		assertEquals("notToBeRemoved", actual.getHeader("notToBeRemoved"));
	}


	private String sendRedirect(final String location) throws ServletException, IOException {
		Filter triggerRedirect = new OncePerRequestFilter() {
			@Override
			protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
					throws ServletException, IOException {
				response.sendRedirect(location);
			}
		};
		triggerRedirect.init(new MockFilterConfig("triggerRedirectFilter"));

		MockHttpServletResponse response = doWithFiltersAndGetResponse(this.filter, triggerRedirect);
		return response.getRedirectedUrl();
	}

	@SuppressWarnings("serial")
	private MockHttpServletResponse doWithFiltersAndGetResponse(Filter... filters) throws ServletException, IOException {
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = new MockFilterChain(new HttpServlet() {
		}, filters);
		filterChain.doFilter(request, response);
		return response;
	}

	private String filterAndGetContextPath() throws ServletException, IOException {
		return filterAndGetWrappedRequest().getContextPath();
	}

	private HttpServletRequest filterAndGetWrappedRequest() throws ServletException, IOException {
		MockHttpServletResponse response = new MockHttpServletResponse();
		this.filter.doFilterInternal(this.request, response, this.filterChain);
		return (HttpServletRequest) this.filterChain.getRequest();
	}

	private void testShouldFilter(String headerName) throws ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(headerName, "1");
		assertFalse(this.filter.shouldNotFilter(request));
	}

}
