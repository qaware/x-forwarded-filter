package de.qaware.web.util.uri;

/**
 * Builder Interface
 */
interface PathComponentBuilder {

	/**
	 * Get  {@see PathComponent} constructed with this {@see PathComponentBuilder}
	 *
	 * @return {@see PathComponent} constructed by this {@see PathComponentBuilder}
	 */
	PathComponent build();

	/**
	 * Shortcut for internal copy constructor
	 *
	 * @return copy of this builder
	 */
	PathComponentBuilder cloneBuilder();
}
