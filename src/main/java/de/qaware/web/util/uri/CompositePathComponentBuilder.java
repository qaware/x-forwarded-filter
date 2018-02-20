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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static de.qaware.web.util.uri.UriComponentsBase.PATH_DELIMITER_STRING;

/**
 *
 */
final class CompositePathComponentBuilder implements PathComponentBuilder {

	private final LinkedList<PathComponentBuilder> builders = new LinkedList<>();

	/**
	 * Adds path segments to this builder
	 *
	 * @param pathSegments segments to add
	 */
	public void addPathSegments(String... pathSegments) {
		if (ArrayUtils.isEmpty(pathSegments)) {
			return;
		}
		PathSegmentComponentBuilder psBuilder = getLastBuilder(PathSegmentComponentBuilder.class);
		FullPathComponentBuilder fpBuilder = getLastBuilder(FullPathComponentBuilder.class);
		if (psBuilder == null) {
			psBuilder = new PathSegmentComponentBuilder();
			this.builders.add(psBuilder);
			if (fpBuilder != null) {
				fpBuilder.removeTrailingSlash();
			}
		}
		psBuilder.append(pathSegments);

	}

	/**
	 * +
	 * add the url path to this builder
	 *
	 * @param path path to add
	 */
	public void addPath(String path) {
		if (StringUtils.isBlank(path)) {
			return;
		}
		PathSegmentComponentBuilder psBuilder = getLastBuilder(PathSegmentComponentBuilder.class);
		FullPathComponentBuilder fpBuilder = getLastBuilder(FullPathComponentBuilder.class);

		if (fpBuilder == null) {
			fpBuilder = new FullPathComponentBuilder();
			this.builders.add(fpBuilder);
		}
		if (psBuilder != null) {
			fpBuilder.append(path.startsWith(PATH_DELIMITER_STRING) ? path : (PATH_DELIMITER_STRING + path));
		} else {
			fpBuilder.append(path);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T getLastBuilder(Class<T> builderClass) {
		if (!this.builders.isEmpty()) {
			PathComponentBuilder last = this.builders.getLast();
			if (builderClass.isInstance(last)) {
				return (T) last;
			}
		}
		return null;
	}

	@Override
	public PathComponent build() {
		int size = this.builders.size();
		List<PathComponent> components = new ArrayList<>(size);
		for (PathComponentBuilder componentBuilder : this.builders) {
			PathComponent pathComponent = componentBuilder.build();
			if (pathComponent != null) {
				components.add(pathComponent);
			}
		}
		if (components.isEmpty()) {
			return NullPathComponent.getInstance();
		}
		if (components.size() == 1) {
			return components.get(0);
		}
		return new PathComponentComposite(components);
	}

	@Override
	public CompositePathComponentBuilder cloneBuilder() {
		CompositePathComponentBuilder compositeBuilder = new CompositePathComponentBuilder();
		for (PathComponentBuilder builder : this.builders) {
			compositeBuilder.builders.add(builder.cloneBuilder());
		}
		return compositeBuilder;
	}
}
