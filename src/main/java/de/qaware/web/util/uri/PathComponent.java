package de.qaware.web.util.uri;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Defines the contract for path (segments).
 */
public interface PathComponent extends Serializable {

	/**
	 * Return the path. Can be {@code null}.
	 *
	 * @return the path
	 */
	String getPath();

	/**
	 * Return the list of path segments. Empty if no path has been set.
	 */
	List<String> getPathSegments();

	/**
	 * Encode all URI components using their specific encoding rules, and
	 * returns the result as a new {@code UriComponents} instance.
	 *
	 * @param charset the encoding of the values contained in this map
	 * @return the encoded URI components
	 */
	PathComponent encode(Charset charset);

	/**
	 * Verifies all URI components to determine whether they contain any illegal
	 * characters, throwing an {@code IllegalArgumentException} if so.
	 *
	 * @throws IllegalArgumentException if any component has illegal characters
	 */
	void verify();

	/**
	 * Expands uri components into path components
	 *
	 * @param uriVariables {@see UriTemplateVariables}
	 * @return corresponding {@see PathComponent}
	 */
	PathComponent expand(UriTemplateVariables uriVariables);

	/**
	 * Set all components of the given UriComponentsBuilder.
	 *
	 * @since 4.2
	 * @param builder{@see UriComponentsBuilder}
	 */
	void copyToUriComponentsBuilder(UriComponentsBuilder builder);
}
