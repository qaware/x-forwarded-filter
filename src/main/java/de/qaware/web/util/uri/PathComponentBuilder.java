package de.qaware.web.util.uri;

/**
 * Builder Interface
 */
interface PathComponentBuilder {

	PathComponent build();

	PathComponentBuilder cloneBuilder();
}
