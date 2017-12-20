/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.qaware.web.util;


import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * Extension of {@link UriComponents} for opaque URIs.
 *
 * @author Arjen Poutsma
 * @author Phillip Webb
 * @see <a href="http://tools.ietf.org/html/rfc3986#section-1.2.3">Hierarchical vs Opaque URIs</a>
 * @since 3.2
 */
@SuppressWarnings("serial")
final class OpaqueUriComponents extends UriComponents {

	/*@Nullable*/
	private final String ssp;


	OpaqueUriComponents(/*@Nullable*/ String scheme, /*@Nullable*/ String schemeSpecificPart, /*@Nullable*/ String fragment) {
		super(scheme, fragment);
		this.ssp = schemeSpecificPart;
	}


	@Override
	/*@Nullable*/
	public String getHost() {
		return null;
	}

	@Override
	public int getPort() {
		return -1;
	}

	@Override
	public String getPath() {
		return null;
	}


	public String getQuery() {
		return null;
	}
	@Override
	public UriComponents encode(Charset charset) {
		return this;
	}

	@Override
	public UriComponents normalize() {
		return this;
	}

	@Override
	public String toUriString() {
		StringBuilder uriBuilder = new StringBuilder();

		if (getScheme() != null) {
			uriBuilder.append(getScheme());
			uriBuilder.append(':');
		}
		if (this.ssp != null) {
			uriBuilder.append(this.ssp);
		}
		if (getFragment() != null) {
			uriBuilder.append('#');
			uriBuilder.append(getFragment());
		}

		return uriBuilder.toString();
	}

	@Override
	public URI toUri() {
		try {
			return new URI(getScheme(), this.ssp, getFragment());
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException("Could not create URI object: " + ex.getMessage(), ex);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof OpaqueUriComponents)) {
			return false;
		}

		OpaqueUriComponents other = (OpaqueUriComponents) obj;
		return Objects.deepEquals(getScheme(), other.getScheme()) &&
				Objects.deepEquals(this.ssp, other.ssp) &&
				Objects.deepEquals(getFragment(), other.getFragment());

	}

	@Override
	public int hashCode() {
		int result = Objects.hashCode(getScheme());
		result = 31 * result + Objects.hashCode(this.ssp);
		result = 31 * result + Objects.hashCode(getFragment());
		return result;
	}

}
