package de.qaware.web.util.uri;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

/**
 *
 */
final class NullPathComponent implements PathComponent {

	private static final long serialVersionUID = 1;

	private static final NullPathComponent INSTANCE = new NullPathComponent();

	private NullPathComponent() {
		//single instance only
	}

	/**
	 * Instance of this immutable class
	 *
	 * @return Instance
	 */
	static NullPathComponent getInstance() {
		return INSTANCE;
	}

	@Override
	public String getPath() {
		return "";
	}

	@Override
	public List<String> getPathSegments() {
		return Collections.emptyList();
	}

	@Override
	public PathComponent encode(Charset charset) {
		return this;
	}

	@Override
	public void verify() {
		//nothing to verify
	}

	@Override
	public PathComponent expand(UriTemplateVariables uriVariables) {
		return this;
	}

	@Override
	public void copyToUriComponentsBuilder(UriComponentsBuilder builder) {
		//Null Path cannot copy
	}

	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	@Override
	public boolean equals(Object obj) {
		//static final anonymous class can compare with ==
		return (this == obj);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
