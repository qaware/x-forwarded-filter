/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.qaware.xff.util;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link HttpHeaders}.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Brian Clozel
 */
public class HttpHeadersTest {

	private final HttpHeaders headers = new HttpHeaders();


	@Test
	public void getFirst() {
		headers.add("Cache-Control", "max-age=1000, public");
		headers.add("Cache-Control", "s-maxage=1000");
		assertThat(headers.getFirst("Cache-Control")).isEqualTo("max-age=1000, public");
	}

	@Test
	public void addAllMap() {
		List<String> expected = Arrays.asList("max-age=1000, public", "s-maxage=1000");
		headers.addAll(Collections.singletonMap("Cache-Control", expected));
		assertThat(headers.getFirst("Cache-Control")).isEqualTo("max-age=1000, public");
		assertThat(headers.get("Cache-Control")).isEqualTo(expected);
	}

	@Test
	public void addAllList() {
		List<String> expected = Arrays.asList("max-age=1000, public", "s-maxage=1000");
		headers.addAll("Cache-Control", expected);
		assertThat(headers.getFirst("Cache-Control")).isEqualTo("max-age=1000, public");
		assertThat(headers.get("Cache-Control")).isEqualTo(expected);
	}

	@Test
	public void putAllMap() {
		List<String> expected = Arrays.asList("max-age=1000, public", "s-maxage=1000");
		headers.putAll(Collections.singletonMap("Cache-Control", expected));
		assertThat(headers.getFirst("Cache-Control")).isEqualTo("max-age=1000, public");
		assertThat(headers.get("Cache-Control")).isEqualTo(expected);
	}

	@Test
	public void put() {
		List<String> expected = Collections.singletonList("s-maxage=1000");
		headers.put("Cache-Control", Collections.singletonList("max-age=1000, public"));
		headers.put("Cache-Control", Collections.singletonList("s-maxage=1000")); //WILL OVERWRITE
		assertThat(headers).isNotEmpty().hasSize(1);
		assertThat(headers.getFirst("Cache-Control")).isEqualTo("s-maxage=1000");
		assertThat(headers.get("Cache-Control")).isEqualTo(expected);
	}

	@Test
	public void clear() {
		headers.put("Cache-Control", Collections.singletonList("s-maxage=1000")); //WILL OVERWRITE
		headers.clear();
		assertThat(headers).isEmpty();
	}

	@Test
	public void sets() {
		List<String> a = Collections.singletonList("aaa");
		List<String> b = Collections.singletonList("bbb");
		headers.add("A", "aaa"); //WILL OVERWRITE
		headers.add("B", "bbb"); //WILL OVERWRITE
		assertThat(headers.keySet()).containsExactly("a", "b");//case insensitive map!
		assertThat(headers.values()).containsExactly(a, b);
		assertThat(headers.entrySet()).contains(Pair.of("a", a), Pair.of("b", b));
	}

	@Test
	public void containsKey() {
		headers.add("Cache-Control", "max-age=1000, public");
		headers.add("Cache-Control", "s-maxage=1000");
		assertThat(headers).isNotEmpty().hasSize(2);
		assertThat(headers.containsKey("Cache-Control")).isTrue();
		assertThat(headers.containsKey("Cache-ControlBAD")).isFalse();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void containsValue() {
		headers.containsValue(Collections.singletonList("foo"));
	}

	@Test
	public void contentLength() {
		long length = 42L;
		headers.setContentLength(length);
		assertEquals("Invalid Content-Length header", length, headers.getContentLength());
		assertEquals("Invalid Content-Length header", "42", headers.getFirst("Content-Length"));
	}


}
