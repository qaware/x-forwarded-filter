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

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

/**
 *
 */
final class NullPathComponent implements PathComponent {

	private static final long serialVersionUID = 1;

	private static final NullPathComponent INSTANCE = new NullPathComponent();

	private NullPathComponent() {
		//single instance only
	}

	/**
	 * Instance of this immutable class
	 *
	 * @return Instance
	 */
	static NullPathComponent getInstance() {
		return INSTANCE;
	}

	@Override
	public String getPath() {
		return "";
	}

	@Override
	public List<String> getPathSegments() {
		return Collections.emptyList();
	}

	@Override
	public PathComponent encode(Charset charset) {
		return this;
	}

	@Override
	public void verify() {
		//nothing to verify
	}

	@Override
	public PathComponent expand(UriTemplateVariables uriVariables) {
		return this;
	}

	@Override
	public void copyToUriComponentsBuilder(UriComponentsBuilder builder) {
		//Null Path cannot copy
	}

	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	@Override
	public boolean equals(Object obj) {
		//static final anonymous class can compare with ==
		return (this == obj);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
