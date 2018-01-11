package de.qaware.web.util.uri;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static de.qaware.web.util.uri.UriComponents.PATH_DELIMITER_STRING;

/**
 *
 */
class CompositePathComponentBuilder implements PathComponentBuilder {

	private final LinkedList<PathComponentBuilder> builders = new LinkedList<>();

	public void addPathSegments(String... pathSegments) {
		if (!ArrayUtils.isEmpty(pathSegments)) {
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
	}

	public void addPath(String path) {
		if (StringUtils.isNotBlank(path)) {
			PathSegmentComponentBuilder psBuilder = getLastBuilder(PathSegmentComponentBuilder.class);
			FullPathComponentBuilder fpBuilder = getLastBuilder(FullPathComponentBuilder.class);

			if (fpBuilder == null) {
				fpBuilder = new FullPathComponentBuilder();
				this.builders.add(fpBuilder);
			}
			if (psBuilder != null) {
				fpBuilder.append(path.startsWith(PATH_DELIMITER_STRING) ? path : PATH_DELIMITER_STRING + path);
			} else {
				fpBuilder.append(path);
			}
		}

	}

	@SuppressWarnings("unchecked")
	/*@Nullable*/
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
	public HierarchicalUriComponents.PathComponent build() {
		int size = this.builders.size();
		List<HierarchicalUriComponents.PathComponent> components = new ArrayList<>(size);
		for (PathComponentBuilder componentBuilder : this.builders) {
			HierarchicalUriComponents.PathComponent pathComponent = componentBuilder.build();
			if (pathComponent != null) {
				components.add(pathComponent);
			}
		}
		if (components.isEmpty()) {
			return HierarchicalUriComponents.NULL_PATH_COMPONENT;
		}
		if (components.size() == 1) {
			return components.get(0);
		}
		return new HierarchicalUriComponents.PathComponentComposite(components);
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
