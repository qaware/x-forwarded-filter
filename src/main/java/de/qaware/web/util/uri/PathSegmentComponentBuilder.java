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

import java.util.LinkedList;
import java.util.List;

/**
 * PathSegmentComponentBuilder
 */
final class PathSegmentComponentBuilder implements PathComponentBuilder {

	private final List<String> pathSegments = new LinkedList<>();

	/**
	 * Append pathSegments to this builder
	 *
	 * @param pathSegments to append
	 */
	public void append(String... pathSegments) {
		for (String pathSegment : pathSegments) {
			if (StringUtils.isNotBlank(pathSegment)) {
				this.pathSegments.add(pathSegment);
			}
		}
	}

	@Override
	public PathComponent build() {
		return (this.pathSegments.isEmpty() ? null :
				new PathSegmentComponent(this.pathSegments));
	}

	@Override
	public PathSegmentComponentBuilder cloneBuilder() {
		PathSegmentComponentBuilder builder = new PathSegmentComponentBuilder();
		builder.pathSegments.addAll(this.pathSegments);
		return builder;
	}
}
