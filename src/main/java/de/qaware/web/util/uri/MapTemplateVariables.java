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
package de.qaware.web.util.uri;

import java.util.Map;

/**
 * URI template variables backed by a map.
 */
final class MapTemplateVariables extends UriTemplateVariables {

	private final Map<String, ?> uriVariables;

	MapTemplateVariables(Map<String, ?> uriVariables) {
		this.uriVariables = uriVariables;
	}

	@Override
	/*@Nullable*/
	public Object getValue(/*@Nullable*/ String name) {
		if (!this.uriVariables.containsKey(name)) {
			throw new IllegalArgumentException("Map has no value for '" + name + "'");
		}
		return this.uriVariables.get(name);
	}
}
