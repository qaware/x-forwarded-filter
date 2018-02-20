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

import org.apache.commons.lang3.Validate;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a collection of PathComponents.
 */
final class PathComponentComposite implements PathComponent {

	private static final long serialVersionUID = 1;

	private final List<PathComponent> pathComponents;

	/**
	 * Mutable - Composite path from list of path components
	 *
	 * @param pathComponents components
	 */
	@SuppressWarnings("squid:S2384")//Mutable member is ok - internal workings
	PathComponentComposite(List<PathComponent> pathComponents) {
		Validate.notNull(pathComponents, "PathComponent List must not be null");
		this.pathComponents = pathComponents;
	}

	@Override
	public String getPath() {
		StringBuilder pathBuilder = new StringBuilder();
		for (PathComponent pathComponent : this.pathComponents) {
			pathBuilder.append(pathComponent.getPath());
		}
		return pathBuilder.toString();
	}

	@Override
	public List<String> getPathSegments() {
		List<String> result = new ArrayList<>();
		for (PathComponent pathComponent : this.pathComponents) {
			result.addAll(pathComponent.getPathSegments());
		}
		return result;
	}

	@Override
	public PathComponent encode(Charset charset) {
		List<PathComponent> encodedComponents = new ArrayList<>(this.pathComponents.size());
		for (PathComponent pathComponent : this.pathComponents) {
			encodedComponents.add(pathComponent.encode(charset));
		}
		return new PathComponentComposite(encodedComponents);
	}

	@Override
	public void verify() {
		for (PathComponent pathComponent : this.pathComponents) {
			pathComponent.verify();
		}
	}

	@Override
	public PathComponent expand(UriTemplateVariables uriVariables) {
		List<PathComponent> expandedComponents = new ArrayList<>(this.pathComponents.size());
		for (PathComponent pathComponent : this.pathComponents) {
			expandedComponents.add(pathComponent.expand(uriVariables));
		}
		return new PathComponentComposite(expandedComponents);
	}

	@Override
	public void copyToUriComponentsBuilder(UriComponentsBuilder builder) {
		for (PathComponent pathComponent : this.pathComponents) {
			pathComponent.copyToUriComponentsBuilder(builder);
		}
	}
}
