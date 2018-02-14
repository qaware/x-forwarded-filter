package de.qaware.web.util.uri;

import java.util.Arrays;
import java.util.Iterator;

/**
 * URI template variables backed by a variable argument array.
 */
final class VarArgsTemplateVariables extends UriTemplateVariables {

	private final Iterator<Object> valueIterator;

	VarArgsTemplateVariables(Object... uriVariableValues) {
		this.valueIterator = Arrays.asList(uriVariableValues).iterator();
	}

	@Override
	/*@Nullable*/
	public Object getValue(/*@Nullable*/ String name) {
		if (!this.valueIterator.hasNext()) {
			throw new IllegalArgumentException("Not enough variable values available to expand '" + name + "'");
		}
		return this.valueIterator.next();
	}
}
