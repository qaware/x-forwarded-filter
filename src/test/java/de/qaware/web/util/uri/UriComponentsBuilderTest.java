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

package de.qaware.web.util.uri;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static de.qaware.web.util.ForwardedHeader.FORWARDED;
import static de.qaware.web.util.ForwardedHeader.X_FORWARDED_HOST;
import static de.qaware.web.util.ForwardedHeader.X_FORWARDED_PORT;
import static de.qaware.web.util.ForwardedHeader.X_FORWARDED_PROTO;
import static de.qaware.web.util.uri.UriComponentsBuilder.fromUriString;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


/**
 * Unit tests for {@link UriComponentsBuilder}.
 *
 * @author Arjen Poutsma
 * @author Phillip Webb
 * @author Oliver Gierke
 * @author David Eckel
 * @author Sam Brannen
 */
public class UriComponentsBuilderTest {

	@Test
	public void plain() throws URISyntaxException {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		UriComponentsBase result = builder.scheme("http").host("example.com")
				.path("foo").queryParam("bar").fragment("baz")
				.build();
		assertEquals("http", result.getScheme());
		assertEquals("example.com", result.getHost());
		assertEquals("foo", result.getPath());
		assertEquals("bar", result.getQuery());
		assertEquals("baz", result.getFragment());

		URI expected = new URI("http://example.com/foo?bar#baz");
		assertEquals("Invalid result URI", expected, result.toUri());
	}

	@Test
	public void multipleFromSameBuilder() throws URISyntaxException {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
				.scheme("http").host("example.com").pathSegment("foo");
		UriComponentsBase result1 = builder.build();
		builder = builder.pathSegment("foo2").queryParam("bar").fragment("baz");
		UriComponentsBase result2 = builder.build();

		assertEquals("http", result1.getScheme());
		assertEquals("example.com", result1.getHost());
		assertEquals("/foo", result1.getPath());
		URI expected = new URI("http://example.com/foo");
		assertEquals("Invalid result URI", expected, result1.toUri());

		assertEquals("http", result2.getScheme());
		assertEquals("example.com", result2.getHost());
		assertEquals("/foo/foo2", result2.getPath());
		assertEquals("bar", result2.getQuery());
		assertEquals("baz", result2.getFragment());
		expected = new URI("http://example.com/foo/foo2?bar#baz");
		assertEquals("Invalid result URI", expected, result2.toUri());
	}

	@Test
	public void fromPath() throws URISyntaxException {
		UriComponentsBase result = UriComponentsBuilder.fromPath("foo").queryParam("bar").fragment("baz").build();
		assertEquals("foo", result.getPath());
		assertEquals("bar", result.getQuery());
		assertEquals("baz", result.getFragment());

		assertEquals("Invalid result URI String", "foo?bar#baz", result.toUriString());

		URI expected = new URI("foo?bar#baz");
		assertEquals("Invalid result URI", expected, result.toUri());

		result = UriComponentsBuilder.fromPath("/foo").build();
		assertEquals("/foo", result.getPath());

		expected = new URI("/foo");
		assertEquals("Invalid result URI", expected, result.toUri());
	}

	@Test
	public void fromHierarchicalUri() throws URISyntaxException {
		URI uri = new URI("http://example.com/foo?bar#baz");
		UriComponentsBase result = UriComponentsBuilder.fromUri(uri).build();
		assertEquals("http", result.getScheme());
		assertEquals("example.com", result.getHost());
		assertEquals("/foo", result.getPath());
		assertEquals("bar", result.getQuery());
		assertEquals("baz", result.getFragment());

		assertEquals("Invalid result URI", uri, result.toUri());
	}

	@Test
	public void fromOpaqueUri() throws URISyntaxException {
		URI uri = new URI("mailto:foo@bar.com#baz");
		UriComponentsBase result = UriComponentsBuilder.fromUri(uri).build();
		assertEquals("mailto", result.getScheme());
		assertEquals("foo@bar.com", result.getSchemeSpecificPart());
		assertEquals("baz", result.getFragment());

		assertEquals("Invalid result URI", uri, result.toUri());
	}

	@Test // SPR-9317
	public void fromUriEncodedQuery() throws URISyntaxException {
		URI uri = new URI("http://www.example.org/?param=aGVsbG9Xb3JsZA%3D%3D");
		String fromUri = UriComponentsBuilder.fromUri(uri).build().getQueryParamsMap().get("param").get(0);
		String fromUriString = fromUriString(uri.toString())
				.build().getQueryParamsMap().get("param").get(0);

		assertEquals(fromUri, fromUriString);
	}

	@Test
	public void testFromUriString() {
		UriComponentsBase result = fromUriString("http://www.ietf.org/rfc/rfc3986.txt").build();
		assertEquals("http", result.getScheme());
		assertNull(result.getUserInfo());
		assertEquals("www.ietf.org", result.getHost());
		assertEquals(-1, result.getPort());
		assertEquals("/rfc/rfc3986.txt", result.getPath());
		assertEquals(Arrays.asList("rfc", "rfc3986.txt"), result.getPathSegments());
		assertNull(result.getQuery());
		assertNull(result.getFragment());

		String url = "http://arjen:foobar@java.sun.com:80" +
				"/javase/6/docs/api/java/util/BitSet.html?foo=bar#and(java.util.BitSet)";
		result = fromUriString(url).build();
		assertEquals("http", result.getScheme());
		assertEquals("arjen:foobar", result.getUserInfo());
		assertEquals("java.sun.com", result.getHost());
		assertEquals(80, result.getPort());
		assertEquals("/javase/6/docs/api/java/util/BitSet.html", result.getPath());
		assertEquals("foo=bar", result.getQuery());
		MultiValuedMap<String, String> expectedQueryParams = new ArrayListValuedHashMap<>(1);
		expectedQueryParams.put("foo", "bar");
		assertEquals(expectedQueryParams, result.getQueryParams());
		assertEquals("and(java.util.BitSet)", result.getFragment());

		result = fromUriString("mailto:java-net@java.sun.com#baz").build();
		assertEquals("mailto", result.getScheme());
		assertNull(result.getUserInfo());
		assertNull(result.getHost());
		assertEquals(-1, result.getPort());
		assertEquals("java-net@java.sun.com", result.getSchemeSpecificPart());
		assertNull(result.getPath());
		assertNull(result.getQuery());
		assertEquals("baz", result.getFragment());

		result = fromUriString("docs/guide/collections/designfaq.html#28").build();
		assertNull(result.getScheme());
		assertNull(result.getUserInfo());
		assertNull(result.getHost());
		assertEquals(-1, result.getPort());
		assertEquals("docs/guide/collections/designfaq.html", result.getPath());
		assertNull(result.getQuery());
		assertEquals("28", result.getFragment());
	}

	@Test // SPR-9832
	public void fromUriStringQueryParamWithReservedCharInValue() throws URISyntaxException {
		String uri = "http://www.google.com/ig/calculator?q=1USD=?EUR";
		UriComponentsBase result = fromUriString(uri).build();

		assertEquals("q=1USD=?EUR", result.getQuery());
		assertEquals("1USD=?EUR", result.getQueryParamsMap().get("q").get(0));
	}

	@Test // SPR-14828
	public void fromUriStringQueryParamEncodedAndContainingPlus() throws Exception {
		String httpUrl = "http://localhost:8080/test/print?value=%EA%B0%80+%EB%82%98";
		URI uri = UriComponentsBuilder.fromHttpUrl(httpUrl).build(true).toUri();

		assertEquals(httpUrl, uri.toString());
	}

	@Test // SPR-10779
	public void fromHttpUrlStringCaseInsesitiveScheme() {
		assertEquals("http", UriComponentsBuilder.fromHttpUrl("HTTP://www.google.com").build().getScheme());
		assertEquals("https", UriComponentsBuilder.fromHttpUrl("HTTPS://www.google.com").build().getScheme());
	}


	@Test(expected = IllegalArgumentException.class) // SPR-10539
	public void fromHttpUrlStringInvalidIPv6Host() throws URISyntaxException {
		UriComponentsBuilder.fromHttpUrl("http://[1abc:2abc:3abc::5ABC:6abc:8080/resource").build().encode();
	}

	@Test // SPR-10539
	public void fromUriStringIPv6Host() throws URISyntaxException {
		UriComponentsBase result = UriComponentsBuilder
				.fromUriString("http://[1abc:2abc:3abc::5ABC:6abc]:8080/resource").build().encode();
		assertEquals("[1abc:2abc:3abc::5ABC:6abc]", result.getHost());

		UriComponentsBase resultWithScopeId = UriComponentsBuilder
				.fromUriString("http://[1abc:2abc:3abc::5ABC:6abc%eth0]:8080/resource").build().encode();
		assertEquals("[1abc:2abc:3abc::5ABC:6abc%25eth0]", resultWithScopeId.getHost());

		UriComponentsBase resultIPv4compatible = UriComponentsBuilder
				.fromUriString("http://[::192.168.1.1]:8080/resource").build().encode();
		assertEquals("[::192.168.1.1]", resultIPv4compatible.getHost());
	}

	@Test // SPR-11970
	public void fromUriStringNoPathWithReservedCharInQuery() {
		UriComponentsBase result = fromUriString("http://example.com?foo=bar@baz").build();
		assertTrue(StringUtils.isEmpty(result.getUserInfo()));
		assertEquals("example.com", result.getHost());
		assertTrue(result.getQueryParams().containsKey("foo"));
		assertEquals("bar@baz", result.getQueryParamsMap().get("foo").get(0));
	}


	@Test
	public void fromHttpRequest() throws URISyntaxException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.setRequestURI("/path");
		request.setQueryString("a=1");

		UriComponentsBase result = UriComponentsBuilder.fromHttpRequest(request).build();
		assertEquals("http", result.getScheme());
		assertEquals("localhost", result.getHost());
		assertEquals(-1, result.getPort());
		assertEquals("/path", result.getPath());
		assertEquals("a=1", result.getQuery());
	}

	@Test // SPR-12771
	public void fromHttpRequestResetsPortBeforeSettingIt() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(X_FORWARDED_PROTO.headerName(), "https");
		request.addHeader(X_FORWARDED_HOST.headerName(), "84.198.58.199");
		request.addHeader(X_FORWARDED_PORT.headerName(), 443);
		request.setScheme("http");
		request.setServerName("example.com");
		request.setServerPort(80);
		request.setRequestURI("/rest/mobile/users/1");

		UriComponentsBase result = UriComponentsBuilder.fromHttpRequest(request).build();

		assertEquals("https", result.getScheme());
		assertEquals("84.198.58.199", result.getHost());
		assertEquals(-1, result.getPort());
		assertEquals("/rest/mobile/users/1", result.getPath());
	}

	@Test //SPR-14761
	public void fromHttpRequestWithForwardedIPv4Host() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.setRequestURI("/mvc-showcase");
		request.addHeader(FORWARDED.headerName(), "host=192.168.0.1");

		UriComponentsBase result = UriComponentsBuilder.fromHttpRequest(request).build();

		assertEquals("http://192.168.0.1/mvc-showcase", result.toString());
	}

	@Test //SPR-14761
	public void fromHttpRequestWithForwardedIPv6() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.setRequestURI("/mvc-showcase");
		request.addHeader(FORWARDED.headerName(), "host=[1abc:2abc:3abc::5ABC:6abc]");

		UriComponentsBase result = UriComponentsBuilder.fromHttpRequest(request).build();

		assertEquals("http://[1abc:2abc:3abc::5ABC:6abc]/mvc-showcase", result.toString());
	}

	@Test //SPR-14761
	public void fromHttpRequestWithForwardedIPv6Host() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.setRequestURI("/mvc-showcase");
		request.addHeader(X_FORWARDED_HOST.headerName(), "[1abc:2abc:3abc::5ABC:6abc]");

		UriComponentsBase result = UriComponentsBuilder.fromHttpRequest(request).build();

		assertEquals("http://[1abc:2abc:3abc::5ABC:6abc]/mvc-showcase", result.toString());
	}

	@Test //SPR-14761
	public void fromHttpRequestWithForwardedIPv6HostAndPort() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.setRequestURI("/mvc-showcase");
		request.addHeader(X_FORWARDED_HOST.headerName(), "[1abc:2abc:3abc::5ABC:6abc]:8080");

		UriComponentsBase result = UriComponentsBuilder.fromHttpRequest(request).build();

		assertEquals("http://[1abc:2abc:3abc::5ABC:6abc]:8080/mvc-showcase", result.toString());
	}


	@Test
	public void fromHttpRequestWithForwardedHost() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.setRequestURI("/mvc-showcase");
		request.addHeader(X_FORWARDED_HOST.headerName(), "anotherHost");

		UriComponentsBase result = UriComponentsBuilder.fromHttpRequest(request).build();

		assertEquals("http://anotherHost/mvc-showcase", result.toString());
	}

	@Test // SPR-10701
	public void fromHttpRequestWithForwardedHostIncludingPort() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.setRequestURI("/mvc-showcase");
		request.addHeader(X_FORWARDED_HOST.headerName(), "webtest.foo.bar.com:443");


		UriComponentsBase result = UriComponentsBuilder.fromHttpRequest(request).build();

		assertEquals("webtest.foo.bar.com", result.getHost());
		assertEquals(443, result.getPort());
	}

	@Test // SPR-11140
	public void fromHttpRequestWithForwardedHostMultiValuedHeader() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(-1);
		request.addHeader(X_FORWARDED_HOST.headerName(), "a.example.org, b.example.org, c.example.org");


		UriComponentsBase result = UriComponentsBuilder.fromHttpRequest(request).build();

		assertEquals("a.example.org", result.getHost());
		assertEquals(-1, result.getPort());
	}

	@Test // SPR-11855
	public void fromHttpRequestWithForwardedHostAndPort() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8080);
		request.addHeader(X_FORWARDED_HOST.headerName(), "foobarhost");
		request.addHeader(X_FORWARDED_PORT.headerName(), "9090");


		UriComponentsBase result = UriComponentsBuilder.fromHttpRequest(request).build();

		assertEquals("foobarhost", result.getHost());
		assertEquals(9090, result.getPort());
	}

	@Test // SPR-11872
	public void fromHttpRequestWithForwardedHostWithDefaultPort() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(10080);
		request.addHeader(X_FORWARDED_HOST.headerName(), "example.org");


		UriComponentsBase result = UriComponentsBuilder.fromHttpRequest(request).build();

		assertEquals("example.org", result.getHost());
		assertEquals(-1, result.getPort());
	}


	@Test
	public void fromHttpRequestWithForwardedHostWithForwardedScheme() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(10080);
		request.addHeader(X_FORWARDED_HOST.headerName(), "example.org");
		request.addHeader(X_FORWARDED_PROTO.headerName(), "https");


		UriComponentsBase result = UriComponentsBuilder.fromHttpRequest(request).build();

		assertEquals("example.org", result.getHost());
		assertEquals("https", result.getScheme());
		assertEquals(-1, result.getPort());
	}

	@Test // SPR-12771
	public void fromHttpRequestWithForwardedProtoAndDefaultPort() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(80);
		request.setRequestURI("/mvc-showcase");
		request.addHeader(X_FORWARDED_PROTO.headerName(), "https");
		request.addHeader(X_FORWARDED_HOST.headerName(), "84.198.58.199");
		request.addHeader(X_FORWARDED_PORT.headerName(), "443");


		UriComponentsBase result = UriComponentsBuilder.fromHttpRequest(request).build();

		assertEquals("https://84.198.58.199/mvc-showcase", result.toString());
	}

	@Test // SPR-12813
	public void fromHttpRequestWithForwardedPortMultiValueHeader() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(9090);
		request.setRequestURI("/mvc-showcase");
		request.addHeader(X_FORWARDED_HOST.headerName(), "a.example.org");
		request.addHeader(X_FORWARDED_PORT.headerName(), "80,52022");


		UriComponentsBase result = UriComponentsBuilder.fromHttpRequest(request).build();

		assertEquals("http://a.example.org/mvc-showcase", result.toString());
	}

	@Test // SPR-12816
	public void fromHttpRequestWithForwardedProtoMultiValueHeader() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8080);
		request.setRequestURI("/mvc-showcase");
		request.addHeader(X_FORWARDED_HOST.headerName(), "a.example.org");
		request.addHeader(X_FORWARDED_PORT.headerName(), "443");
		request.addHeader(X_FORWARDED_PROTO.headerName(), "https,https");


		UriComponentsBase result = UriComponentsBuilder.fromHttpRequest(request).build();

		assertEquals("https://a.example.org/mvc-showcase", result.toString());
	}

	@Test // SPR-12742
	public void fromHttpRequestWithTrailingSlash() throws Exception {
		UriComponentsBase before = UriComponentsBuilder.fromPath("/foo/").build();
		UriComponentsBase after = UriComponentsBuilder.newInstance().uriComponents(before).build();
		assertEquals("/foo/", after.getPath());
	}

	@Test
	public void path() throws URISyntaxException {
		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/foo/bar");
		UriComponentsBase result = builder.build();

		assertEquals("/foo/bar", result.getPath());
		assertEquals(Arrays.asList("foo", "bar"), result.getPathSegments());
	}

	@Test
	public void pathSegments() throws URISyntaxException {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		UriComponentsBase result = builder.pathSegment("foo").pathSegment("bar").build();

		assertEquals("/foo/bar", result.getPath());
		assertEquals(Arrays.asList("foo", "bar"), result.getPathSegments());
	}

	@Test
	public void pathThenPath() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/foo/bar").path("ba/z");
		UriComponentsBase result = builder.build().encode();

		assertEquals("/foo/barba/z", result.getPath());
		assertEquals(Arrays.asList("foo", "barba", "z"), result.getPathSegments());
	}

	@Test
	public void pathThenPathSegments() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/foo/bar").pathSegment("ba/z");
		UriComponentsBase result = builder.build().encode();

		assertEquals("/foo/bar/ba%2Fz", result.getPath());
		assertEquals(Arrays.asList("foo", "bar", "ba%2Fz"), result.getPathSegments());
	}

	@Test
	public void pathSegmentsThenPathSegments() {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance().pathSegment("foo").pathSegment("bar");
		UriComponentsBase result = builder.build();

		assertEquals("/foo/bar", result.getPath());
		assertEquals(Arrays.asList("foo", "bar"), result.getPathSegments());
	}

	@Test
	public void pathSegmentsThenPath() {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance().pathSegment("foo").path("/");
		UriComponentsBase result = builder.build();

		assertEquals("/foo/", result.getPath());
		assertEquals(Collections.singletonList("foo"), result.getPathSegments());
	}

	@Test
	public void pathSegmentsSomeEmpty() {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance().pathSegment("", "foo", "", "bar");
		UriComponentsBase result = builder.build();

		assertEquals("/foo/bar", result.getPath());
		assertEquals(Arrays.asList("foo", "bar"), result.getPathSegments());
	}

	@Test // SPR-12398
	public void pathWithDuplicateSlashes() throws URISyntaxException {
		UriComponentsBase uriComponents = UriComponentsBuilder.fromPath("/foo/////////bar").build();
		assertEquals("/foo/bar", uriComponents.getPath());
	}

	@Test
	public void replacePath() {
		UriComponentsBuilder builder = fromUriString("http://www.ietf.org/rfc/rfc2396.txt");
		builder.replacePath("/rfc/rfc3986.txt");
		UriComponentsBase result = builder.build();

		assertEquals("http://www.ietf.org/rfc/rfc3986.txt", result.toUriString());

		builder = fromUriString("http://www.ietf.org/rfc/rfc2396.txt");
		builder.replacePath(null);
		result = builder.build();

		assertEquals("http://www.ietf.org", result.toUriString());
	}

	@Test
	public void replaceQuery() {
		UriComponentsBuilder builder = fromUriString("http://example.com/foo?foo=bar&baz=qux");
		builder.replaceQuery("baz=42");
		UriComponentsBase result = builder.build();

		assertEquals("http://example.com/foo?baz=42", result.toUriString());

		builder = fromUriString("http://example.com/foo?foo=bar&baz=qux");
		builder.replaceQuery(null);
		result = builder.build();

		assertEquals("http://example.com/foo", result.toUriString());
	}

	@Test
	public void queryParams() throws URISyntaxException {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		UriComponentsBase result = builder.queryParam("baz", "qux", 42).build();

		assertEquals("baz=qux&baz=42", result.getQuery());
		ArrayListValuedHashMap<String, String> expectedQueryParams = new ArrayListValuedHashMap<>(2);
		expectedQueryParams.put("baz", "qux");
		expectedQueryParams.put("baz", "42");
		assertEquals(expectedQueryParams, result.getQueryParams());
	}

	@Test
	public void emptyQueryParam() throws URISyntaxException {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		UriComponentsBase result = builder.queryParam("baz").build();

		assertEquals("baz", result.getQuery());
		MultiValuedMap<String, String> expectedQueryParams = new ArrayListValuedHashMap<>(2);
		expectedQueryParams.put("baz", null);
		assertEquals(expectedQueryParams, result.getQueryParams());
	}

	@Test
	public void replaceQueryParam() {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance().queryParam("baz", "qux", 42);
		builder.replaceQueryParam("baz", "xuq", 24);
		UriComponentsBase result = builder.build();

		assertEquals("baz=xuq&baz=24", result.getQuery());

		builder = UriComponentsBuilder.newInstance().queryParam("baz", "qux", 42);
		builder.replaceQueryParam("baz");
		result = builder.build();

		assertNull("Query param should have been deleted", result.getQuery());
	}

	@Test
	public void buildAndExpandHierarchical() {
		UriComponentsBase result = UriComponentsBuilder.fromPath("/{foo}").buildAndExpand("fooValue");
		assertEquals("/fooValue", result.toUriString());

		Map<String, String> values = new HashMap<>();
		values.put("foo", "fooValue");
		values.put("bar", "barValue");
		result = UriComponentsBuilder.fromPath("/{foo}/{bar}").buildAndExpand(values);
		assertEquals("/fooValue/barValue", result.toUriString());
	}

	@Test
	public void buildAndExpandOpaque() {
		UriComponentsBase result = fromUriString("mailto:{user}@{domain}")
				.buildAndExpand("foo", "example.com");
		assertEquals("mailto:foo@example.com", result.toUriString());

		Map<String, String> values = new HashMap<>();
		values.put("user", "foo");
		values.put("domain", "example.com");
		fromUriString("mailto:{user}@{domain}").buildAndExpand(values);
		assertEquals("mailto:foo@example.com", result.toUriString());
	}

	@Test
	public void queryParamWithValueWithEquals() throws Exception {
		UriComponentsBase uriComponents = fromUriString("http://example.com/foo?bar=baz").build();
		assertThat(uriComponents.toUriString()).isEqualTo("http://example.com/foo?bar=baz");
		assertThat(uriComponents.getQueryParamsMap().get("bar").get(0)).isEqualTo("baz");
	}

	@Test
	public void queryParamWithoutValueWithEquals() throws Exception {
		UriComponentsBase uriComponents = fromUriString("http://example.com/foo?bar=").build();
		assertThat(uriComponents.toUriString()).isEqualTo("http://example.com/foo?bar=");
		assertThat(uriComponents.getQueryParamsMap().get("bar").get(0)).isEqualTo("");
	}

	@Test
	public void queryParamWithoutValueWithoutEquals() throws Exception {
		UriComponentsBase uriComponents = fromUriString("http://example.com/foo?bar").build();
		assertThat(uriComponents.toUriString()).isEqualTo("http://example.com/foo?bar");

		// TODO [SPR-13537] Change equalTo(null) to equalTo("").
		assertThat(uriComponents.getQueryParamsMap().get("bar").get(0)).isEqualTo(null);
	}

	@Test
	public void relativeUrls() throws Exception {
		String baseUrl = "http://example.com";
		assertThat(fromUriString(baseUrl + "/foo/../bar").build().toString()).isEqualTo((baseUrl + "/foo/../bar"));
		assertThat(fromUriString(baseUrl + "/foo/../bar").build().toUriString()).isEqualTo(baseUrl + "/foo/../bar");
		assertThat(fromUriString(baseUrl + "/foo/../bar").build().toUri().getPath()).isEqualTo("/foo/../bar");
		assertThat(fromUriString("../../").build().toString()).isEqualTo("../../");
		assertThat(fromUriString("../../").build().toUriString()).isEqualTo("../../");
		assertThat(fromUriString("../../").build().toUri().getPath()).isEqualTo("../../");
		assertThat(fromUriString(baseUrl).path("foo/../bar").build().toString()).isEqualTo(baseUrl + "/foo/../bar");
		assertThat(fromUriString(baseUrl).path("foo/../bar").build().toUriString()).isEqualTo(baseUrl + "/foo/../bar");
		assertThat(fromUriString(baseUrl).path("foo/../bar").build().toUri().getPath()).isEqualTo("/foo/../bar");
	}

	@Test
	public void emptySegments() throws Exception {
		String baseUrl = "http://example.com/abc/";
		assertThat(fromUriString(baseUrl).path("/x/y/z").build().toString()).isEqualTo("http://example.com/abc/x/y/z");
		assertThat(fromUriString(baseUrl).pathSegment("x", "y", "z").build().toString()).isEqualTo("http://example.com/abc/x/y/z");
		assertThat(fromUriString(baseUrl).path("/x/").path("/y/z").build().toString()).isEqualTo("http://example.com/abc/x/y/z");
		assertThat(fromUriString(baseUrl).pathSegment("x").path("y").build().toString()).isEqualTo("http://example.com/abc/x/y");
	}

	@Test
	public void parsesEmptyFragment() {
		UriComponentsBase components = fromUriString("/example#").build();
		assertThat(components.getFragment()).isNull();
		assertThat(components.toString()).isEqualTo("/example");
	}

	@Test  // SPR-13257
	public void parsesEmptyUri() {
		UriComponentsBase components = fromUriString("").build();
		assertThat(components.toString()).isEqualTo("");
	}

	@Test
	public void testCopyConstructor() throws URISyntaxException {
		UriComponentsBuilder builder1 = UriComponentsBuilder.newInstance();
		builder1.scheme("http").host("e1.com").path("/p1").pathSegment("ps1").queryParam("q1").fragment("f1");

		UriComponentsBuilder builder2 = new UriComponentsBuilder(builder1);
		builder2.scheme("https").host("e2.com").path("p2").pathSegment("ps2").queryParam("q2").fragment("f2");

		UriComponentsBase result1 = builder1.build();
		assertEquals("http", result1.getScheme());
		assertEquals("e1.com", result1.getHost());
		assertEquals("/p1/ps1", result1.getPath());
		assertEquals("q1", result1.getQuery());
		assertEquals("f1", result1.getFragment());

		UriComponentsBase result2 = builder2.build();
		assertEquals("https", result2.getScheme());
		assertEquals("e2.com", result2.getHost());
		assertEquals("/p1/ps1/p2/ps2", result2.getPath());
		assertEquals("q1&q2", result2.getQuery());
		assertEquals("f2", result2.getFragment());
	}

	@Test // SPR-11856
	public void fromHttpRequestForwardedHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(FORWARDED.headerName(), "proto=https; host=84.198.58.199");
		request.setScheme("http");
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");


		UriComponentsBase result = UriComponentsBuilder.fromHttpRequest(request).build();

		assertEquals("https", result.getScheme());
		assertEquals("84.198.58.199", result.getHost());
		assertEquals("/rest/mobile/users/1", result.getPath());
	}

	@Test
	public void fromHttpRequestForwardedHeaderQuoted() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(FORWARDED.headerName(), "proto=\"https\"; host=\"84.198.58.199\"");
		request.setScheme("http");
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");


		UriComponentsBase result = UriComponentsBuilder.fromHttpRequest(request).build();

		assertEquals("https", result.getScheme());
		assertEquals("84.198.58.199", result.getHost());
		assertEquals("/rest/mobile/users/1", result.getPath());
	}

	@Test
	public void fromHttpRequestMultipleForwardedHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(FORWARDED.headerName(), "host=84.198.58.199;proto=https");
		request.addHeader(FORWARDED.headerName(), "proto=ftp; host=1.2.3.4");
		request.setScheme("http");
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");


		UriComponentsBase result = UriComponentsBuilder.fromHttpRequest(request).build();

		assertEquals("https", result.getScheme());
		assertEquals("84.198.58.199", result.getHost());
		assertEquals("/rest/mobile/users/1", result.getPath());
	}

	@Test
	public void fromHttpRequestMultipleForwardedHeaderComma() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(FORWARDED.headerName(), "host=84.198.58.199 ;proto=https, proto=ftp; host=1.2.3.4");
		request.setScheme("http");
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");


		UriComponentsBase result = UriComponentsBuilder.fromHttpRequest(request).build();

		assertEquals("https", result.getScheme());
		assertEquals("84.198.58.199", result.getHost());
		assertEquals("/rest/mobile/users/1", result.getPath());
	}

	@Test
	public void fromHttpRequestForwardedHeaderWithHostPortAndWithoutServerPort() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(FORWARDED.headerName(), "proto=https; host=84.198.58.199:9090");
		request.setScheme("http");
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");


		UriComponentsBase result = UriComponentsBuilder.fromHttpRequest(request).build();

		assertEquals("https", result.getScheme());
		assertEquals("84.198.58.199", result.getHost());
		assertEquals("/rest/mobile/users/1", result.getPath());
		assertEquals(9090, result.getPort());
		assertEquals("https://84.198.58.199:9090/rest/mobile/users/1", result.toUriString());
	}

	@Test
	public void fromHttpRequestForwardedHeaderWithHostPortAndServerPort() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(FORWARDED.headerName(), "proto=https; host=84.198.58.199:9090");
		request.setScheme("http");
		request.setServerPort(8080);
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");


		UriComponentsBase result = UriComponentsBuilder.fromHttpRequest(request).build();

		assertEquals("https", result.getScheme());
		assertEquals("84.198.58.199", result.getHost());
		assertEquals("/rest/mobile/users/1", result.getPath());
		assertEquals(9090, result.getPort());
		assertEquals("https://84.198.58.199:9090/rest/mobile/users/1", result.toUriString());
	}

	@Test
	public void fromHttpRequestForwardedHeaderWithoutHostPortAndWithServerPort() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(FORWARDED.headerName(), "proto=https; host=84.198.58.199");
		request.setScheme("http");
		request.setServerPort(8080);
		request.setServerName("example.com");
		request.setRequestURI("/rest/mobile/users/1");


		UriComponentsBase result = UriComponentsBuilder.fromHttpRequest(request).build();

		assertEquals("https", result.getScheme());
		assertEquals("84.198.58.199", result.getHost());
		assertEquals("/rest/mobile/users/1", result.getPath());
		assertEquals(-1, result.getPort());
		assertEquals("https://84.198.58.199/rest/mobile/users/1", result.toUriString());
	}
}
