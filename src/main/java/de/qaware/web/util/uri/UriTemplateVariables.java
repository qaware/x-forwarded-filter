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

package de.qaware.web.util.uri;

/**
 * Defines the contract for URI Template variables
 */
public abstract class UriTemplateVariables {

	static final Object SKIP_VALUE = UriTemplateVariables.class;

	/**
	 * Get the value for the given URI variable name.
	 * If the value is {@code null}, an empty String is expanded.
	 * If the value is {@link #SKIP_VALUE}, the URI variable is not expanded.
	 *
	 * @param name the variable name
	 * @return the variable value, possibly {@code null} or {@link #SKIP_VALUE}
	 */
	/*@Nullable*/
	abstract Object getValue(/*@Nullable*/ String name);
}
