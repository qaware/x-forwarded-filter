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

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a path backed by a String.
 */
final class FullPathComponent implements PathComponent {

	private static final long serialVersionUID = 1;

	private final String path;

	/**
	 * Full path component from path
	 *
	 * @param path path
	 */
	FullPathComponent(String path) {
		this.path = (path != null ? path : "");
	}

	@Override
	public String getPath() {
		return this.path;
	}

	@Override
	public List<String> getPathSegments() {
        List<String> segments = tokenizeTrimIgnoreEmpty(this.path, UriComponents.PATH_DELIMITER_STRING);
        return Collections.unmodifiableList(segments);
	}

	private static List<String> tokenizeTrimIgnoreEmpty(String path, String pathDelimiterString) {
		String[] tokens = StringUtils.split(path, pathDelimiterString);
		if (tokens == null || tokens.length == 0) {
			return Collections.emptyList();
		}

		List<String> result = new ArrayList<>(tokens.length);
		for (String token : tokens) {
			if (StringUtils.isNotBlank(token)) {
				result.add(token);
			}
		}
		return result;
	}

	@Override
	public PathComponent encode(Charset charset) {
		String encodedPath = HierarchicalUriComponents.encodeUriComponent(getPath(), charset, URIComponentType.PATH);
		return new FullPathComponent(encodedPath);
	}

	@Override
	public void verify() {
        UriComponents.verifyUriComponent(this.path, URIComponentType.PATH);
    }

	@Override
	public PathComponent expand(UriTemplateVariables uriVariables) {
        String expandedPath = UriComponents.expandUriComponent(getPath(), uriVariables);
        return new FullPathComponent(expandedPath);
	}

	@Override
	public void copyToUriComponentsBuilder(UriComponentsBuilder builder) {
		builder.path(getPath());
	}

	@Override
	public boolean equals(Object obj) {
		return (this == obj || (obj instanceof FullPathComponent &&
				getPath().equals(((FullPathComponent) obj).getPath())));
	}

	@Override
	public int hashCode() {
		return getPath().hashCode();
	}
}
