package de.qaware.web.util.uri;

/**
 *
 */
interface PathComponentBuilder {

	/*@Nullable*/
	HierarchicalUriComponents.PathComponent build();

	PathComponentBuilder cloneBuilder();
}
