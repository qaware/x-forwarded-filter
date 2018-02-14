package de.qaware.web.util.uri;

import org.apache.commons.lang3.Validate;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a collection of PathComponents.
 */
final class PathComponentComposite implements PathComponent {

	private final List<PathComponent> pathComponents;

	/**
	 * Mutable - Composite path from list of path components
	 * @param pathComponents components
	 */
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
