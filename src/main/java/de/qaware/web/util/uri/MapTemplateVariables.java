package de.qaware.web.util.uri;

import java.util.Map;

/**
 * URI template variables backed by a map.
 */
final class MapTemplateVariables extends UriTemplateVariables {

	private final Map<String, ?> uriVariables;

	MapTemplateVariables(Map<String, ?> uriVariables) {
		this.uriVariables = uriVariables;
	}

	@Override
	/*@Nullable*/
	public Object getValue(/*@Nullable*/ String name) {
		if (!this.uriVariables.containsKey(name)) {
			throw new IllegalArgumentException("Map has no value for '" + name + "'");
		}
		return this.uriVariables.get(name);
	}
}
