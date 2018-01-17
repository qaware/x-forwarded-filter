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

import de.qaware.web.util.HttpHeaders;
import de.qaware.web.util.HttpServletRequestUtil;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.qaware.web.util.ForwardedHeader.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;


/**
 * Builder for {@link UriComponents}.
 * <p>
 * <p>Typical usage involves:
 * <ol>
 * <li>Create a {@code UriComponentsBuilder} with one of the static factory methods
 * (such as {@link #fromPath(String)} or {@link #fromUri(URI)})</li>
 * <li>Set the various URI components through the respective methods ({@link #scheme(String)},
 * {@link #userInfo(String)}, {@link #host(String)}, {@link #port(int)}, {@link #path(String)},
 * {@link #pathSegment(String...)}, {@link #queryParam(String, Object...)}, and
 * {@link #fragment(String)}.</li>
 * <li>Build the {@link UriComponents} instance with the {@link #build()} method.</li>
 * </ol>
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @author Oliver Gierke
 * @author Brian Clozel
 * @see #newInstance()
 * @see #fromPath(String)
 * @see #fromUri(URI)
 * @since 3.1
 */
public class UriComponentsBuilder  {

	private static final Pattern QUERY_PARAM_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?");

	private static final String SCHEME_PATTERN = "([^:/?#]+):";

	private static final String HTTP_PATTERN = "(?i)(http|https):";

	private static final String USERINFO_PATTERN = "([^@\\[/?#]*)";

	private static final String HOST_IPV4_PATTERN = "[^\\[/?#:]*";

	private static final String HOST_IPV6_PATTERN = "\\[[\\p{XDigit}\\:\\.]*[%\\p{Alnum}]*\\]";

	private static final String HOST_PATTERN = "(" + HOST_IPV6_PATTERN + "|" + HOST_IPV4_PATTERN + ")";

	private static final String PORT_PATTERN = "(\\d*(?:\\{[^/]+?\\})?)";

	private static final String PATH_PATTERN = "([^?#]*)";

	private static final String QUERY_PATTERN = "([^#]*)";

	private static final String LAST_PATTERN = "(.*)";

	// Regex patterns that matches URIs. See RFC 3986, appendix B
	private static final Pattern URI_PATTERN = Pattern.compile(
			"^(" + SCHEME_PATTERN + ")?" + "(//(" + USERINFO_PATTERN + "@)?" + HOST_PATTERN + "(:" + PORT_PATTERN +
					")?" + ")?" + PATH_PATTERN + "(\\?" + QUERY_PATTERN + ")?" + "(#" + LAST_PATTERN + ")?");

	private static final Pattern HTTP_URL_PATTERN = Pattern.compile(
			"^" + HTTP_PATTERN + "(//(" + USERINFO_PATTERN + "@)?" + HOST_PATTERN + "(:" + PORT_PATTERN + ")?" + ")?" +
					PATH_PATTERN + "(\\?" + LAST_PATTERN + ")?");

	private static final Pattern FORWARDED_HOST_PATTERN = Pattern.compile("host=\"?([^;,\"]+)\"?");

	private static final Pattern FORWARDED_PROTO_PATTERN = Pattern.compile("proto=\"?([^;,\"]+)\"?");




	/*@Nullable*/
	private String scheme;

	/*@Nullable*/
	private String ssp;

	/*@Nullable*/
	private String userInfo;

	/*@Nullable*/
	private String host;

	/*@Nullable*/
	private String port;

	private CompositePathComponentBuilder pathBuilder;

	private final MultiValuedMap<String, String> queryParams = new ArrayListValuedHashMap<>();

	/*@Nullable*/
	private String fragment;


	/**
	 * Default constructor. Protected to prevent direct instantiation.
	 *
	 * @see #newInstance()
	 * @see #fromPath(String)
	 * @see #fromUri(URI)
	 */
	protected UriComponentsBuilder() {
		this.pathBuilder = new CompositePathComponentBuilder();
	}

	/**
	 * Create a deep copy of the given UriComponentsBuilder.
	 *
	 * @param other the other builder to copy from
	 * @since 4.1.3
	 */
	protected UriComponentsBuilder(UriComponentsBuilder other) {
		this.scheme = other.scheme;
		this.ssp = other.ssp;
		this.userInfo = other.userInfo;
		this.host = other.host;
		this.port = other.port;
		this.pathBuilder = other.pathBuilder.cloneBuilder();
		this.queryParams.putAll(other.queryParams);
		this.fragment = other.fragment;
	}


	// Factory methods

	/**
	 * Create a new, empty builder.
	 *
	 * @return the new {@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder newInstance() {
		return new UriComponentsBuilder();
	}

	/**
	 * Create a builder that is initialized with the given path.
	 *
	 * @param path the path to initialize with
	 * @return the new {@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder fromPath(String path) {
		UriComponentsBuilder builder = new UriComponentsBuilder();
		builder.path(path);
		return builder;
	}

	/**
	 * Create a builder that is initialized with the given {@code URI}.
	 *
	 * @param uri the URI to initialize with
	 * @return the new {@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder fromUri(URI uri) {
		UriComponentsBuilder builder = new UriComponentsBuilder();
		builder.uri(uri);
		return builder;
	}

	/**
	 * Create a builder that is initialized with the given URI string.
	 * <p><strong>Note:</strong> The presence of reserved characters can prevent
	 * correct parsing of the URI string. For example if a query parameter
	 * contains {@code '='} or {@code '&'} characters, the query string cannot
	 * be parsed unambiguously. Such values should be substituted for URI
	 * variables to enable correct parsing:
	 * <pre class="code">
	 * String uriString = &quot;/hotels/42?filter={value}&quot;;
	 * UriComponentsBuilder.fromUriString(uriString).buildAndExpand(&quot;hot&amp;cold&quot;);
	 * </pre>
	 *
	 * @param uri the URI string to initialize with
	 * @return the new {@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder fromUriString(String uri) {
		Validate.notNull(uri, "URI must not be null");
		Matcher matcher = URI_PATTERN.matcher(uri);
		if (!matcher.matches()) {
				throw new IllegalArgumentException("[" + uri + "] is not a valid URI");
		}
		UriComponentsBuilder builder = new UriComponentsBuilder();
		String scheme = matcher.group(2);
		String userInfo = matcher.group(5);
		String host = matcher.group(6);
		String port = matcher.group(8);
		String path = matcher.group(9);
		String query = matcher.group(11);
		String fragment = matcher.group(13);
		builder.scheme(scheme);
		if (isOpaque(uri, scheme)) {
			setSchemeSpecificPart(uri, builder, scheme, fragment);
		} else {
			builder.userInfo(userInfo);
			builder.host(host);
			if (StringUtils.isNotEmpty(port)) {
				builder.port(port);
			}
			builder.path(path);
			builder.query(query);
		}
		if (isNotBlank(fragment)) {
			builder.fragment(fragment);
		}
		return builder;
	}

	private static void setSchemeSpecificPart(String uri, UriComponentsBuilder builder, String scheme, String fragment) {
		String ssp = uri.substring(scheme.length()).substring(1);
		if (StringUtils.isNotEmpty(fragment)) {
			ssp = ssp.substring(0, ssp.length() - (fragment.length() + 1));
		}
		builder.schemeSpecificPart(ssp);
	}

	private static boolean isOpaque(String uri, String scheme) {
		if (StringUtils.isNotEmpty(scheme)) {
			String rest = uri.substring(scheme.length());
			if (!rest.startsWith(":/")) {
				return  true;
			}
		}
		return false;
	}

	/**
	 * Create a URI components builder from the given HTTP URL String.
	 * <p><strong>Note:</strong> The presence of reserved characters can prevent
	 * correct parsing of the URI string. For example if a query parameter
	 * contains {@code '='} or {@code '&'} characters, the query string cannot
	 * be parsed unambiguously. Such values should be substituted for URI
	 * variables to enable correct parsing:
	 * <pre class="code">
	 * String urlString = &quot;https://example.com/hotels/42?filter={value}&quot;;
	 * UriComponentsBuilder.fromHttpUrl(urlString).buildAndExpand(&quot;hot&amp;cold&quot;);
	 * </pre>
	 *
	 * @param httpUrl the source URI
	 * @return the URI components of the URI
	 */
	public static UriComponentsBuilder fromHttpUrl(String httpUrl) {
		Validate.notNull(httpUrl, "HTTP URL must not be null");
		Matcher matcher = HTTP_URL_PATTERN.matcher(httpUrl);
		if (matcher.matches()) {
			UriComponentsBuilder builder = new UriComponentsBuilder();
			String scheme = matcher.group(1);
			builder.scheme(scheme != null ? scheme.toLowerCase() : null);
			builder.userInfo(matcher.group(4));
			String host = matcher.group(5);
			if (StringUtils.isNotEmpty(scheme) && StringUtils.isEmpty(host)) {
				throw new IllegalArgumentException("[" + httpUrl + "] is not a valid HTTP URL");
			}
			builder.host(host);
			String port = matcher.group(7);
			if (StringUtils.isNotEmpty(port)) {
				builder.port(port);
			}
			builder.path(matcher.group(8));
			builder.query(matcher.group(10));
			return builder;
		} else {
			throw new IllegalArgumentException("[" + httpUrl + "] is not a valid HTTP URL");
		}
	}

	/**
	 * Create a new {@code UriComponents} object from the URI associated with
	 * the given HttpRequest while also overlaying with values from the headers
	 * "Forwarded" (<a href="http://tools.ietf.org/html/rfc7239">RFC 7239</a>),
	 * or "X-Forwarded-Host", "X-Forwarded-Port", and "X-Forwarded-Proto" if
	 * "Forwarded" is not found.
	 *
	 * @param request the source request
	 * @return the URI components of the URI
	 * @since 4.1.5
	 */
	public static UriComponentsBuilder fromHttpRequest(HttpServletRequest request) {
		URI uri = HttpServletRequestUtil.getURI(request);
		HttpHeaders headers = HttpServletRequestUtil.getHeaders(request);
		return fromUri(uri).adaptFromForwardedHeaders(headers);
	}


	// build methods

	/**
	 * Build a {@code UriComponents} instance from the various components contained in this builder.
	 *
	 * @return the URI components
	 */
	public UriComponents build() {
		return build(false);
	}

	/**
	 * Build a {@code UriComponents} instance from the various components
	 * contained in this builder.
	 *
	 * @param encoded whether all the components set in this builder are
	 *                encoded ({@code true}) or not ({@code false})
	 * @return the URI components
	 */
	public UriComponents build(boolean encoded) {
		if (this.ssp != null) {
			return new OpaqueUriComponents(this.scheme, this.ssp, this.fragment);
		} else {
			return new HierarchicalUriComponents(this.scheme, this.fragment, this.userInfo,
					this.host, this.port, this.pathBuilder.build(), this.queryParams, encoded, true);
		}
	}

	/**
	 * Build a {@code UriComponents} instance and replaces URI template variables
	 * with the values from a map. This is a shortcut method which combines
	 * calls to {@link #build()} and then {@link UriComponents#expand(Map)}.
	 *
	 * @param uriVariables the map of URI variables
	 * @return the URI components with expanded values
	 */
	public UriComponents buildAndExpand(Map<String, ?> uriVariables) {
		return build(false).expand(uriVariables);
	}

	/**
	 * Build a {@code UriComponents} instance and replaces URI template variables
	 * with the values from an array. This is a shortcut method which combines
	 * calls to {@link #build()} and then {@link UriComponents#expand(Object...)}.
	 *
	 * @param uriVariableValues URI variable values
	 * @return the URI components with expanded values
	 */
	public UriComponents buildAndExpand(Object... uriVariableValues) {
		return build(false).expand(uriVariableValues);
	}


	/**
	 * Build a {@link URI} instance and replaces URI template variables
	 * with the values from an array.
	 *
	 * @param uriVariables the map of URI variables
	 * @return the URI
	 */
	public URI build(Object... uriVariables) {
		return buildAndExpand(uriVariables).encode().toUri();
	}

	/**
	 * Build a {@link URI} instance and replaces URI template variables
	 * with the values from a map.
	 *
	 * @param uriVariables the map of URI variables
	 * @return the URI
	 */
	public URI build(Map<String, ?> uriVariables) {
		return buildAndExpand(uriVariables).encode().toUri();
	}


	/**
	 * Build a URI String. This is a shortcut method which combines calls
	 * to {@link #build()}, then {@link UriComponents#encode()} and finally
	 * {@link UriComponents#toUriString()}.
	 *
	 * @see UriComponents#toUriString()
	 * @since 4.1
	 */
	public String toUriString() {
		return build(false).encode().toUriString();
	}


	// Instance methods

	/**
	 * Initialize components of this builder from components of the given URI.
	 *
	 * @param uri the URI
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder uri(URI uri) {
		Validate.notNull(uri, "URI must not be null");
		this.scheme = uri.getScheme();
		if (uri.isOpaque()) {
			this.ssp = uri.getRawSchemeSpecificPart();
			resetHierarchicalComponents();
		} else {
			if (uri.getRawUserInfo() != null) {
				this.userInfo = uri.getRawUserInfo();
			}
			if (uri.getHost() != null) {
				this.host = uri.getHost();
			}
			if (uri.getPort() != -1) {
				this.port = String.valueOf(uri.getPort());
			}
			if (StringUtils.isNotEmpty(uri.getRawPath())) {
				this.pathBuilder = new CompositePathComponentBuilder();
				this.pathBuilder.addPath(uri.getRawPath());
			}
			if (StringUtils.isNotEmpty(uri.getRawQuery())) {
				this.queryParams.clear();
				query(uri.getRawQuery());
			}
			resetSchemeSpecificPart();
		}
		if (uri.getRawFragment() != null) {
			this.fragment = uri.getRawFragment();
		}
		return this;
	}

	/**
	 * Set or append individual URI components of this builder from the values
	 * of the given {@link UriComponents} instance.
	 * <p>For the semantics of each component (i.e. set vs append) check the
	 * builder methods on this class. For example {@link #host(String)} sets
	 * while {@link #path(String)} appends.
	 *
	 * @param uriComponents the UriComponents to copy from
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder uriComponents(UriComponents uriComponents) {
		Validate.notNull(uriComponents, "UriComponents must not be null");
		uriComponents.copyToUriComponentsBuilder(this);
		return this;
	}

	/**
	 * Set the URI scheme. The given scheme may contain URI template variables,
	 * and may also be {@code null} to clear the scheme of this builder.
	 *
	 * @param scheme the URI scheme
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder scheme(/*@Nullable*/ String scheme) {
		this.scheme = scheme;
		return this;
	}

	/**
	 * Set the URI scheme-specific-part. When invoked, this method overwrites
	 * {@linkplain #userInfo(String) user-info}, {@linkplain #host(String) host},
	 * {@linkplain #port(int) port}, {@linkplain #path(String) path}, and
	 * {@link #query(String) query}.
	 *
	 * @param ssp the URI scheme-specific-part, may contain URI template parameters
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder schemeSpecificPart(String ssp) {
		this.ssp = ssp;
		resetHierarchicalComponents();
		return this;
	}

	/**
	 * Set the URI user info. The given user info may contain URI template variables,
	 * and may also be {@code null} to clear the user info of this builder.
	 *
	 * @param userInfo the URI user info
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder userInfo(/*@Nullable*/ String userInfo) {
		this.userInfo = userInfo;
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Set the URI host. The given host may contain URI template variables,
	 * and may also be {@code null} to clear the host of this builder.
	 *
	 * @param host the URI host
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder host(/*@Nullable*/ String host) {
		this.host = host;
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Set the URI port. Passing {@code -1} will clear the port of this builder.
	 *
	 * @param port the URI port
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder port(int port) {
		Validate.isTrue(port >= -1, "Port must be >= -1");
		this.port = String.valueOf(port);
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Set the URI port. Use this method only when the port needs to be
	 * parameterized with a URI variable. Otherwise use {@link #port(int)}.
	 * Passing {@code null} will clear the port of this builder.
	 *
	 * @param port the URI port
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder port(/*@Nullable*/ String port) {
		this.port = port;
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Append the given path to the existing path of this builder.
	 * The given path may contain URI template variables.
	 *
	 * @param path the URI path
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder path(String path) {
		this.pathBuilder.addPath(path);
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Append path segments to the existing path. Each path segment may contain
	 * URI template variables and should not contain any slashes.
	 * Use {@code path("/")} subsequently to ensure a trailing slash.
	 *
	 * @param pathSegments the URI path segments
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder pathSegment(String... pathSegments)  {
		this.pathBuilder.addPathSegments(pathSegments);
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Set the path of this builder overriding all existing path and path segment values.
	 *
	 * @param path the URI path (a {@code null} value results in an empty path)
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder replacePath(/*@Nullable*/ String path) {
		this.pathBuilder = new CompositePathComponentBuilder();
		if (path != null) {
			this.pathBuilder.addPath(path);
		}
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Append the given query to the existing query of this builder.
	 * The given query may contain URI template variables.
	 * <p><strong>Note:</strong> The presence of reserved characters can prevent
	 * correct parsing of the URI string. For example if a query parameter
	 * contains {@code '='} or {@code '&'} characters, the query string cannot
	 * be parsed unambiguously. Such values should be substituted for URI
	 * variables to enable correct parsing:
	 * <pre class="code">
	 * UriComponentsBuilder.fromUriString(&quot;/hotels/42&quot;)
	 * .query(&quot;filter={value}&quot;)
	 * .buildAndExpand(&quot;hot&amp;cold&quot;);
	 * </pre>
	 *
	 * @param query the query string
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder query(/*@Nullable*/ String query) {
		if (query != null) {
			Matcher matcher = QUERY_PARAM_PATTERN.matcher(query);
			while (matcher.find()) {
				String name = matcher.group(1);
				String eq = matcher.group(2);
				String value = matcher.group(3);
				if (value == null) {
					value = isNotBlank(eq) ? "" : null;
				}
				queryParam(name, value);
			}
		} else {
			this.queryParams.clear();
		}
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Set the query of this builder overriding all existing query parameters.
	 *
	 * @param query the query string; a {@code null} value removes all query parameters.
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder replaceQuery(/*@Nullable*/ String query) {
		this.queryParams.clear();
		if (query != null) {
			query(query);
		}
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Append the given query parameter to the existing query parameters. The
	 * given name or any of the values may contain URI template variables. If no
	 * values are given, the resulting URI will contain the query parameter name
	 * only (i.e. {@code ?foo} instead of {@code ?foo=bar}.
	 *
	 * @param name   the query parameter name
	 * @param values the query parameter values
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder queryParam(String name, Object... values) {
		Validate.notNull(name, "Name must not be null");
		if (ArrayUtils.isNotEmpty(values)) {
			for (Object value : values) {
				String valueAsString = (value != null ? value.toString() : null);
				this.queryParams.put(name, valueAsString);
			}
		} else {
			this.queryParams.put(name, null);
		}
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Add the given query parameters.
	 *
	 * @param params the params
	 * @return this UriComponentsBuilder
	 * @since 4.0
	 */
	public UriComponentsBuilder queryParams(/*@Nullable*/ Map<String, List<String>> params) {
		if (params != null) {
			params.forEach(this.queryParams::putAll);
		}
		return this;
	}

	/**
	 * Set the query parameter values overriding all existing query values for
	 * the same parameter. If no values are given, the query parameter is removed.
	 *
	 * @param name   the query parameter name
	 * @param values the query parameter values
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder replaceQueryParam(String name, Object... values) {
		Validate.notNull(name, "Name must not be null");
		this.queryParams.remove(name);
		if (!ArrayUtils.isEmpty(values)) {
			queryParam(name, values);
		}
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Set the query parameter values overriding all existing query values.
	 *
	 * @param params the query parameter name
	 * @return this UriComponentsBuilder
	 * @since 4.2
	 */
	public UriComponentsBuilder replaceQueryParams(/*@Nullable*/ MultiValuedMap<String, String> params) {
		this.queryParams.clear();
		if (params != null) {
			this.queryParams.putAll(params);
		}
		return this;
	}

	/**
	 * Set the URI fragment. The given fragment may contain URI template variables,
	 * and may also be {@code null} to clear the fragment of this builder.
	 *
	 * @param fragment the URI fragment
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder fragment(/*@Nullable*/ String fragment) {
		if (fragment != null) {
			Validate.notEmpty(fragment, "Fragment must not be empty");
			this.fragment = fragment;
		} else {
			this.fragment = null;
		}
		return this;
	}

	/**
	 * Adapt this builder's scheme+host+port from the given headers, specifically
	 * "Forwarded" (<a href="http://tools.ietf.org/html/rfc7239">RFC 7239</a>,
	 * or "X-Forwarded-Host", "X-Forwarded-Port", and "X-Forwarded-Proto" if
	 * "Forwarded" is not found.
	 *
	 * @param headers the HTTP headers to consider
	 * @return this UriComponentsBuilder
	 * @since 4.2.7
	 */
	private UriComponentsBuilder adaptFromForwardedHeaders(HttpHeaders headers) {
		String forwardedHeader = headers.getFirst(FORWARDED.headerName());

		if (isNotBlank(forwardedHeader)) {
			adaptForwardedHeader(forwardedHeader);
		} else {
			adaptXForwardedHost(headers);
			adaptXForwardedPort(headers);
			adaptXForwardedProto(headers);
		}

		if (isHttp() || isHttps()) {
			this.port = null;
		}

		return this;
	}

	private void adaptForwardedHeader(String forwardedHeader) {
		String forwardedToUse = getFirstValueToken(forwardedHeader, ",");
		Matcher matcher = FORWARDED_HOST_PATTERN.matcher(forwardedToUse);
		if (matcher.find()) {
			adaptForwardedHost(matcher.group(1).trim());
		}
		matcher = FORWARDED_PROTO_PATTERN.matcher(forwardedToUse);
		if (matcher.find()) {
			scheme(matcher.group(1).trim());
		}
	}

	private void adaptXForwardedProto(HttpHeaders headers) {
		String protocolHeader = headers.getFirst(X_FORWARDED_PROTO.headerName());
		if (isNotBlank(protocolHeader)) {
			scheme(getFirstValueToken(protocolHeader, ","));
		}
	}

	private void adaptXForwardedPort(HttpHeaders headers) {
		String portHeader = headers.getFirst(X_FORWARDED_PORT.headerName());
		if (isNotBlank(portHeader)) {
			port(Integer.parseInt(getFirstValueToken(portHeader, ",")));
		}
	}

	private void adaptXForwardedHost(HttpHeaders headers) {
		String hostHeader = headers.getFirst(X_FORWARDED_HOST.headerName());
		if (isNotBlank(hostHeader)) {
			adaptForwardedHost(getFirstValueToken(hostHeader, ","));
		}
	}

	private boolean isHttp() {
		return scheme != null && scheme.equals("http") && "80".equals(this.port);
	}

	private boolean isHttps() {
		return scheme != null && scheme.equals("https") && "443".equals(this.port);
	}


	/**
	 * Returns first token only. e.g: value="123, 345, 678"  with delim="," will return "123"
	 * or more formally: return value.substring(0,value.indexOf(delim));
	 *
	 * @param value value to be split
	 * @param delimiter delimiter
	 * @return first token
	 */
	private static String getFirstValueToken(String value, String delimiter) {
		int pos = value.indexOf(delimiter);
		return (pos == -1) ? value : value.substring(0, pos);
	}

	private void adaptForwardedHost(String hostToUse) {
		int portSeparatorIdx = hostToUse.lastIndexOf(':');
		if (portSeparatorIdx > hostToUse.lastIndexOf(']')) {
			host(hostToUse.substring(0, portSeparatorIdx));
			port(Integer.parseInt(hostToUse.substring(portSeparatorIdx + 1)));
		} else {
			host(hostToUse);
			port(null);
		}
	}

	private void resetHierarchicalComponents() {
		this.userInfo = null;
		this.host = null;
		this.port = null;
		this.pathBuilder = new CompositePathComponentBuilder();
		this.queryParams.clear();
	}

	private void resetSchemeSpecificPart() {
		this.ssp = null;
	}


}
