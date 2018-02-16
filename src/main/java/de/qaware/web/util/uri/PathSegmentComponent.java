package de.qaware.web.util.uri;

import org.apache.commons.lang3.Validate;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a path backed by a String list (i.e. path segments).
 */
final class PathSegmentComponent implements PathComponent {

	private static final long serialVersionUID = 1;

	private final List<String> pathSegments;

	/**
	 * (Immutable) PathSegment Component form list of path segments
	 *
	 * @param pathSegments segments
	 */
	PathSegmentComponent(List<String> pathSegments) {
		Validate.notNull(pathSegments, "List must not be null");
		this.pathSegments = Collections.unmodifiableList(new ArrayList<>(pathSegments));
	}

	@Override
	public String getPath() {
		StringBuilder pathBuilder = new StringBuilder();
		pathBuilder.append(UriComponentsBase.PATH_DELIMITER);
		for (Iterator<String> iterator = this.pathSegments.iterator(); iterator.hasNext(); ) {
			String pathSegment = iterator.next();
			pathBuilder.append(pathSegment);
			if (iterator.hasNext()) {
				pathBuilder.append(UriComponentsBase.PATH_DELIMITER);
			}
		}
		return pathBuilder.toString();
	}

	@Override
	@SuppressWarnings("squid:S2384")//Mutable members - ok inner workings
	public List<String> getPathSegments() {
		return this.pathSegments;
	}

	@Override
	public PathComponent encode(Charset charset) {
		List<String> lpathSegments = getPathSegments();
		List<String> encodedPathSegments = new ArrayList<>(lpathSegments.size());
		for (String pathSegment : lpathSegments) {
			String encodedPathSegment = HierarchicalUriComponents.encodeUriComponent(pathSegment, charset, URIComponentType.PATH_SEGMENT);
			encodedPathSegments.add(encodedPathSegment);
		}
		return new PathSegmentComponent(encodedPathSegments);
	}

	@Override
	public void verify() {
		for (String pathSegment : getPathSegments()) {
			UriComponentsBase.verifyUriComponent(pathSegment, URIComponentType.PATH_SEGMENT);
		}
	}

	@Override
	public PathComponent expand(UriTemplateVariables uriVariables) {
		List<String> lPathSegments = getPathSegments();
		List<String> expandedPathSegments = new ArrayList<>(lPathSegments.size());
		for (String pathSegment : lPathSegments) {
			String expandedPathSegment = UriComponentsBase.expandUriComponent(pathSegment, uriVariables);
			expandedPathSegments.add(expandedPathSegment);
		}
		return new PathSegmentComponent(expandedPathSegments);
	}

	@Override
	public void copyToUriComponentsBuilder(UriComponentsBuilder builder) {
		builder.pathSegment(getPathSegments().toArray(new String[getPathSegments().size()]));
	}

	@Override
	public boolean equals(Object obj) {
		return (this == obj || (obj instanceof PathSegmentComponent &&
				getPathSegments().equals(((PathSegmentComponent) obj).getPathSegments())));
	}

	@Override
	public int hashCode() {
		return getPathSegments().hashCode();
	}
}
