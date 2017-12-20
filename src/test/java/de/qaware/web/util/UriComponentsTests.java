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

package de.qaware.web.util;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Phillip Webb
 */
public class UriComponentsTests {

	@Test
	public void encode() {
		UriComponents uriComponents = UriComponentsBuilder.fromPath("/hotel list").build();
		UriComponents encoded = uriComponents.encode();
		assertThat("/hotel%20list").isEqualTo(encoded.getPath());
	}

	@Test
	public void toUriEncoded() throws URISyntaxException {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(
				"http://example.com/hotel list/Z\u00fcrich").build();
		assertThat(new URI("http://example.com/hotel%20list/Z%C3%BCrich")).isEqualTo( uriComponents.encode().toUri());
	}

	@Test
	public void toUriNotEncoded() throws URISyntaxException {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(
				"http://example.com/hotel list/Z\u00fcrich").build();
		assertThat(new URI("http://example.com/hotel%20list/Z\u00fcrich")).isEqualTo( uriComponents.toUri());
	}

	@Test
	public void toUriAlreadyEncoded() throws URISyntaxException {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(
				"http://example.com/hotel%20list/Z%C3%BCrich").build(true);
		UriComponents encoded = uriComponents.encode();
		assertThat(new URI("http://example.com/hotel%20list/Z%C3%BCrich")).isEqualTo( encoded.toUri());
	}

	@Test
	public void toUriWithIpv6HostAlreadyEncoded() throws URISyntaxException {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(
				"http://[1abc:2abc:3abc::5ABC:6abc]:8080/hotel%20list/Z%C3%BCrich").build(true);
		UriComponents encoded = uriComponents.encode();
		assertThat(new URI("http://[1abc:2abc:3abc::5ABC:6abc]:8080/hotel%20list/Z%C3%BCrich")).isEqualTo( encoded.toUri());
	}


	// SPR-12123

	@Test
	public void port() {
		UriComponents uri1 = UriComponentsBuilder.fromUriString("http://example.com:8080/bar").build();
		UriComponents uri2 = UriComponentsBuilder.fromUriString("http://example.com/bar").port(8080).build();

		assertThat(8080).isEqualTo( uri1.getPort());
		assertThat("http://example.com:8080/bar").isEqualTo( uri1.toUriString());
		assertThat(8080).isEqualTo( uri2.getPort());
		assertThat("http://example.com:8080/bar").isEqualTo( uri2.toUriString());

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
		UriComponents uriComponents = UriComponentsBuilder.fromUriString("http://example.com/foo/../bar").build();
		assertThat("http://example.com/bar").isEqualTo( uriComponents.normalize().toString());
	}

	@Test
	public void serializable() throws Exception {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(
				"http://example.com").path("/{foo}").query("bar={baz}").build();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(uriComponents);
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
		UriComponents readObject = (UriComponents) ois.readObject();
		assertThat(uriComponents.toString()).isEqualTo( readObject.toString());
	}



	@Test
	public void equalsHierarchicalUriComponents() throws Exception {
		String url = "http://example.com";
		UriComponents uric1 = UriComponentsBuilder.fromUriString(url).path("/{foo}").query("bar={baz}").build();
		UriComponents uric2 = UriComponentsBuilder.fromUriString(url).path("/{foo}").query("bar={baz}").build();
		UriComponents uric3 = UriComponentsBuilder.fromUriString(url).path("/{foo}").query("bin={baz}").build();
		assertThat(uric1).isInstanceOf(HierarchicalUriComponents.class);
		assertThat(uric1).isEqualTo( uric1);
		assertThat(uric1).isEqualTo(uric2);
		assertThat(uric1).isNotEqualTo(uric3);
	}

	@Test
	public void equalsOpaqueUriComponents() throws Exception {
		String baseUrl = "http:example.com";
		UriComponents uric1 = UriComponentsBuilder.fromUriString(baseUrl + "/foo/bar").build();
		UriComponents uric2 = UriComponentsBuilder.fromUriString(baseUrl + "/foo/bar").build();
		UriComponents uric3 = UriComponentsBuilder.fromUriString(baseUrl + "/foo/bin").build();

		assertThat(uric1).isInstanceOf(OpaqueUriComponents.class);
		assertThat(uric1).isEqualTo( uric1);
		assertThat(uric1).isEqualTo(uric2);
		assertThat(uric1).isNotEqualTo(uric3);
	}

}
