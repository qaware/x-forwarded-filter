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

package de.qaware.web.util.uri;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiMapUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Extension of {@link UriComponentsBase} for hierarchical URIs.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @see <a href="http://tools.ietf.org/html/rfc3986#section-1.2.3">Hierarchical URIs</a>
 * @since 3.1.3
 */
@SuppressWarnings({"serial"})
final class HierarchicalUriComponents extends UriComponentsBase {

	private static final long serialVersionUID = 1;

	private static final String FOLDER_SEPARATOR = "/";

	private static final String WINDOWS_FOLDER_SEPARATOR = "\\";

	private static final String TOP_PATH = "..";

	private static final String CURRENT_PATH = ".";

	/*@Nullable*/
	private final String userInfo;

	/*@Nullable*/
	private final String host;

	/*@Nullable*/
	private final String port;

	private final PathComponent path;

	//MultiValuedMap interface does not implement Serializable but all know implementations of it do.
	@SuppressWarnings("squid:S1948") //the IF does not implement serializable but impls do
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
//copy constructor
	HierarchicalUriComponents(/*@Nullable*/ String scheme, /*@Nullable*/ String fragment, /*@Nullable*/ String userInfo,
			/*@Nullable*/ String host, /*@Nullable*/ String port, /*@Nullable*/ PathComponent path,
			/*@Nullable*/ MultiValuedMap<String, String> queryParams, boolean encoded, boolean verify) {

		super(scheme, fragment);
		this.userInfo = userInfo;
		this.host = host;
		this.port = port;
		this.path = (path != null ? path : NullPathComponent.getInstance());
		if (queryParams == null) {
			this.queryParams = MultiMapUtils.EMPTY_MULTI_VALUED_MAP;
		} else {
			this.queryParams = MultiMapUtils.unmodifiableMultiValuedMap(new ArrayListValuedHashMap<>(queryParams));
		}
		this.encoded = encoded;

		if (verify) {
			verify();
		}
	}


	// Component getters

	@Override
	/*@Nullable*/
	public String getSchemeSpecificPart() {
		return null;
	}

	@Override
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

	@Override
	/*@NonNull*/
	public String getPath() {
		return this.path.getPath();
	}

	@Override
	public List<String> getPathSegments() {
		return this.path.getPathSegments();
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
			appendQueryParam(queryBuilder, name, values);
		}
		return queryBuilder.toString();
	}

	private static void appendQueryParam(StringBuilder queryBuilder, String name, Collection<String> values) {
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

	private static void appendQueryParamName(StringBuilder queryBuilder, String name) {
		if (queryBuilder.length() != 0) {
			queryBuilder.append('&');
		}
		queryBuilder.append(name);
	}

	/**
	 * Return the map of query parameters. Empty if no query has been set.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Map<String, List<String>> getQueryParamsMap() {
		return MultiMapUtils.isEmpty(queryParams) ? Collections.emptyMap() : (Map<String, List<String>>) (Object) this.queryParams.asMap();
	}

	@Override
	public MultiValuedMap<String, String> getQueryParams() {
		return queryParams;
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
		String schemeTo = (scheme != null ? encodeUriComponent(scheme, charset, URIComponentType.SCHEME) : null);
		String fragmentTo = (fragment != null ? encodeUriComponent(fragment, charset, URIComponentType.FRAGMENT) : null);
		String userInfoTo = (this.userInfo != null ? encodeUriComponent(this.userInfo, charset, URIComponentType.USER_INFO) : null);
		String hostTo = (this.host != null ? encodeUriComponent(this.host, charset, getHostType()) : null);
		PathComponent pathTo = this.path.encode(charset);
		MultiValuedMap<String, String> paramsTo = encodeQueryParams(charset);
		return new HierarchicalUriComponents(schemeTo, fragmentTo, userInfoTo, hostTo, this.port,
				pathTo, paramsTo, true, false);
	}

	private MultiValuedMap<String, String> encodeQueryParams(Charset charset) {
		int size = this.queryParams.size();
		MultiValuedMap<String, String> result = new ArrayListValuedHashMap<>(size, 1);
		for (Map.Entry<String, Collection<String>> entry : this.queryParams.asMap().entrySet()) {
			String name = encodeUriComponent(entry.getKey(), charset, URIComponentType.QUERY_PARAM);
			List<String> values = new ArrayList<>(entry.getValue().size());
			for (String value : entry.getValue()) {
				values.add(encodeUriComponent(value, charset, URIComponentType.QUERY_PARAM));
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
	@SuppressWarnings("squid:S109")//"magic number"  required for encoder logic
	static String encodeUriComponent(String source, Charset charset, URIComponentType type) {
		if (StringUtils.isEmpty(source)) {
			return source;
		}
		Validate.notNull(charset, "Charset must not be null");
		Validate.notNull(type, "URIComponentType must not be null");

		byte[] bytes = source.getBytes(charset);
		ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
		boolean changed = false;
		for (byte b : bytes) {
			if (b < 0) {
				b += 256;
			}
			if (type.isAllowedCharacter(b)) {
				bos.write(b);
			} else {
				bos.write('%');
				//split byte into two 4 bit nibbles and convert them to characters
				char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
				char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
				bos.write(hex1);
				bos.write(hex2);
				changed = true;
			}
		}
		return (changed ? bufferToString(charset, bos) : source);
	}

	private static String bufferToString(Charset charset, ByteArrayOutputStream bos) {
		try {
			return bos.toString(charset.name());
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError("Existing charsets cannot be unsupported", e);
		}
	}

	private URIComponentType getHostType() {
		return (this.host != null && this.host.startsWith("[") ? URIComponentType.HOST_IPV6 : URIComponentType.HOST_IPV4);
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
		verifyUriComponent(getScheme(), URIComponentType.SCHEME);
		verifyUriComponent(this.userInfo, URIComponentType.USER_INFO);
		verifyUriComponent(this.host, getHostType());
		this.path.verify();
		for (Map.Entry<String, Collection<String>> entry : queryParams.asMap().entrySet()) {
			verifyUriComponent(entry.getKey(), URIComponentType.QUERY_PARAM);
			for (String value : entry.getValue()) {
				verifyUriComponent(value, URIComponentType.QUERY_PARAM);
			}
		}
		verifyUriComponent(getFragment(), URIComponentType.FRAGMENT);
	}


	// Expanding

	@Override
	protected HierarchicalUriComponents expandInternal(UriTemplateVariables uriVariables) {
		Validate.validState(!this.encoded, "Cannot expand an already encoded UriComponents object");

		String schemeTo = expandUriComponent(getScheme(), uriVariables);
		String fragmentTo = expandUriComponent(getFragment(), uriVariables);
		String userInfoTo = expandUriComponent(this.userInfo, uriVariables);
		String hostTo = expandUriComponent(this.host, uriVariables);
		String portTo = expandUriComponent(this.port, uriVariables);
		PathComponent pathTo = this.path.expand(uriVariables);
		MultiValuedMap<String, String> paramsTo = expandQueryParams(uriVariables);

		return new HierarchicalUriComponents(schemeTo, fragmentTo, userInfoTo, hostTo, portTo,
				pathTo, paramsTo, false, false);
	}

	private MultiValuedMap<String, String> expandQueryParams(UriTemplateVariables variables) {
		int size = this.queryParams.size();
		MultiValuedMap<String, String> result = new ArrayListValuedHashMap<>(size, 1);
		QueryUriTemplateVariables queryVariables = new QueryUriTemplateVariables(variables);
		for (Map.Entry<String, Collection<String>> entry : this.queryParams.asMap().entrySet()) {
			String name = expandUriComponent(entry.getKey(), queryVariables);
			List<String> values = new ArrayList<>(entry.getValue().size());
			for (String value : entry.getValue()) {
				values.add(expandUriComponent(value, queryVariables));
			}
			result.putAll(name, values);
		}
		return result;
	}

	/**
	 * Normalize the path removing sequences like "path/..".
	 */
	@Override
	public UriComponentsBase normalize() {
		String normalizedPath = cleanPath(getPath());
		return new HierarchicalUriComponents(getScheme(), getFragment(), this.userInfo, this.host, this.port,
				new FullPathComponent(normalizedPath), this.queryParams, this.encoded, false);
	}


	/**
	 * Normalize the path by suppressing sequences like "path/.." and
	 * inner simple dots.
	 * <p>The result is convenient for path comparison. For other uses,
	 * notice that Windows separators ("\") are replaced by simple slashes.
	 *
	 * @param path the original path
	 * @return the normalized path
	 */
	private static String cleanPath(String path) {
		if (StringUtils.isBlank(path)) {
			return path;
		}
		//bring to common form
		String pathToUse = StringUtils.replace(path, WINDOWS_FOLDER_SEPARATOR, FOLDER_SEPARATOR);

		// Strip prefix from path to analyze, to not treat it as part of the
		// first path element. This is necessary to correctly parse paths like
		// "file:core/../core/io/Resource.class", where the ".." should just
		// strip the first "core" directory while keeping the "file:" prefix.
		int prefixIndex = pathToUse.indexOf(':');
		String prefix = "";
		if (prefixIndex != -1) {
			prefix = pathToUse.substring(0, prefixIndex + 1);
			if (prefix.contains(FOLDER_SEPARATOR)) {
				prefix = "";
			} else {
				pathToUse = pathToUse.substring(prefixIndex + 1);
			}
		}

		if (pathToUse.startsWith(FOLDER_SEPARATOR)) {
			prefix = prefix + FOLDER_SEPARATOR;
			pathToUse = pathToUse.substring(1);
		}
		//normalize path by splitting into distinct path tokens
		List<String> pathElements = splitIntoPathElements(pathToUse);
		return prefix + String.join(FOLDER_SEPARATOR, pathElements);
	}

	private static List<String> splitIntoPathElements(String pathToUse) {
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
		return pathElements;
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
			return this.encoded ? new URI(toString()) : toUriFromComponents();
		} catch (URISyntaxException ex) {
			throw new IllegalStateException("Could not create URI object: " + ex.getMessage(), ex);
		}
	}

	private URI toUriFromComponents() throws URISyntaxException {
		String lPath = getPath();
		// Only prefix the path delimiter if something exists before it
		if (StringUtils.isNotEmpty(lPath)  //
				&& lPath.charAt(0) != PATH_DELIMITER//
				&& (getScheme() != null || getUserInfo() != null || getHost() != null || getPort() != -1)) {
			lPath = PATH_DELIMITER + lPath;
		}
		return new URI(getScheme(), getUserInfo(), getHost(), getPort(), lPath, getQuery(),
				getFragment());
	}

	@Override
	protected void copyToUriComponentsBuilder(UriComponentsBuilder builder) {
		if (getScheme() != null) {
			builder.scheme(getScheme());
		}
		if (getUserInfo() != null) {
			builder.userInfo(getUserInfo());
		}
		if (getHost() != null) {
			builder.host(getHost());
		}
		// Avoid parsing the port, may have URI variable..
		if (this.port != null) {
			builder.port(this.port);
		}
		this.path.copyToUriComponentsBuilder(builder);
		if (!getQueryParams().isEmpty()) {
			builder.queryParams(getQueryParamsMap());
		}
		if (getFragment() != null) {
			builder.fragment(getFragment());
		}
	}


	@SuppressWarnings("squid:S1067")//number of conditional operators > 3 -> very clear intent as it is
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


}
