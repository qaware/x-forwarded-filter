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

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Array;

/**
 * QueryUriTemplateVariables
 */
class QueryUriTemplateVariables extends UriTemplateVariables {

	private final UriTemplateVariables delegate;

	QueryUriTemplateVariables(UriTemplateVariables delegate) {
		this.delegate = delegate;
	}

	@Override
	public Object getValue(/*@Nullable*/ String name) {
		Object value = this.delegate.getValue(name);

		if (isArray(value)) {
			value = StringUtils.join(", ", toObjectArray(value));
		}
		return value;
	}

	private static boolean isArray(/*@Nullable*/ Object obj) {
		return (obj != null && obj.getClass().isArray());
	}

	private static Object[] toObjectArray(/*@Nullable*/ Object source) {
		if (source instanceof Object[]) {
			return (Object[]) source;
		}
		if (source == null) {
			return new Object[0];
		}
		if (!source.getClass().isArray()) {
			throw new IllegalArgumentException("Source is not an array: " + source);
		}
		int length = Array.getLength(source);
		if (length == 0) {
			return new Object[0];
		}
		Class<?> wrapperType = Array.get(source, 0).getClass();
		Object[] newArray = (Object[]) Array.newInstance(wrapperType, length);
		for (int i = 0; i < length; i++) {
			newArray[i] = Array.get(source, i);
		}
		return newArray;
	}
}
