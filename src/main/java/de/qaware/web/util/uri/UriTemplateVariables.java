package de.qaware.web.util.uri;

/**
 * Defines the contract for URI Template variables
 */
public abstract class UriTemplateVariables {

	static final Object SKIP_VALUE = UriTemplateVariables.class;

	/**
	 * Get the value for the given URI variable name.
	 * If the value is {@code null}, an empty String is expanded.
	 * If the value is {@link #SKIP_VALUE}, the URI variable is not expanded.
	 *
	 * @param name the variable name
	 * @return the variable value, possibly {@code null} or {@link #SKIP_VALUE}
	 */
	/*@Nullable*/
	abstract Object getValue(/*@Nullable*/ String name);
}
