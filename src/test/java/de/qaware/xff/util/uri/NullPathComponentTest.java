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
package de.qaware.xff.util.uri;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class NullPathComponentTest {
	private static final NullPathComponent NPC = NullPathComponent.getInstance();
	@Test
	public void test(){
		assertEquals(NPC.hashCode(),NPC.hashCode());
		assertEquals(NPC,NPC.expand(null));
		assertEquals(NPC.encode(null),NPC);
		assertEquals(Collections.emptyList(),NPC.getPathSegments());
		assertTrue(NPC.equals(NPC));
		UriComponentsBuilder ucp=UriComponentsBuilder.newInstance();
		NPC.copyToUriComponentsBuilder(ucp);
		assertTrue(UriComponentsBuilder.newInstance().toUriString().equals(ucp.toUriString()));
	}
}
