package de.qaware.web.util.uri;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a path backed by a String.
 */
final class FullPathComponent implements PathComponent {

	private final String path;

	/**
	 * Full path component from path
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
		List<String> segments = tokenizeTrimIgnoreEmpty(this.path, UriComponentsBase.PATH_DELIMITER_STRING);
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
		UriComponentsBase.verifyUriComponent(this.path, URIComponentType.PATH);
	}

	@Override
	public PathComponent expand(UriTemplateVariables uriVariables) {
		String expandedPath = UriComponentsBase.expandUriComponent(getPath(), uriVariables);
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
