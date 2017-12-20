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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiMapUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Extension of {@link UriComponents} for hierarchical URIs.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @see <a href="http://tools.ietf.org/html/rfc3986#section-1.2.3">Hierarchical URIs</a>
 * @since 3.1.3
 */
@SuppressWarnings({"serial", "squid:S1948"})
final class HierarchicalUriComponents extends UriComponents {

	private static final char PATH_DELIMITER = '/';


	/*@Nullable*/
	private final String userInfo;

	/*@Nullable*/
	private final String host;

	/*@Nullable*/
	private final String port;

	private final PathComponent path;

	private final MultiValuedMap<String, String> queryParams;

	private final boolean encoded;


	/**
	 * Package-private constructor. All arguments are optional, and can be {@code null}.
	 *
	 * @param scheme      the scheme
	 * @param userInfo    the user info
	 * @param host        the host
	 * @param port        the port
	 * @param path        the path
	 * @param queryParams the query parameters
	 * @param fragment    the fragment
	 * @param encoded     whether the components are already encoded
	 * @param verify      whether the components need to be checked for illegal characters
	 */
	@SuppressWarnings("squid:S00107")
	//copy and builder constructor
	HierarchicalUriComponents(/*@Nullable*/ String scheme, /*@Nullable*/ String fragment, /*@Nullable*/ String userInfo,
			/*@Nullable*/ String host, /*@Nullable*/ String port, /*@Nullable*/ PathComponent path,
			/*@Nullable*/ MultiValuedMap<String, String> queryParams, boolean encoded, boolean verify) {

		super(scheme, fragment);

		this.userInfo = userInfo;
		this.host = host;
		this.port = port;
		this.path = (path != null ? path : NULL_PATH_COMPONENT);
		this.queryParams = (queryParams != null ? MultiMapUtils.unmodifiableMultiValuedMap(queryParams) : MultiMapUtils.EMPTY_MULTI_VALUED_MAP);
		this.encoded = encoded;

		if (verify) {
			verify();
		}
	}


	// Component getters

	/*@Nullable*/
	public String getUserInfo() {
		return this.userInfo;
	}

	@Override
	/*@Nullable*/
	public String getHost() {
		return this.host;
	}

	@Override
	public int getPort() {
		if (this.port == null) {
			return -1;
		} else if (this.port.contains("{")) {
			throw new IllegalStateException(
					"The port contains a URI variable but has not been expanded yet: " + this.port);
		}
		return Integer.parseInt(this.port);
	}

	//@NonNull
	@Override
	public String getPath() {
		return this.path.getPath();
	}

	@Override
	/*@Nullable*/
	public String getQuery() {
		if (this.queryParams.isEmpty()) {
			return null;
		}
		StringBuilder queryBuilder = new StringBuilder();

		for (Map.Entry<String, Collection<String>> entry : this.queryParams.asMap().entrySet()) {
			String name = entry.getKey();
			Collection<String> values = entry.getValue();
			if (CollectionUtils.isEmpty(values)) {
				appendQueryParamName(queryBuilder, name);
			} else {
				for (Object value : values) {
					appendQueryParamName(queryBuilder, name);

					if (value != null) {
						queryBuilder.append('=');
						queryBuilder.append(value.toString());
					}
				}
			}
		}
		return queryBuilder.toString();
	}

	private void appendQueryParamName(StringBuilder queryBuilder, String name) {
		if (queryBuilder.length() != 0) {
			queryBuilder.append('&');
		}
		queryBuilder.append(name);
	}


	// Encoding

	/**
	 * Encode all URI components using their specific encoding rules and return
	 * the result as a new {@code UriComponents} instance.
	 *
	 * @param charset the encoding of the values
	 * @return the encoded URI components
	 */
	@Override
	public HierarchicalUriComponents encode(Charset charset) {
		if (this.encoded) {
			return this;
		}
		String scheme = getScheme();
		String fragment = getFragment();
		String schemeTo = (scheme != null ? encodeUriComponent(scheme, charset, Type.SCHEME) : null);
		String fragmentTo = (fragment != null ? encodeUriComponent(fragment, charset, Type.FRAGMENT) : null);
		String userInfoTo = (this.userInfo != null ? encodeUriComponent(this.userInfo, charset, Type.USER_INFO) : null);
		String hostTo = (this.host != null ? encodeUriComponent(this.host, charset, getHostType()) : null);
		PathComponent pathTo = this.path.encode(charset);
		MultiValuedMap<String, String> paramsTo = encodeQueryParams(charset);
		return new HierarchicalUriComponents(schemeTo, fragmentTo, userInfoTo, hostTo, this.port,
				pathTo, paramsTo, true, false);
	}

	private MultiValuedMap<String, String> encodeQueryParams(Charset charset) {
		int size = this.queryParams.size();
		ArrayListValuedHashMap<String, String> result = new ArrayListValuedHashMap<>(size, 1);
		for (Map.Entry<String, Collection<String>> entry : this.queryParams.asMap().entrySet()) {
			String name = encodeUriComponent(entry.getKey(), charset, Type.QUERY_PARAM);
			List<String> values = new ArrayList<>(entry.getValue().size());
			for (String value : entry.getValue()) {
				values.add(encodeUriComponent(value, charset, Type.QUERY_PARAM));
			}
			result.putAll(name, values);
		}
		return result;
	}

	/**
	 * Encode the given source into an encoded String using the rules specified
	 * by the given component and with the given options.
	 *
	 * @param source  the source String
	 * @param charset the encoding of the source String
	 * @param type    the URI component for the source
	 * @return the encoded URI
	 * @throws IllegalArgumentException when the given value is not a valid URI component
	 */
	static String encodeUriComponent(String source, Charset charset, Type type) {
		if (!StringUtils.isNotBlank(source)) {
			return source;
		}
		Validate.notNull(charset, "Charset must not be null");
		Validate.notNull(type, "Type must not be null");

		byte[] bytes = source.getBytes(charset);
		ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
		boolean changed = false;
		for (byte b : bytes) {
			if (b < 0) {
				b += 256;
			}
			if (type.isAllowed(b)) {
				bos.write(b);
			} else {
				bos.write('%');
				char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
				char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
				bos.write(hex1);
				bos.write(hex2);
				changed = true;
			}
		}
		return (changed ? new String(bos.toByteArray(), charset) : source);
	}

	private Type getHostType() {
		return (this.host != null && this.host.startsWith("[") ? Type.HOST_IPV6 : Type.HOST_IPV4);
	}


	// Verifying

	/**
	 * Verifies all URI components to determine whether they contain any illegal
	 * characters, throwing an {@code IllegalArgumentException} if so.
	 *
	 * @throws IllegalArgumentException if any component has illegal characters
	 */
	private void verify() {
		if (!this.encoded) {
			return;
		}
		verifyUriComponent(getScheme(), Type.SCHEME);
		verifyUriComponent(this.userInfo, Type.USER_INFO);
		verifyUriComponent(this.host, getHostType());
		this.path.verify();
		for (Map.Entry<String, Collection<String>> entry : queryParams.asMap().entrySet()) {
			verifyUriComponent(entry.getKey(), Type.QUERY_PARAM);
			for (String value : entry.getValue()) {
				verifyUriComponent(value, Type.QUERY_PARAM);
			}
		}
		verifyUriComponent(getFragment(), Type.FRAGMENT);
	}

	private static void verifyUriComponent(/*@Nullable*/ String source, Type type) {
		if (source == null) {
			return;
		}
		int length = source.length();
		int pos = -1;
		while (++pos < length) {
			char ch = source.charAt(pos);
			if (ch == '%') {
				if ((pos + 2) < length) {
					char hex1 = source.charAt(pos + 1);
					char hex2 = source.charAt(pos + 2);
					int u = Character.digit(hex1, 16);
					int l = Character.digit(hex2, 16);
					if (u == -1 || l == -1) {
						throw new IllegalArgumentException("Invalid encoded sequence \"" +
								source.substring(pos) + "\"");
					}
					pos += 2;
				} else {
					throw new IllegalArgumentException("Invalid encoded sequence \"" +
							source.substring(pos) + "\"");
				}
			} else if (!type.isAllowed(ch)) {
				throw new IllegalArgumentException("Invalid character '" + ch + "' for " +
						type.name() + " in \"" + source + "\"");
			}
		}
	}


	// Expanding

	/**
	 * Normalize the path removing sequences like "path/..".
	 */
	@Override
	public UriComponents normalize() {
		String normalizedPath = cleanPath(getPath());
		return new HierarchicalUriComponents(getScheme(), getFragment(), this.userInfo, this.host, this.port,
				new FullPathComponent(normalizedPath), this.queryParams, this.encoded, false);
	}


	private static final String FOLDER_SEPARATOR = "/";

	private static final String WINDOWS_FOLDER_SEPARATOR = "\\";

	private static final String TOP_PATH = "..";

	private static final String CURRENT_PATH = ".";

	/**
	 * Normalize the path by suppressing sequences like "path/.." and
	 * inner simple dots.
	 * <p>The result is convenient for path comparison. For other uses,
	 * notice that Windows separators ("\") are replaced by simple slashes.
	 *
	 * @param path the original path
	 * @return the normalized path
	 */
	@SuppressWarnings("squid:S3776")
	public static String cleanPath(String path) {
		if (StringUtils.isBlank(path)) {
			return path;
		}
		String pathToUse = StringUtils.replace(path, WINDOWS_FOLDER_SEPARATOR, FOLDER_SEPARATOR);

		// Strip prefix from path to analyze, to not treat it as part of the
		// first path element. This is necessary to correctly parse paths like
		// "file:core/../core/io/Resource.class", where the ".." should just
		// strip the first "core" directory while keeping the "file:" prefix.
		int prefixIndex = pathToUse.indexOf(':');
		String prefix = "";
		if (prefixIndex != -1) {
			prefix = pathToUse.substring(0, prefixIndex + 1);
			if (prefix.contains("/")) {
				prefix = "";
			} else {
				pathToUse = pathToUse.substring(prefixIndex + 1);
			}
		}
		if (pathToUse.startsWith(FOLDER_SEPARATOR)) {
			prefix = prefix + FOLDER_SEPARATOR;
			pathToUse = pathToUse.substring(1);
		}

		String[] pathArray = StringUtils.splitPreserveAllTokens(pathToUse, FOLDER_SEPARATOR);
		List<String> pathElements = new LinkedList<>();
		int tops = 0;

		for (int i = pathArray.length - 1; i >= 0; i--) {
			String element = pathArray[i];
			if (CURRENT_PATH.equals(element)) {
				// Points to current directory - drop it.
				continue;
			}

			if (TOP_PATH.equals(element)) {
				// Registering top path found.
				tops++;
			} else {
				if (tops > 0) {
					// Merging path element with element corresponding to top path.
					tops--;
				} else {
					// Normal path element found.
					pathElements.add(0, element);
				}
			}
		}

		// Remaining top paths need to be retained.
		for (int i = 0; i < tops; i++) {
			pathElements.add(0, TOP_PATH);
		}

		return prefix + String.join(FOLDER_SEPARATOR, pathElements);
	}


	// Other functionality

	/**
	 * Returns a URI String from this {@code UriComponents} instance.
	 */
	@Override
	public String toUriString() {
		StringBuilder uriBuilder = new StringBuilder();
		if (getScheme() != null) {
			uriBuilder.append(getScheme());
			uriBuilder.append(':');
		}
		if (this.userInfo != null || this.host != null) {
			uriBuilder.append("//");
			if (this.userInfo != null) {
				uriBuilder.append(this.userInfo);
				uriBuilder.append('@');
			}
			if (this.host != null) {
				uriBuilder.append(host);
			}
			if (getPort() != -1) {
				uriBuilder.append(':');
				uriBuilder.append(port);
			}
		}
		String lpath = getPath();
		if (StringUtils.isNotBlank(lpath)) {
			if (uriBuilder.length() != 0 && lpath.charAt(0) != PATH_DELIMITER) {
				uriBuilder.append(PATH_DELIMITER);
			}
			uriBuilder.append(lpath);
		}
		String query = getQuery();
		if (query != null) {
			uriBuilder.append('?');
			uriBuilder.append(query);
		}
		if (getFragment() != null) {
			uriBuilder.append('#');
			uriBuilder.append(getFragment());
		}
		return uriBuilder.toString();
	}

	/**
	 * Returns a {@code URI} from this {@code UriComponents} instance.
	 */
	@Override
	public URI toUri() {
		try {
			if (this.encoded) {
				return new URI(toString());
			}
			else {
				String path = getPath();
				if (StringUtils.isNotEmpty(path) && path.charAt(0) != PATH_DELIMITER) {
					// Only prefix the path delimiter if something exists before it
					if (getScheme() != null || getUserInfo() != null || getHost() != null || getPort() != -1) {
						path = PATH_DELIMITER + path;
					}
				}
				return new URI(getScheme(), getUserInfo(), getHost(), getPort(), path, getQuery(),
						getFragment());
			}
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
		if (!(obj instanceof HierarchicalUriComponents)) {
			return false;
		}
		HierarchicalUriComponents other = (HierarchicalUriComponents) obj;
		return Objects.deepEquals(getScheme(), other.getScheme()) &&
				Objects.deepEquals(getUserInfo(), other.getUserInfo()) &&
				Objects.deepEquals(getHost(), other.getHost()) &&
				getPort() == other.getPort() &&
				this.path.equals(other.path) &&
				this.queryParams.equals(other.queryParams) &&
				Objects.deepEquals(getFragment(), other.getFragment());
	}

	@Override
	public int hashCode() {
		int result = Objects.hashCode(getScheme());
		result = 31 * result + Objects.hashCode(this.userInfo);
		result = 31 * result + Objects.hashCode(this.host);
		result = 31 * result + Objects.hashCode(this.port);
		result = 31 * result + this.path.hashCode();
		result = 31 * result + this.queryParams.hashCode();
		result = 31 * result + Objects.hashCode(getFragment());
		return result;
	}


	// Nested types

	/**
	 * Enumeration used to identify the allowed characters per URI component.
	 * <p>Contains methods to indicate whether a given character is valid in a specific URI component.
	 *
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>
	 */
	enum Type {

		SCHEME {
			@Override
			public boolean isAllowed(int c) {
				return isAlpha(c) || isDigit(c) || '+' == c || '-' == c || '.' == c;
			}
		},
		USER_INFO {
			@Override
			public boolean isAllowed(int c) {
				return isUnreserved(c) || isSubDelimiter(c) || ':' == c;
			}
		},
		HOST_IPV4 {
			@Override
			public boolean isAllowed(int c) {
				return isUnreserved(c) || isSubDelimiter(c);
			}
		},
		HOST_IPV6 {
			@Override
			public boolean isAllowed(int c) {
				return isUnreserved(c) || isSubDelimiter(c) || '[' == c || ']' == c || ':' == c;
			}
		},
		PATH {
			@Override
			public boolean isAllowed(int c) {
				return isPchar(c) || '/' == c;
			}
		},
		PATH_SEGMENT {
			@Override
			public boolean isAllowed(int c) {
				return isPchar(c);
			}
		},
		QUERY_PARAM {
			@Override
			public boolean isAllowed(int c) {
				if ('=' == c || '&' == c) {
					return false;
				} else {
					return isPchar(c) || '/' == c || '?' == c;
				}
			}
		},
		FRAGMENT {
			@Override
			public boolean isAllowed(int c) {
				return isPchar(c) || '/' == c || '?' == c;
			}
		};

		/**
		 * Indicates whether the given character is allowed in this URI component.
		 *
		 * @return {@code true} if the character is allowed; {@code false} otherwise
		 */
		public abstract boolean isAllowed(int c);

		/**
		 * Indicates whether the given character is in the {@code ALPHA} set.
		 *
		 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
		 */
		protected boolean isAlpha(int c) {
			return (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z');
		}

		/**
		 * Indicates whether the given character is in the {@code DIGIT} set.
		 *
		 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
		 */
		protected boolean isDigit(int c) {
			return (c >= '0' && c <= '9');
		}

		/**
		 * Indicates whether the given character is in the {@code sub-delims} set.
		 *
		 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
		 */
		protected boolean isSubDelimiter(int c) {
			return ('!' == c || '$' == c || '&' == c || '\'' == c || '(' == c || ')' == c || '*' == c || '+' == c ||
					',' == c || ';' == c || '=' == c);
		}

		/**
		 * Indicates whether the given character is in the {@code unreserved} set.
		 *
		 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
		 */
		protected boolean isUnreserved(int c) {
			return (isAlpha(c) || isDigit(c) || '-' == c || '.' == c || '_' == c || '~' == c);
		}

		/**
		 * Indicates whether the given character is in the {@code pchar} set.
		 *
		 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
		 */
		protected boolean isPchar(int c) {
			return (isUnreserved(c) || isSubDelimiter(c) || ':' == c || '@' == c);
		}
	}


	/**
	 * Defines the contract for path (segments).
	 */
	interface PathComponent extends Serializable {

		String getPath();

		PathComponent encode(Charset charset);

		void verify();

	}


	/**
	 * Represents a path backed by a String.
	 */
	static final class FullPathComponent implements PathComponent {

		private final String path;

		public FullPathComponent(/*@Nullable*/ String path) {
			this.path = (path != null ? path : "");
		}

		@Override
		public String getPath() {
			return this.path;
		}

		@Override
		public PathComponent encode(Charset charset) {
			String encodedPath = encodeUriComponent(getPath(), charset, Type.PATH);
			return new FullPathComponent(encodedPath);
		}

		@Override
		public void verify() {
			verifyUriComponent(this.path, Type.PATH);
		}

		@Override
		public boolean equals(Object obj) {
			return (this == obj || (obj instanceof FullPathComponent &&
					getPath().equals(((FullPathComponent) obj).getPath())));
		}

		@Override
		public int hashCode() {
			return getPath().hashCode();
		}
	}


	/**
	 * Represents a path backed by a String list (i.e. path segments).
	 */
	static final class PathSegmentComponent implements PathComponent {

		private final List<String> pathSegments;

		public PathSegmentComponent(List<String> pathSegments) {
			Validate.notNull(pathSegments, "List must not be null");
			this.pathSegments = Collections.unmodifiableList(new ArrayList<>(pathSegments));
		}

		@Override
		public String getPath() {
			StringBuilder pathBuilder = new StringBuilder();
			pathBuilder.append(PATH_DELIMITER);
			for (Iterator<String> iterator = this.pathSegments.iterator(); iterator.hasNext(); ) {
				String pathSegment = iterator.next();
				pathBuilder.append(pathSegment);
				if (iterator.hasNext()) {
					pathBuilder.append(PATH_DELIMITER);
				}
			}
			return pathBuilder.toString();
		}

		public List<String> getPathSegments() {
			return this.pathSegments;
		}

		@Override
		public PathComponent encode(Charset charset) {
			List<String> lpathSegments = getPathSegments();
			List<String> encodedPathSegments = new ArrayList<>(lpathSegments.size());
			for (String pathSegment : lpathSegments) {
				String encodedPathSegment = encodeUriComponent(pathSegment, charset, Type.PATH_SEGMENT);
				encodedPathSegments.add(encodedPathSegment);
			}
			return new PathSegmentComponent(encodedPathSegments);
		}

		@Override
		public void verify() {
			for (String pathSegment : getPathSegments()) {
				verifyUriComponent(pathSegment, Type.PATH_SEGMENT);
			}
		}

		@Override
		public boolean equals(Object obj) {
			return (this == obj || (obj instanceof PathSegmentComponent &&
					getPathSegments().equals(((PathSegmentComponent) obj).getPathSegments())));
		}

		@Override
		public int hashCode() {
			return getPathSegments().hashCode();
		}
	}


	/**
	 * Represents a collection of PathComponents.
	 */
	static final class PathComponentComposite implements PathComponent {

		private final List<PathComponent> pathComponents;

		public PathComponentComposite(List<PathComponent> pathComponents) {
			Validate.notNull(pathComponents, "PathComponent List must not be null");
			this.pathComponents = pathComponents;
		}

		@Override
		public String getPath() {
			StringBuilder pathBuilder = new StringBuilder();
			for (PathComponent pathComponent : this.pathComponents) {
				pathBuilder.append(pathComponent.getPath());
			}
			return pathBuilder.toString();
		}

		@Override
		public PathComponent encode(Charset charset) {
			List<PathComponent> encodedComponents = new ArrayList<>(this.pathComponents.size());
			for (PathComponent pathComponent : this.pathComponents) {
				encodedComponents.add(pathComponent.encode(charset));
			}
			return new PathComponentComposite(encodedComponents);
		}

		@Override
		public void verify() {
			for (PathComponent pathComponent : this.pathComponents) {
				pathComponent.verify();
			}
		}

	}


	/**
	 * Represents an empty path.
	 */
	static final PathComponent NULL_PATH_COMPONENT = new PathComponent() {
		@Override
		public String getPath() {
			return "";
		}

		@Override
		public PathComponent encode(Charset charset) {
			return this;
		}

		@Override
		public void verify() {
			//NULL_PATH_COMPONENT has noting to verify
		}

		@Override
		public boolean equals(Object obj) {
			return (this == obj);
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}
	};


}
