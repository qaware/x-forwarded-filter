/*
 * Copyright 2002-2016 the original author or authors.
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

package de.quaware.util;

import de.qaware.util.CollectionUtils;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 */
public class CollectionUtilsTests {

	@Test
	public void testIsEmpty() {
		assertTrue(CollectionUtils.isEmpty((Set<Object>) null));
		assertTrue(CollectionUtils.isEmpty((Map<String, String>) null));
		assertTrue(CollectionUtils.isEmpty(new HashMap<String, String>()));
		assertTrue(CollectionUtils.isEmpty(new HashSet<>()));

		List<Object> list = new LinkedList<>();
		list.add(new Object());
		assertFalse(CollectionUtils.isEmpty(list));

		Map<String, String> map = new HashMap<>();
		map.put("foo", "bar");
		assertFalse(CollectionUtils.isEmpty(map));
	}


}
