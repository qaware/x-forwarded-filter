package de.qaware.web.util.uri;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Defines the contract for path (segments).
 */
public interface PathComponent extends Serializable {

	String getPath();

	List<String> getPathSegments();

	PathComponent encode(Charset charset);

	void verify();

	PathComponent expand(UriTemplateVariables uriVariables);

	void copyToUriComponentsBuilder(UriComponentsBuilder builder);
}
