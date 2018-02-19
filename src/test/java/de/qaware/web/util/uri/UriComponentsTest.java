/*
 * Copyright 2002-2015 the original author or authors.
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


import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;

import static de.qaware.web.util.uri.UriComponentsBuilder.fromUriString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author Arjen Poutsma
 * @author Phillip Webb
 */
public class UriComponentsTest {

	@Test
	public void encode() {
		UriComponentsBase uriComponents = UriComponentsBuilder.fromPath("/hotel list").build();
		UriComponentsBase encoded = uriComponents.encode();
		assertEquals("/hotel%20list", encoded.getPath());
	}

	@Test
	public void toUriEncoded() throws URISyntaxException {
		UriComponentsBase uriComponents = UriComponentsBuilder.fromUriString(
				"http://example.com/hotel list/Z\u00fcrich").build();
		assertEquals(new URI("http://example.com/hotel%20list/Z%C3%BCrich"), uriComponents.encode().toUri());
	}

	@Test
	public void toUriNotEncoded() throws URISyntaxException {
		UriComponentsBase uriComponents = UriComponentsBuilder.fromUriString(
				"http://example.com/hotel list/Z\u00fcrich").build();
		assertEquals(new URI("http://example.com/hotel%20list/Z\u00fcrich"), uriComponents.toUri());
	}

	@Test
	public void toUriAlreadyEncoded() throws URISyntaxException {
		UriComponentsBase uriComponents = UriComponentsBuilder.fromUriString(
				"http://example.com/hotel%20list/Z%C3%BCrich").build(true);
		UriComponentsBase encoded = uriComponents.encode();
		assertEquals(new URI("http://example.com/hotel%20list/Z%C3%BCrich"), encoded.toUri());
	}

	@Test
	public void toUriWithIpv6HostAlreadyEncoded() throws URISyntaxException {
		UriComponentsBase uriComponents = UriComponentsBuilder.fromUriString(
				"http://[1abc:2abc:3abc::5ABC:6abc]:8080/hotel%20list/Z%C3%BCrich").build(true);
		UriComponentsBase encoded = uriComponents.encode();
		assertEquals(new URI("http://[1abc:2abc:3abc::5ABC:6abc]:8080/hotel%20list/Z%C3%BCrich"), encoded.toUri());
	}

	@Test
	public void expand() {
		UriComponentsBase uriComponents = UriComponentsBuilder.fromUriString(
				"http://example.com").path("/{foo} {bar}").build();
		uriComponents = uriComponents.expand("1 2", "3 4");
		assertEquals("/1 2 3 4", uriComponents.getPath());
		assertEquals("http://example.com/1 2 3 4", uriComponents.toUriString());
	}

	// SPR-13311

	@Test
	public void expandWithRegexVar() {
		String template = "/myurl/{name:[a-z]{1,5}}/show";
		UriComponentsBase uriComponents = UriComponentsBuilder.fromUriString(template).build();
		uriComponents = uriComponents.expand(Collections.singletonMap("name", "test"));
		assertEquals("/myurl/test/show", uriComponents.getPath());
	}

	// SPR-12123

	@Test
	public void port() {
		UriComponentsBase uri1 = fromUriString("http://example.com:8080/bar").build();
		UriComponentsBase uri2 = fromUriString("http://example.com/bar").port(8080).build();
		UriComponentsBase uri3 = fromUriString("http://example.com/bar").port("{port}").build().expand(8080);
		UriComponentsBase uri4 = fromUriString("http://example.com/bar").port("808{digit}").build().expand(0);
		assertEquals(8080, uri1.getPort());
		assertEquals("http://example.com:8080/bar", uri1.toUriString());
		assertEquals(8080, uri2.getPort());
		assertEquals("http://example.com:8080/bar", uri2.toUriString());
		assertEquals(8080, uri3.getPort());
		assertEquals("http://example.com:8080/bar", uri3.toUriString());
		assertEquals(8080, uri4.getPort());
		assertEquals("http://example.com:8080/bar", uri4.toUriString());
	}

	@Test(expected = IllegalStateException.class)
	public void expandEncoded() {
		UriComponentsBuilder.fromPath("/{foo}").build().encode().expand("bar");
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidCharacters() {
		UriComponentsBuilder.fromPath("/{foo}").build(true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidEncodedSequence() {
		UriComponentsBuilder.fromPath("/fo%2o").build(true);
	}

	@Test
	public void normalize() {
		UriComponentsBase uriComponents = UriComponentsBuilder.fromUriString("http://example.com/foo/../bar").build();
		assertEquals("http://example.com/bar", uriComponents.normalize().toString());
	}

	@Test
	public void serializable() throws Exception {
		UriComponentsBase uriComponents = UriComponentsBuilder.fromUriString(
				"http://example.com").path("/{foo}").query("bar={baz}").build();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(uriComponents);
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
		UriComponentsBase readObject = (UriComponentsBase) ois.readObject();
		assertThat(uriComponents.toString()).isEqualTo(readObject.toString());
	}

	@Test
	public void copyToUriComponentsBuilder() {
		UriComponentsBase source = UriComponentsBuilder.fromPath("/foo/bar").pathSegment("ba/z").build();
		UriComponentsBuilder targetBuilder = UriComponentsBuilder.newInstance();
		source.copyToUriComponentsBuilder(targetBuilder);
		UriComponentsBase result = targetBuilder.build().encode();
		assertEquals("/foo/bar/ba%2Fz", result.getPath());
		assertEquals(Arrays.asList("foo", "bar", "ba%2Fz"), result.getPathSegments());
	}

	@Test
	public void equalsHierarchicalUriComponents() throws Exception {
		String url = "http://example.com";
		UriComponentsBase uric1 = UriComponentsBuilder.fromUriString(url).path("/{foo}").query("bar={baz}").build();
		UriComponentsBase uric2 = UriComponentsBuilder.fromUriString(url).path("/{foo}").query("bar={baz}").build();
		UriComponentsBase uric3 = UriComponentsBuilder.fromUriString(url).path("/{foo}").query("bin={baz}").build();
		assertThat(uric1).isInstanceOf(HierarchicalUriComponents.class);
		assertThat(uric1).isEqualTo(uric1);
		assertThat(uric1).isEqualTo(uric2);
		assertThat(uric1).isNotEqualTo(uric3);
	}

	@Test
	public void equalsOpaqueUriComponents() throws Exception {
		String baseUrl = "http:example.com";
		UriComponentsBase uric1 = UriComponentsBuilder.fromUriString(baseUrl + "/foo/bar").build();
		UriComponentsBase uric2 = UriComponentsBuilder.fromUriString(baseUrl + "/foo/bar").build();
		UriComponentsBase uric3 = fromUriString(baseUrl + "/foo/bin").build();

		assertThat(uric1).isInstanceOf(OpaqueUriComponents.class);
		assertThat(uric1).isEqualTo(uric1);
		assertThat(uric1).isEqualTo(uric2);
		assertThat(uric1).isNotEqualTo(uric3);
	}

}
