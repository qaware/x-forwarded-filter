package de.qaware.web.util.uri;

import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;

/**
 *
 */
class PathSegmentComponentBuilder implements PathComponentBuilder {

	private final List<String> pathSegments = new LinkedList<>();

	public void append(String... pathSegments) {
		for (String pathSegment : pathSegments) {
			if (StringUtils.isNotBlank(pathSegment)) {
				this.pathSegments.add(pathSegment);
			}
		}
	}

	@Override
	public HierarchicalUriComponents.PathComponent build() {
		return (this.pathSegments.isEmpty() ? null :
				new HierarchicalUriComponents.PathSegmentComponent(this.pathSegments));
	}

	@Override
	public PathSegmentComponentBuilder cloneBuilder() {
		PathSegmentComponentBuilder builder = new PathSegmentComponentBuilder();
		builder.pathSegments.addAll(this.pathSegments);
		return builder;
	}
}
